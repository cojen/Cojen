/*
 *  Copyright 2004 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.util;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;
import org.cojen.classfile.ClassFile;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.Opcode;
import org.cojen.classfile.TypeDesc;

/**
 * Provides a simple and efficient means of reading and writing bean
 * properties. BeanPropertyAccessor auto-generates code, eliminating the
 * need to invoke methods via reflection. Bean access methods are bound-to
 * directly, using a special hash/switch design pattern.
 *
 * @author Brian S O'Neill
 */
public abstract class BeanPropertyAccessor<B> {
    // Maps classes to softly referenced BeanPropertyAccessors.
    private static Map cAccessors = new WeakIdentityMap();

    /**
     * Returns a new or cached BeanPropertyAccessor for the given class.
     */
    public static <B> BeanPropertyAccessor<B> forClass(Class<B> clazz) {
        synchronized (cAccessors) {
            BeanPropertyAccessor bpa;
            SoftReference ref = (SoftReference) cAccessors.get(clazz);
            if (ref != null) {
                bpa = (BeanPropertyAccessor)ref.get();
                if (bpa != null) {
                    return bpa;
                }
            }
            bpa = generate(clazz);
            cAccessors.put(clazz, new SoftReference(bpa));
            return bpa;
        }
    }

    private static BeanPropertyAccessor generate(Class beanType) {
        ClassInjector ci = ClassInjector.create
            (BeanPropertyAccessor.class.getName(), beanType.getClassLoader());
        Class clazz = ci.defineClass(generateClassFile(ci.getClassName(), beanType));

        try {
            return (BeanPropertyAccessor)clazz.newInstance();
        } catch (InstantiationException e) {
            throw new InternalError(e.toString());
        } catch (IllegalAccessException e) {
            throw new InternalError(e.toString());
        }
    }

    private static ClassFile generateClassFile(String className,
                                               Class beanType)
    {
        BeanProperty[][] props = getBeanProperties(beanType);

        ClassFile cf = new ClassFile(className, BeanPropertyAccessor.class);
        cf.markSynthetic();
        cf.setSourceFile(BeanPropertyAccessor.class.getName());
        try {
            cf.setTarget(System.getProperty("java.specification.version"));
        } catch (Exception e) {
        }

        MethodInfo ctor = cf.addConstructor(Modifiers.PUBLIC, null);
        ctor.markSynthetic();
        CodeBuilder builder = new CodeBuilder(ctor);

        builder.loadThis();
        builder.invokeSuperConstructor(null);
        builder.returnVoid();

        generateMethod(cf, beanType, props[0], true);
        generateMethod(cf, beanType, props[1], false);

        return cf;
    }

