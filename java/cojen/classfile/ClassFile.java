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

package cojen.classfile;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import cojen.classfile.attr.DeprecatedAttr;
import cojen.classfile.attr.InnerClassesAttr;
import cojen.classfile.attr.SignatureAttr;
import cojen.classfile.attr.SourceFileAttr;
import cojen.classfile.attr.SyntheticAttr;

/**
 * A class used to create Java class files. Call the writeTo method
 * to produce a class file.
 *
 * <p>See <i>The Java Virtual Machine Specification</i> (ISBN 0-201-63452-X)
 * for information on how class files are structured. Section 4.1 describes
 * the ClassFile structure.
 * 
 * @author Brian S O'Neill
 */
public class ClassFile {
    private static final int MAGIC = 0xCAFEBABE;

    /**
     * Reads a ClassFile from the given InputStream. With this method, inner
     * classes cannot be loaded, and custom attributes cannot be defined.
     *
     * @param in source of class file data
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(InputStream in) throws IOException {
        return readFrom(in, null, null);
    }

    /**
     * Reads a ClassFile from the given DataInput. With this method, inner
     * classes cannot be loaded, and custom attributes cannot be defined.
     *
     * @param din source of class file data
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(DataInput din) throws IOException {
        return readFrom(din, null, null);
    }

    /**
     * Reads a ClassFile from the given InputStream. A
     * {@link ClassFileDataLoader} may be provided, which allows inner class
     * definitions to be loaded. Also, an {@link AttributeFactory} may be
     * provided, which allows non-standard attributes to be read. All
     * remaining unknown attribute types are captured, but are not decoded.
     *
     * @param in source of class file data
     * @param loader optional loader for reading inner class definitions
     * @param attrFactory optional factory for reading custom attributes
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(InputStream in,
                                     ClassFileDataLoader loader,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }
        return readFrom((DataInput)in, loader, attrFactory);
    }

    /**
     * Reads a ClassFile from the given DataInput. A
     * {@link ClassFileDataLoader} may be provided, which allows inner class
     * definitions to be loaded. Also, an {@link AttributeFactory} may be
     * provided, which allows non-standard attributes to be read. All
     * remaining unknown attribute types are captured, but are not decoded.
     *
     * @param din source of class file data
     * @param loader optional loader for reading inner class definitions
     * @param attrFactory optional factory for reading custom attributes
     * @throws IOException for I/O error or if classfile is invalid.
     * @throws ArrayIndexOutOfBoundsException if a constant pool index is out
     * of range.
     * @throws ClassCastException if a constant pool index references the
     * wrong type.
     */
    public static ClassFile readFrom(DataInput din,
                                     ClassFileDataLoader loader,
                                     AttributeFactory attrFactory)
        throws IOException
    {
        return readFrom(din, loader, attrFactory, new HashMap(11), null);
    }

    /**
     * @param loadedClassFiles Maps name to ClassFiles for classes already
     * loaded. This prevents infinite loop: inner loads outer loads inner...
     */
    private static ClassFile readFrom(DataInput din,
                                      ClassFileDataLoader loader,
                                      AttributeFactory attrFactory,
                                      Map loadedClassFiles,
                                      ClassFile outerClass)
        throws IOException
    {
        int magic = din.readInt();
        if (magic != MAGIC) {
            throw new IOException("Incorrect magic number: 0x" + 
                                  Integer.toHexString(magic));
        }

        short minor = din.readShort();
        short major = din.readShort();

        ConstantPool cp = ConstantPool.readFrom(din);
        Modifiers modifiers = Modifiers.getInstance(din.readUnsignedShort())
            .toSynchronized(false);

        int index = din.readUnsignedShort();
        ConstantClassInfo thisClass = (ConstantClassInfo)cp.getConstant(index);

        index = din.readUnsignedShort();
        ConstantClassInfo superClass = null;
        if (index > 0) {
            superClass = (ConstantClassInfo)cp.getConstant(index);
        }

        ClassFile cf =
            new ClassFile(cp, modifiers, thisClass, superClass, outerClass);
        cf.setVersion(major, minor);
        loadedClassFiles.put(cf.getClassName(), cf);

        // Read interfaces.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            index = din.readUnsignedShort();
            ConstantClassInfo info = (ConstantClassInfo)cp.getConstant(index);
            cf.addInterface(info.getType().getRootName());
        }
        
