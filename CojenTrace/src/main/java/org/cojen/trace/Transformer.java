/*
 *  Copyright 2006 Brian S O'Neill
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

package org.cojen.trace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;

import java.lang.reflect.Method;

import java.security.ProtectionDomain;

import java.util.HashMap;
import java.util.Map;

import org.cojen.classfile.attribute.Annotation;
import org.cojen.classfile.attribute.AnnotationsAttr;

import org.cojen.classfile.Attribute;
import org.cojen.classfile.ClassFile;
import org.cojen.classfile.CodeAssembler;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.CodeDisassembler;
import org.cojen.classfile.DelegatedCodeAssembler;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.Opcode;
import org.cojen.classfile.TypeDesc;

import org.cojen.classfile.constant.ConstantIntegerInfo;

import static org.cojen.trace.TraceMode.*;
import static org.cojen.trace.TraceModes.*;

/**
 * 
 *
 * @author Brian S O'Neill
 */
class Transformer implements ClassFileTransformer {
    private static final String HANDLER_FIELD = "handler$";

    private final TraceAgent mAgent;

    Transformer(TraceAgent agent) {
        mAgent = agent;
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer)
        throws IllegalClassFormatException
    {
        if (loader == null) {
            // Cannot transform classes loaded by bootstrap loader.
            return null;
        }

        // Test if loader can access TraceAgent.
        try {
            loader.loadClass(TraceAgent.class.getName());
        } catch (ClassNotFoundException e) {
            return null;
        }

        className = className.replace('/', '.');

        // Handle special cases...
        if (className.startsWith("org.cojen.trace.") ||
            className.startsWith("sun.reflect."))
        {
            return null;
        }

        TraceModes modes = mAgent.getTraceModes(className);

        if (modes == null || modes == ALL_OFF) {
            return null;
        }

        ClassFile cf;
        try {
            cf = ClassFile.readFrom(new ByteArrayInputStream(classfileBuffer));
        } catch (Exception e) {
            IllegalClassFormatException e2 = new IllegalClassFormatException();
            e2.initCause(e);
            throw e2;
        }

        if (cf.getModifiers().isInterface()) {
            // Short-circuit. Nothing to transform.
            return null;
        }

        Map<MethodInfo, Integer> transformedMethodIDs = new HashMap<MethodInfo, Integer>();

        for (MethodInfo mi : cf.getMethods()) {
            tryTransform(modes, transformedMethodIDs, mi);
        }

        for (MethodInfo mi : cf.getConstructors()) {
            tryTransform(modes, transformedMethodIDs, mi);
        }

        if (transformedMethodIDs.size() == 0) {
            // Class is unchanged.
            return null;
        }

        // Add field for holding reference to handler.
        cf.addField(Modifiers.PRIVATE.toStatic(true),
                    HANDLER_FIELD, TypeDesc.forClass(TraceHandler.class));
        
        // Add or prepend static initializer for getting handler and
        // registering methods.
        {
            MethodInfo clinit = cf.getInitializer();
            CodeDisassembler dis;
            if (clinit == null) {
                dis = null;
                clinit = cf.addInitializer();
            } else {
                dis = new CodeDisassembler(clinit);
            }

            CodeBuilder b = new CodeBuilder(clinit);

            TypeDesc agentType = TypeDesc.forClass(TraceAgent.class);
            LocalVariable agentVar = b.createLocalVariable("agent", agentType);
            b.loadConstant(mAgent.getAgentID());
            b.invokeStatic(agentType, "getTraceAgent",
                           agentType, new TypeDesc[] {TypeDesc.LONG});
            b.storeLocal(agentVar);

            TypeDesc handlerType = TypeDesc.forClass(TraceHandler.class);
            b.loadLocal(agentVar);
            b.invokeVirtual(agentType, "getTraceHandler", handlerType, null);
            b.storeStaticField(HANDLER_FIELD, handlerType);

            // Finish registering each method.
            TypeDesc classType = TypeDesc.forClass(Class.class);
            TypeDesc classArrayType = classType.toArrayType();
            TypeDesc methodType = TypeDesc.forClass(Method.class);

            for (Map.Entry<MethodInfo, Integer> entry : transformedMethodIDs.entrySet()) {
                MethodInfo mi = entry.getKey();
                int mid = entry.getValue();

                // For use below when calling registerTraceMethod.
                b.loadLocal(agentVar);
                b.loadConstant(mid);

                b.loadConstant(cf.getType());
                b.loadConstant(mi.getName().equals("<init>") ? null : mi.getName());
                TypeDesc[] types = mi.getMethodDescriptor().getParameterTypes();
                if (types.length == 0) {
                    b.loadNull();
                } else {
                    b.loadConstant(types.length);
                    b.newObject(classArrayType);
                    for (int i=0; i<types.length; i++) {
                        // dup array
                        b.dup();
                        b.loadConstant(i);
                        b.loadConstant(types[i]);
                        b.storeToArray(classType);
                    }
                }

                b.invokeVirtual(agentType, "registerTraceMethod", null, new TypeDesc[]
                    {TypeDesc.INT, classType, TypeDesc.STRING, classArrayType});
            }

            if (dis == null) {
                b.returnVoid();
            } else {
                dis.disassemble(b);
            }
        }

        // Define the newly transformed class.

        /* debugging
        try {
            java.io.FileOutputStream fout =
                new java.io.FileOutputStream(cf.getClassName() + ".class");
            cf.writeTo(fout);
            fout.close();
        } catch (Exception e) {
            System.out.println("Error defining: " + cf.getClassName());
            e.printStackTrace(System.out);
        }
        */

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            cf.writeTo(out);
            out.close();
        } catch (Exception e) {
            IllegalClassFormatException e2 = new IllegalClassFormatException();
            e2.initCause(e);
            throw e2;
        }