    private static void generateMethod(ClassFile cf,
                                       Class beanType,
                                       BeanProperty[] properties,
                                       boolean forRead)
    {
        TypeDesc objectType = TypeDesc.OBJECT;
        TypeDesc stringType = TypeDesc.STRING;
        TypeDesc intType = TypeDesc.INT;
        TypeDesc booleanType = TypeDesc.BOOLEAN;
        TypeDesc exceptionType =
            TypeDesc.forClass(NoSuchPropertyException.class);

        MethodInfo mi;
        if (forRead) {
            TypeDesc[] params = {objectType, stringType};
            mi = cf.addMethod
                (Modifiers.PUBLIC, "getPropertyValue", objectType, params);
        } else {
            TypeDesc[] params = new TypeDesc[] {
                objectType, stringType, objectType
            };
            mi = cf.addMethod
                (Modifiers.PUBLIC, "setPropertyValue", null, params);
        }

        mi.markSynthetic();
        CodeBuilder builder = new CodeBuilder(mi);

        LocalVariable beanVar = builder.getParameter(0);
        LocalVariable propertyVar = builder.getParameter(1);
        LocalVariable valueVar;
        if (forRead) {
            valueVar = null;
        } else {
            valueVar = builder.getParameter(2);
        }

        builder.loadLocal(beanVar);
        builder.checkCast(TypeDesc.forClass(beanType));
        builder.storeLocal(beanVar);

        if (properties.length > 0) {
            int[] cases = new int[hashCapacity(properties.length)];
            int caseCount = cases.length;
            for (int i=0; i<caseCount; i++) {
                cases[i] = i;
            }

            Label[] switchLabels = new Label[caseCount];
            Label noMatch = builder.createLabel();
            List[] caseMethods = caseMethods(caseCount, properties);
            
            for (int i=0; i<caseCount; i++) {
                List matches = caseMethods[i];
                if (matches == null || matches.size() == 0) {
                    switchLabels[i] = noMatch;
                } else {
                    switchLabels[i] = builder.createLabel();
                }
            }

            if (properties.length > 1) {
                builder.loadLocal(propertyVar);
                builder.invokeVirtual(String.class.getName(),
                                      "hashCode", intType, null);
                builder.loadConstant(0x7fffffff);
                builder.math(Opcode.IAND);
                builder.loadConstant(caseCount);
                builder.math(Opcode.IREM);
            
                builder.switchBranch(cases, switchLabels, noMatch);
            }
            
            // Params to invoke String.equals.
            TypeDesc[] params = {objectType};
            
            for (int i=0; i<caseCount; i++) {
                List matches = caseMethods[i];
                if (matches == null || matches.size() == 0) {
                    continue;
                }
                
                switchLabels[i].setLocation();
                
                int matchCount = matches.size();
                for (int j=0; j<matchCount; j++) {
                    BeanProperty bp = (BeanProperty)matches.get(j);
                    
                    // Test against name to find exact match.
                    
                    builder.loadConstant(bp.getName());
                    builder.loadLocal(propertyVar);
                    builder.invokeVirtual(String.class.getName(),
                                          "equals", booleanType, params);
                    
                    Label notEqual;
                    
                    if (j == matchCount - 1) {
                        notEqual = null;
                        builder.ifZeroComparisonBranch(noMatch, "==");
                    } else {
                        notEqual = builder.createLabel();
                        builder.ifZeroComparisonBranch(notEqual, "==");
                    }
                    
                    if (forRead) {
                        builder.loadLocal(beanVar);
                        builder.invoke(bp.getReadMethod());
                        TypeDesc type = TypeDesc.forClass(bp.getType());
                        builder.convert(type, type.toObjectType());
                        builder.returnValue(TypeDesc.OBJECT);
                    } else {
                        builder.loadLocal(beanVar);
                        builder.loadLocal(valueVar);
                        TypeDesc type = TypeDesc.forClass(bp.getType());
                        builder.checkCast(type.toObjectType());
                        builder.convert(type.toObjectType(), type);
                        builder.invoke(bp.getWriteMethod());
                        builder.returnVoid();
                    }
                    
                    if (notEqual != null) {
                        notEqual.setLocation();
                    }
                }
            }
            
            noMatch.setLocation();
        }

        builder.newObject(exceptionType);
        builder.dup();
        builder.loadLocal(propertyVar);
        builder.loadConstant(forRead);

        // Params to invoke NoSuchPropertyException.<init>.
        TypeDesc[] params = {stringType, booleanType};

        builder.invokeConstructor
            (NoSuchPropertyException.class.getName(), params);
        builder.throwObject();
    }

    /**
     * Returns a prime number, at least twice as large as needed. This should
     * minimize hash collisions. Since all the hash keys are known up front,
     * the capacity could be tweaked until there are no collisions, but this
     * technique is easier and deterministic.
     */
    private static int hashCapacity(int min) {
        BigInteger capacity = BigInteger.valueOf(min * 2 + 1);
        while (!capacity.isProbablePrime(10)) {
            capacity = capacity.add(BigInteger.valueOf(2));
        }
        return capacity.intValue();
    }