        // Read fields.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            cf.mFields.add(FieldInfo.readFrom(cf, din, attrFactory));
        }
        
        // Read methods.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            cf.mMethods.add(MethodInfo.readFrom(cf, din, attrFactory));
        }

        // Read attributes.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            Attribute attr = Attribute.readFrom(cp, din, attrFactory);
            cf.addAttribute(attr);
            if (attr instanceof InnerClassesAttr) {
                cf.mInnerClassesAttr = (InnerClassesAttr)attr;
            }
        }

        // Load inner and outer classes.
        if (cf.mInnerClassesAttr != null && loader != null) {
            InnerClassesAttr.Info[] infos =
                cf.mInnerClassesAttr.getInnerClassesInfo();
            for (int i=0; i<infos.length; i++) {
                InnerClassesAttr.Info info = infos[i];

                if (thisClass.equals(info.getInnerClass())) {
                    // This class is an inner class.
                    if (info.getInnerClassName() != null) {
                        cf.mInnerClassName = info.getInnerClassName();
                    }
                    ConstantClassInfo outer = info.getOuterClass();
                    if (cf.mOuterClass == null && outer != null) {
                        cf.mOuterClass = readOuterClass
                            (outer, loader, attrFactory, loadedClassFiles);
                    }
                    Modifiers innerFlags = info.getModifiers();
                    modifiers = modifiers
                        .toStatic(innerFlags.isStatic())
                        .toPrivate(innerFlags.isPrivate())
                        .toProtected(innerFlags.isProtected())
                        .toPublic(innerFlags.isPublic());
                } else if (thisClass.equals(info.getOuterClass())) {
                    // This class is an outer class.
                    ConstantClassInfo inner = info.getInnerClass();
                    if (inner != null) {
                        ClassFile innerClass = readInnerClass
                            (inner, loader, attrFactory, loadedClassFiles, cf);
                        
                        if (innerClass != null) {
                            if (innerClass.getInnerClassName() == null) {
                                innerClass.mInnerClassName =
                                    info.getInnerClassName();
                            }
                            if (cf.mInnerClasses == null) {
                                cf.mInnerClasses = new ArrayList();
                            }
                            cf.mInnerClasses.add(innerClass);
                        }
                    }
                }
            }
        }

        return cf;
    }

    private static ClassFile readOuterClass(ConstantClassInfo outer,
                                            ClassFileDataLoader loader,
                                            AttributeFactory attrFactory,
                                            Map loadedClassFiles)
        throws IOException
    {
        String name = outer.getType().getRootName();

        ClassFile outerClass = (ClassFile)loadedClassFiles.get(name);
        if (outerClass != null) {
            return outerClass;
        }

        InputStream in = loader.getClassData(name);
        if (in == null) {
            return null;
        }

        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }

        return readFrom
            ((DataInput)in, loader, attrFactory, loadedClassFiles, null);
    }

    private static ClassFile readInnerClass(ConstantClassInfo inner,
                                            ClassFileDataLoader loader,
                                            AttributeFactory attrFactory,
                                            Map loadedClassFiles,
                                            ClassFile outerClass)
        throws IOException
    {
        String name = inner.getType().getRootName();

        ClassFile innerClass = (ClassFile)loadedClassFiles.get(name);
        if (innerClass != null) {
            return innerClass;
        }

        InputStream in = loader.getClassData(name);
        if (in == null) {
            return null;
        }

        if (!(in instanceof DataInput)) {
            in = new DataInputStream(in);
        }

        return readFrom
            ((DataInput)in, loader, attrFactory, loadedClassFiles, outerClass);
    }

    private int mVersion;
    private String mTarget;
    {
        setTarget(null);
    }

    private final String mClassName;
    private final String mSuperClassName;
    private String mInnerClassName;
    private TypeDesc mType;

    private ConstantPool mCp;
    
    private Modifiers mModifiers;

    private ConstantClassInfo mThisClass;
    private ConstantClassInfo mSuperClass;
    
    // Holds ConstantInfo objects.
    private List mInterfaces = new ArrayList(2);
    private Set mInterfaceSet = new HashSet(7);
    
    // Holds objects.
    private List mFields = new ArrayList();
    private List mMethods = new ArrayList();
    private List mAttributes = new ArrayList();
    
    private SourceFileAttr mSource;

    private List mInnerClasses;
    private int mAnonymousInnerClassCount = 0;
    private InnerClassesAttr mInnerClassesAttr;

    // Is non-null for inner classes.
    private ClassFile mOuterClass;

    /** 
     * By default, the ClassFile defines public, non-final, concrete classes.
     * This constructor creates a ClassFile for a class that extends
     * java.lang.Object.
     * <p>
     * Use the {@link #getModifiers modifiers} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     */
    public ClassFile(String className) {
        this(className, (String)null);
    }
    
    /** 
     * By default, the ClassFile defines public, non-final, concrete classes.
     * <p>
     * Use the {@link #getModifiers modifiers} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     * @param superClass Super class.
     */
    public ClassFile(String className, Class superClass) {
        this(className, superClass.getName());
    }

    /** 
     * By default, the ClassFile defines public, non-final, concrete classes.
     * <p>
     * Use the {@link #getModifiers modifiers} to change the default
     * modifiers for this class or to turn it into an interface.
     *
     * @param className Full class name of the form ex: "java.lang.String".
     * @param superClassName Full super class name.
     */
    public ClassFile(String className, String superClassName) {
        if (superClassName == null) {
            if (!className.equals(Object.class.getName())) {
                superClassName = Object.class.getName();
            }
        }

        mCp = new ConstantPool();

        // public, non-final, concrete class
        mModifiers = Modifiers.PUBLIC;

        mThisClass = ConstantClassInfo.make(mCp, className);
        mSuperClass = ConstantClassInfo.make(mCp, superClassName);

        mClassName = className;
        mSuperClassName = superClassName;
    }

    /**
     * Used to construct a ClassFile when read from a stream.
     */
    private ClassFile(ConstantPool cp, Modifiers modifiers,
                      ConstantClassInfo thisClass,
                      ConstantClassInfo superClass,
                      ClassFile outerClass) {

        mCp = cp;

        mModifiers = modifiers;

        mThisClass = thisClass;
        mSuperClass = superClass;

        mClassName = thisClass.getType().getRootName();
        if (superClass == null) {
            mSuperClassName = null;
        } else {
            mSuperClassName = superClass.getType().getRootName();
        }

        mOuterClass = outerClass;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getSuperClassName() {
        return mSuperClassName;
    }

    /**
     * Returns a TypeDesc for the type of this ClassFile.
     */
    public TypeDesc getType() {
        if (mType == null) {
            mType = TypeDesc.forClass(mClassName);
        }
        return mType;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    /**
     * Returns the names of all the interfaces that this class implements.
     */
    public String[] getInterfaces() {
        int size = mInterfaces.size();
        String[] names = new String[size];

        for (int i=0; i<size; i++) {
            names[i] = ((ConstantClassInfo)mInterfaces.get(i))
                .getType().getRootName();
        }

        return names;
    }

    /**
     * Returns all the fields defined in this class.
     */
    public FieldInfo[] getFields() {
        FieldInfo[] fields = new FieldInfo[mFields.size()];
        return (FieldInfo[])mFields.toArray(fields);
    }

    /**
     * Returns all the methods defined in this class, not including
     * constructors and static initializers.
     */
    public MethodInfo[] getMethods() {
        int size = mMethods.size();
        List methodsOnly = new ArrayList(size);

        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            String name = method.getName();
            if (!"<init>".equals(name) && !"<clinit>".equals(name)) {
                methodsOnly.add(method);
            }
        }

        MethodInfo[] methodsArray = new MethodInfo[methodsOnly.size()];
        return (MethodInfo[])methodsOnly.toArray(methodsArray);
    }

    /**
     * Returns all the constructors defined in this class.
     */
    public MethodInfo[] getConstructors() {
        int size = mMethods.size();
        List ctorsOnly = new ArrayList(size);

        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            if ("<init>".equals(method.getName())) {
                ctorsOnly.add(method);
            }
        }

        MethodInfo[] ctorsArray = new MethodInfo[ctorsOnly.size()];
        return (MethodInfo[])ctorsOnly.toArray(ctorsArray);
    }

    /**
     * Returns the static initializer defined in this class or null if there
     * isn't one.
     */
    public MethodInfo getInitializer() {
        int size = mMethods.size();

        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            if ("<clinit>".equals(method.getName())) {
                return method;
            }
        }

        return null;
    }

    /**
     * Returns all the inner classes defined in this class. If no inner classes
     * are defined, then an array of length zero is returned.
     */
    public ClassFile[] getInnerClasses() {
        if (mInnerClasses == null) {
            return new ClassFile[0];
        }

        ClassFile[] innerClasses = new ClassFile[mInnerClasses.size()];
        return (ClassFile[])mInnerClasses.toArray(innerClasses);
    }

    /**
     * Returns true if this ClassFile represents an inner class.
     */
    public boolean isInnerClass() {
        return mOuterClass != null;
    }

    /**
     * If this ClassFile represents a non-anonymous inner class, returns its
     * short inner class name.
     */
    public String getInnerClassName() {
        return mInnerClassName;
    }

    /**
     * Returns null if this ClassFile does not represent an inner class.
     *
     * @see #isInnerClass()
     */
    public ClassFile getOuterClass() {
        return mOuterClass;
    }

    /**
     * Returns a value indicating how deeply nested an inner class is with
     * respect to its outermost enclosing class. For top level classes, 0
     * is returned. For first level inner classes, 1 is returned, etc.
     */
    public int getClassDepth() {
        int depth = 0;

        ClassFile outer = mOuterClass;
        while (outer != null) {
            depth++;
            outer = outer.mOuterClass;
        }

        return depth;
    }

    /**
     * Returns the source file of this class file or null if not set.
     */
    public String getSourceFile() {
        if (mSource == null) {
            return null;
        } else {
            return mSource.getFileName();
        }
    }

    public boolean isSynthetic() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SyntheticAttr) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof DeprecatedAttr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the signature attribute of this classfile, or null if none is
     * defined.
     */
    // TODO: Eventually remove this method
    public SignatureAttr getSignatureAttr() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SignatureAttr) {
                return (SignatureAttr) obj;
            }
        }
        return null;
    }

    /**
     * Provides access to the ClassFile's ContantPool.
     *
     * @return The constant pool for this class file.
     */
    public ConstantPool getConstantPool() {
        return mCp;
    }
    
    /**
     * Add an interface that this class implements.
     *
     * @param interfaceName Full interface name.
     */
    public void addInterface(String interfaceName) {
        if (!mInterfaceSet.contains(interfaceName)) {
            mInterfaces.add(ConstantClassInfo.make(mCp, interfaceName));
            mInterfaceSet.add(interfaceName);
        }
    }
    
    /**
     * Add an interface that this class implements.
     */
    public void addInterface(Class i) {
        addInterface(i.getName());
    }
    
    /**
     * Add a field to this class.
     */
    public FieldInfo addField(Modifiers modifiers,
                              String fieldName,
                              TypeDesc type) {
        FieldInfo fi = new FieldInfo(this, modifiers, fieldName, type);
        mFields.add(fi);
        return fi;
    }
    
    /**
     * Add a method to this class.
     *
     * @param ret Is null if method returns void.
     * @param params May be null if method accepts no parameters.
     */
    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                TypeDesc ret,
                                TypeDesc[] params) {
        MethodDesc md = MethodDesc.forArguments(ret, params);
        return addMethod(modifiers, methodName, md);
    }

    /**
     * Add a method to this class.
     */
    public MethodInfo addMethod(Modifiers modifiers,
                                String methodName,
                                MethodDesc md) {
        MethodInfo mi = new MethodInfo(this, modifiers, methodName, md);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add a method to this class. This method is handy for implementing
     * methods defined by a pre-existing interface.
     */
    public MethodInfo addMethod(Method method) {
        Modifiers modifiers = Modifiers.getInstance(method.getModifiers()).toAbstract(false);

        TypeDesc ret = TypeDesc.forClass(method.getReturnType());

        Class[] paramClasses = method.getParameterTypes();
        TypeDesc[] params = new TypeDesc[paramClasses.length];
        for (int i=0; i<params.length; i++) {
            params[i] = TypeDesc.forClass(paramClasses[i]);
        }

        MethodInfo mi = addMethod(modifiers, method.getName(), ret, params);
        
        // exception stuff...
        Class[] exceptions = method.getExceptionTypes();
        for (int i=0; i<exceptions.length; i++) {
            mi.addException(exceptions[i].getName());
        }

        return mi;
    }

    /**
     * Add a constructor to this class.
     *
     * @param params May be null if constructor accepts no parameters.
     */
    public MethodInfo addConstructor(Modifiers modifiers,
                                     TypeDesc[] params) {
        MethodDesc md = MethodDesc.forArguments(null, params);
        MethodInfo mi = new MethodInfo(this, modifiers, "<init>", md);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Adds a public, no-arg constructor with the code buffer properly defined.
     */
    public MethodInfo addDefaultConstructor() {
        MethodInfo mi = addConstructor(Modifiers.PUBLIC, null);
        CodeBuilder builder = new CodeBuilder(mi);
        builder.loadThis();
        builder.invokeSuperConstructor(null);
        builder.returnVoid();
        return mi;
    }

    /**
     * Add a static initializer to this class.
     */
    public MethodInfo addInitializer() {
        MethodDesc md = MethodDesc.forArguments(null, null);
        Modifiers af = Modifiers.NONE.toStatic(true);
        MethodInfo mi = new MethodInfo(this, af, "<clinit>", md);
        mMethods.add(mi);
        return mi;
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     */
    public ClassFile addInnerClass(String innerClassName) {
        return addInnerClass(innerClassName, (String)null);
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClass Super class.
     */
    public ClassFile addInnerClass(String innerClassName, Class superClass) {
        return addInnerClass(innerClassName, superClass.getName());
    }

    /**
     * Add an inner class to this class. By default, inner classes are private
     * static.
     *
     * @param innerClassName Optional short inner class name.
     * @param superClassName Full super class name.
     */
    public ClassFile addInnerClass(String innerClassName, 
                                   String superClassName) {
        String fullInnerClassName;
        if (innerClassName == null) {
            char sep = getMajorVersion() < 49 ? '$' : '+';
            fullInnerClassName = 
                mClassName + sep + (++mAnonymousInnerClassCount);
        } else {
            fullInnerClassName = mClassName + '$' + innerClassName;
        }

        ClassFile inner = new ClassFile(fullInnerClassName, superClassName);
        Modifiers modifiers = inner.getModifiers().toPrivate(true).toStatic(true);
        inner.mInnerClassName = innerClassName;
        inner.mOuterClass = this;

        if (mInnerClasses == null) {
            mInnerClasses = new ArrayList();
        }

        mInnerClasses.add(inner);
        
        // Record the inner class in this, the outer class.
        if (mInnerClassesAttr == null) {
            addAttribute(new InnerClassesAttr(mCp));
        }

        mInnerClassesAttr.addInnerClass(fullInnerClassName, mClassName, 
                                        innerClassName, modifiers);

        // Record the inner class in itself.
        inner.addAttribute(new InnerClassesAttr(inner.getConstantPool()));
        inner.mInnerClassesAttr.addInnerClass(fullInnerClassName, mClassName,
                                              innerClassName, modifiers);

        return inner;
    }

    /**
     * Set the source file of this class file by adding a source file
     * attribute. The source doesn't actually have to be a file,
     * but the virtual machine spec names the attribute "SourceFile_attribute".
     */
    public void setSourceFile(String fileName) {
        addAttribute(new SourceFileAttr(mCp, fileName));
    }

    /**
     * Mark this class as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this class as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    /**
     * Add an attribute to this class.
     */
    public void addAttribute(Attribute attr) {
        if (attr instanceof SourceFileAttr) {
            if (mSource != null) {
                mAttributes.remove(mSource);
            }
            mSource = (SourceFileAttr)attr;
        } else if (attr instanceof InnerClassesAttr) {
            if (mInnerClassesAttr != null) {
                mAttributes.remove(mInnerClassesAttr);
            }
            mInnerClassesAttr = (InnerClassesAttr)attr;
        }

        mAttributes.add(attr);
    }

    public Attribute[] getAttributes() {
        Attribute[] attrs = new Attribute[mAttributes.size()];
        return (Attribute[])mAttributes.toArray(attrs);
    }

    /**
     * Specify what target virtual machine version classfile should generate
     * for. Calling this method changes the major and minor version of the
     * classfile format.
     *
     * @param target VM version, 1.0, 1.1, etc.
     * @throws IllegalArgumentException if target is not supported
     */
    public void setTarget(String target) throws IllegalArgumentException {
        int major, minor;

        if (target == null || "1.0".equals(target) || "1.1".equals(target)) {
            major = 45; minor = 3;
            if (target == null) {
                target = "1.0";
            }
        } else if ("1.2".equals(target)) {
            major = 46; minor = 0;
        } else if ("1.3".equals(target)) {
            major = 47; minor = 0;
        } else if ("1.4".equals(target)) {
            major = 48; minor = 0;
        } else if ("1.5".equals(target)) {
            major = 49; minor = 0;
        } else {
            throw new IllegalArgumentException
                ("Unsupported target version: " + target);
        }
        
        mVersion = (minor << 16) | (major & 0xffff);
        mTarget = target.intern();
    }

    /**
     * Returns the target virtual machine version, or null if unknown.
     */
    public String getTarget() {
        return mTarget;
    }

    /**
     * Sets the version to use when writing the generated classfile, overriding
     * the target.
     */
    public void setVersion(int major, int minor) {
        if (major > 65535 || minor > 65535) {
            throw new IllegalArgumentException
                ("Version number element cannot exceed 65535");
        }

        mVersion = (minor << 16) | (major & 0xffff);

        String target;
        switch (major) {
        default:
            target = null;
            break;
        case 45:
            target = minor == 3 ? "1.0" : null;
            break;
        case 46:
            target = minor == 0 ? "1.2" : null;
            break;
        case 47:
            target = minor == 0 ? "1.3" : null;
            break;
        case 48:
            target = minor == 0 ? "1.4" : null;
            break;
        case 49:
            target = minor == 0 ? "1.5" : null;
            break;
        }

        mTarget = target;
    }

    /**
     * Returns the major version number of the classfile format.
     */
    public int getMajorVersion() {
        return mVersion & 0xffff;
    }

    /**
     * Returns the minor version number of the classfile format.
     */
    public int getMinorVersion() {
        return (mVersion >> 16) & 0xffff;
    }

    /**
     * Writes the ClassFile to the given OutputStream.
     */
    public void writeTo(OutputStream out) throws IOException {
        if (!(out instanceof DataOutput)) {
            out = new DataOutputStream(out);
        }
        writeTo((DataOutput)out);
    }

    /**
     * Writes the ClassFile to the given DataOutput.
     */
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeInt(MAGIC);
        dout.writeInt(mVersion);
        
        mCp.writeTo(dout);
        
        dout.writeShort(mModifiers.getBitmask() | Modifier.SYNCHRONIZED);

        dout.writeShort(mThisClass.getIndex());
        if (mSuperClass != null) {
            dout.writeShort(mSuperClass.getIndex());
        } else {
            dout.writeShort(0);
        }
        
        int size = mInterfaces.size();
        if (size > 65535) {
            throw new IllegalStateException
                ("Interfaces count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            int index = ((ConstantInfo)mInterfaces.get(i)).getIndex();
            dout.writeShort(index);
        }
        
        size = mFields.size();
        if (size > 65535) {
            throw new IllegalStateException
                ("Field count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            FieldInfo field = (FieldInfo)mFields.get(i);
            field.writeTo(dout);
        }
        
        size = mMethods.size();
        if (size > 65535) {
            throw new IllegalStateException
                ("Method count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            MethodInfo method = (MethodInfo)mMethods.get(i);
            method.writeTo(dout);
        }
        
        size = mAttributes.size();
        if (size > 65535) {
            throw new IllegalStateException
                ("Attribute count cannot exceed 65535: " + size);
        }
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = (Attribute)mAttributes.get(i);
            attr.writeTo(dout);
        }
    }

    // TODO: Add a toString method that dumps the descriptor like MethodInfo
    // and FieldInfo

}