        return out.toByteArray();
    }

    private Boolean getBooleanParam(Map<String, Annotation.MemberValue> memberValues,
                                    String paramName)
    {
        Annotation.MemberValue mv = memberValues.get(paramName);
        Object constant;
        if (mv != null && (constant = mv.getValue()) != null
            && constant instanceof ConstantIntegerInfo) {
            return ((ConstantIntegerInfo) constant).getValue() != 0;
        }
        return null;
    }

    private boolean getBooleanParam(Map<String, Annotation.MemberValue> memberValues,
                                    String paramName,
                                    boolean defaultValue)
    {
        Boolean value = getBooleanParam(memberValues, paramName);
        return value == null ? defaultValue : value;
    }

    private void tryTransform(TraceModes modes,
                              Map<MethodInfo, Integer> transformedMethodIDs,
                              MethodInfo mi)
    {
        if (mi.getModifiers().isAbstract() || mi.getModifiers().isNative()) {
            return;
        }

        Annotation traceAnnotation = null;
        findTrace: {
            Annotation[] annotations = mi.getRuntimeInvisibleAnnotations();
            for (Annotation ann : annotations) {
                if (ann.getType().getFullName().equals(Trace.class.getName())) {
                    traceAnnotation = ann;
                    break findTrace;
                }
            }
        }

        boolean args, result, exception, time, alloc, root, graft;
        alloc = false;

        if (traceAnnotation == null) {
            // If no user trace annotation exists, check if any trace mode is
            // on which will turn on trace anyhow.

            args = modes.getTraceArguments() == ON;
            result = modes.getTraceResult() == ON;
            exception = modes.getTraceException() == ON;
            time = modes.getTraceTime() == ON;

            if (!args && !result && !exception && !time) {
                if (modes.getTraceCalls() != ON) {
                    // No features forced on, so don't trace.
                    return;
                }
            }

            root = false;
            graft = false;
        } else {
            // Extract trace parameters. Defaults copied from Trace annotation.

            Map<String, Annotation.MemberValue> memberValues = traceAnnotation.getMemberValues();
            
            args = getBooleanParam(memberValues, "args", false);
            if (modes.getTraceArguments() != USER) {
                args = modes.getTraceArguments() == ON;
            }

            result = getBooleanParam(memberValues, "result", false);
            if (modes.getTraceResult() != USER) {
                result = modes.getTraceResult() == ON;
            }

            exception = getBooleanParam(memberValues, "exception", false);
            if (modes.getTraceException() != USER) {
                exception = modes.getTraceException() == ON;
            }

            time = getBooleanParam(memberValues, "time", true);
            if (modes.getTraceTime() != USER) {
                time = modes.getTraceTime() == ON;
            }

            if (!args && !result && !exception && !time) {
                if (modes.getTraceCalls() == OFF) {
                    // No features on, so don't trace.
                    return;
                }
            }

            root = getBooleanParam(memberValues, "root", false);
            graft = getBooleanParam(memberValues, "graft", false);
        }

        int mid = transform(mi, args, result, exception, time, alloc, root, graft);

        transformedMethodIDs.put(mi, mid);
    }

    /**
     * @param args when true, pass method arguments to trace handler
     * @param result when true, pass method return value to trace handler
     * @param exception when true, pass thrown exception to trace handler
     * @param time when true, pass method execution time to trace handler
     * @param root when true, indicate to trace handler that method should be reported as root
     * @param graft when true, indicate to trace handler that method should be reported as graft
     * @param alloc when true, pass new objects to trace handler
     * @return method id
     */
    private int transform(MethodInfo mi,
                          boolean args,
                          boolean result,
                          boolean exception,
                          boolean time,
                          boolean alloc,
                          boolean root,
                          boolean graft)
    {
        if (mi.getMethodDescriptor().getParameterCount() == 0) {
            args = false;
        }
        if (mi.getMethodDescriptor().getReturnType() == TypeDesc.VOID) {
            result = false;
        }

        int mid = mAgent.reserveMethod(root, graft);

        CodeDisassembler dis = new CodeDisassembler(mi);
        CodeBuilder b = new CodeBuilder(mi);

        // Call enterMethod
        {
            b.loadStaticField(HANDLER_FIELD, TypeDesc.forClass(TraceHandler.class));

            b.loadConstant(mid);

            int argCount = mi.getMethodDescriptor().getParameterCount();
            TypeDesc[] params;
            if (!args || argCount == 0) {
                params = new TypeDesc[] {TypeDesc.INT};
            } else if (argCount == 1) {
                params = new TypeDesc[] {TypeDesc.INT, TypeDesc.OBJECT};
                b.loadLocal(b.getParameter(0));
                b.convert(b.getParameter(0).getType(), TypeDesc.OBJECT);
            } else {
                params = new TypeDesc[] {TypeDesc.INT, TypeDesc.OBJECT.toArrayType()};
                b.loadConstant(argCount);
                b.newObject(TypeDesc.OBJECT.toArrayType());
                for (int i=0; i<argCount; i++) {
                    // dup array
                    b.dup();
                    b.loadConstant(i);
                    b.loadLocal(b.getParameter(i));
                    b.convert(b.getParameter(i).getType(), TypeDesc.OBJECT);
                    b.storeToArray(TypeDesc.OBJECT);
                }
            }

            b.invokeInterface(TypeDesc.forClass(TraceHandler.class), "enterMethod", null, params);
        }

        LocalVariable startTime = null;
        if (time) {
            startTime = b.createLocalVariable("startTime", TypeDesc.LONG);
            b.invokeStatic(TypeDesc.forClass(System.class), "nanoTime", TypeDesc.LONG, null);
            b.storeLocal(startTime);
        }

        Label tryStart = b.createLabel().setLocation();
        Label tryEnd = b.createLabel();

        if (!alloc) {
            dis.disassemble(b, null, tryEnd);
        } else {
            dis.disassemble(new AllocTracer(b, mid), null, tryEnd);
        }

        tryEnd.setLocation();

        // Fall to this point for normal exit.
        {
            // Save result in local variable to pass to exitMethod (if result passing enabled)
            LocalVariable resultVar = null;
            if (result) {
                resultVar = b.createLocalVariable
                    ("result", mi.getMethodDescriptor().getReturnType());
                b.storeLocal(resultVar);
            }
            
            // Prepare call to exit method
            b.loadStaticField(HANDLER_FIELD, TypeDesc.forClass(TraceHandler.class));
            b.loadConstant(mid);
            
            TypeDesc[] exitMethodParams;
            if (time) {
                if (result) {
                    exitMethodParams = new TypeDesc[] {
                        TypeDesc.INT, TypeDesc.OBJECT, TypeDesc.LONG
                    };
                    b.loadLocal(resultVar);
                    b.convert(resultVar.getType(), TypeDesc.OBJECT);
                } else {
                    exitMethodParams = new TypeDesc[] {TypeDesc.INT, TypeDesc.LONG};
                }
            } else if (result) {
                exitMethodParams = new TypeDesc[] {TypeDesc.INT, TypeDesc.OBJECT};
                b.loadLocal(resultVar);
                b.convert(resultVar.getType(), TypeDesc.OBJECT);
            } else {
                exitMethodParams = new TypeDesc[] {TypeDesc.INT};
            }
            
            if (time) {
                b.invokeStatic(TypeDesc.forClass(System.class), "nanoTime", TypeDesc.LONG, null);
                b.loadLocal(startTime);
                b.math(Opcode.LSUB);
                // Leave on stack for exitMethod call.
            }
            
            // Call exitMethod
            b.invokeInterface(TypeDesc.forClass(TraceHandler.class), "exitMethod",
                              null, exitMethodParams);
            
            if (result) {
                b.loadLocal(resultVar);
            }
            b.returnValue(mi.getMethodDescriptor().getReturnType());
        }

        b.exceptionHandler(tryStart, tryEnd, null);

        // Fall to this point for exception exit.

        {
            // Save exception in local variable to pass to exitMethod (if
            // exception passing enabled)
            TypeDesc throwableType = TypeDesc.forClass(Throwable.class);
            LocalVariable exceptionVar = null;
            if (exception) {
                exceptionVar = b.createLocalVariable("e", throwableType);
                b.storeLocal(exceptionVar);
            }
            
            b.loadStaticField(HANDLER_FIELD, TypeDesc.forClass(TraceHandler.class));
            b.loadConstant(mid);
            
            TypeDesc[] exitMethodParams;
            if (time) {
                if (exception) {
                    exitMethodParams = new TypeDesc[] {TypeDesc.INT, throwableType, TypeDesc.LONG};
                    b.loadLocal(exceptionVar);
                } else {
                    exitMethodParams = new TypeDesc[] {TypeDesc.INT, TypeDesc.LONG};
                }
            } else if (exception) {
                exitMethodParams = new TypeDesc[] {TypeDesc.INT, throwableType};
                b.loadLocal(exceptionVar);
            } else {
                exitMethodParams = new TypeDesc[] {TypeDesc.INT};
            }
            
            if (time) {
                b.invokeStatic(TypeDesc.forClass(System.class), "nanoTime", TypeDesc.LONG, null);
                b.loadLocal(startTime);
                b.math(Opcode.LSUB);
                // Leave on stack for exitMethod call.
            }
            
            // Call exitMethod
            b.invokeInterface(TypeDesc.forClass(TraceHandler.class), "exitMethod",
                              null, exitMethodParams);

            if (exception) {
                b.loadLocal(exceptionVar);
            }
            b.throwObject();
        }

        return mid;
    }

    private static class AllocTracer extends DelegatedCodeAssembler {
        private final int mMethodID;

        private int mNewObjectCount;

        AllocTracer(CodeAssembler assembler, int mid) {
            super(assembler);
            mMethodID = mid;
        }

        public void newObject(TypeDesc type) {
            mAssembler.newObject(type);
            if (type.isArray()) {
                callTraceMethod();
            } else {
                // Regular objects are traced after the constructor has been
                // called. Constructor calls of the form "this(...)" also call
                // invokeConstructor, but there is no matching
                // newObject. Distinguish these cases by using a counter.
                mNewObjectCount++;
            }
        }

        public void newObject(TypeDesc type, int dimensions) {
            mAssembler.newObject(type, dimensions);
            callTraceMethod();
        }

        public void invokeConstructor(TypeDesc[] params) {
            mAssembler.invokeConstructor(params);
            if (mNewObjectCount > 0) {
                mNewObjectCount--;
                callTraceMethod();
            }
        }

        public void invokeConstructor(TypeDesc classDesc, TypeDesc[] params) {
            mAssembler.invokeConstructor(classDesc, params);
            if (mNewObjectCount > 0) {
                mNewObjectCount--;
                callTraceMethod();
            }
        }

        public void invokeConstructor(String className, TypeDesc[] params) {
            mAssembler.invokeConstructor(className, params);
            if (mNewObjectCount > 0) {
                mNewObjectCount--;
                callTraceMethod();
            }
        }

        private void callTraceMethod() {
            // stack: obj
            dup();
            // stack: obj, obj
            loadStaticField(HANDLER_FIELD, TypeDesc.forClass(TraceHandler.class));
            // stack: obj, obj, handler
            swap();
            // stack: obj, handler, obj
            loadConstant(mMethodID);
            // stack: obj, handler, obj, int
            swap();
            // stack: obj, handler, int, obj
            invokeInterface(TypeDesc.forClass(TraceHandler.class), "newObject", null,
                            new TypeDesc[] {TypeDesc.INT, TypeDesc.OBJECT});
            // stack: obj
        }
    }
}