    /**
     * Returns an array of Lists of BeanProperties. The first index
     * matches a switch case, the second index provides a list of all the
     * BeanProperties whose name hash matched on the case.
     */
    private static List[] caseMethods(int caseCount,
                                      BeanProperty[] props) {
        List[] cases = new List[caseCount];

        for (int i=0; i<props.length; i++) {
            BeanProperty prop = props[i];
            int hashCode = prop.getName().hashCode();
            int caseValue = (hashCode & 0x7fffffff) % caseCount;
            List matches = cases[caseValue];
            if (matches == null) {
                matches = cases[caseValue] = new ArrayList();
            }
            matches.add(prop);
        }

        return cases;
    }

    /**
     * Returns two arrays of BeanProperties. Array 0 contains read
     * BeanProperties, array 1 contains the write BeanProperties.
     */
    private static BeanProperty[][] getBeanProperties(Class beanType) {
        List readProperties = new ArrayList();
        List writeProperties = new ArrayList();

        Map map = BeanIntrospector.getAllProperties(beanType);

        Iterator it = map.values().iterator();
        while (it.hasNext()) {
            BeanProperty bp = (BeanProperty)it.next();
            if (bp.getReadMethod() != null) {
                readProperties.add(bp);
            }
            if (bp.getWriteMethod() != null) {
                writeProperties.add(bp);
            }
        }

        BeanProperty[][] props = new BeanProperty[2][];
        
        props[0] = new BeanProperty[readProperties.size()];
        readProperties.toArray(props[0]);
        props[1] = new BeanProperty[writeProperties.size()];
        writeProperties.toArray(props[1]);

        return props;
    }

    protected BeanPropertyAccessor() {
    }

    // The actual public methods that will need to be defined.

    public abstract Object getPropertyValue(B bean, String property)
        throws NoSuchPropertyException;

    public abstract void setPropertyValue(B bean, String property,
                                          Object value)
        throws NoSuchPropertyException;

    // Auto-generated code sample:
    /*
    public Object getPropertyValue(Object bean, String property) {
        Bean bean = (Bean)bean;
        
        switch ((property.hashCode() & 0x7fffffff) % 11) {
        case 0:
            if ("name".equals(property)) {
                return bean.getName();
            }
            break;
        case 1:
            // No case
            break;
        case 2:
            // Hash collision
            if ("value".equals(property)) {
                return bean.getValue();
            } else if ("age".equals(property)) {
                return new Integer(bean.getAge());
            }
            break;
        case 3:
            if ("start".equals(property)) {
                return bean.getStart();
            }
            break;
        case 4:
        case 5:
        case 6:
            // No case
            break;
        case 7:
            if ("end".equals(property)) {
                return bean.isEnd() ? Boolean.TRUE : Boolean.FALSE;
            }
            break;
        case 8:
        case 9:
        case 10:
            // No case
            break;
        }
        
        throw new NoSuchPropertyException(property, true);
    }

    public void setPropertyValue(Object bean, String property, Object value) {
        Bean bean = (Bean)bean;
        
        switch ((property.hashCode() & 0x7fffffff) % 11) {
        case 0:
            if ("name".equals(property)) {
                bean.setName(value);
            }
            break;
        case 1:
            // No case
            break;
        case 2:
            // Hash collision
            if ("value".equals(property)) {
                bean.setValue(value);
            } else if ("age".equals(property)) {
                bean.setAge(((Integer)value).intValue());
            }
            break;
        case 3:
            if ("start".equals(property)) {
                bean.setStart(value);
            }
            break;
        case 4:
        case 5:
        case 6:
            // No case
            break;
        case 7:
            if ("end".equals(property)) {
                bean.setEnd(((Boolean)value).booleanValue());
            }
            break;
        case 8:
        case 9:
        case 10:
            // No case
            break;
        }
        
        throw new NoSuchPropertyException(property, false);
    }
    */
}
