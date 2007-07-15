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

import java.lang.ref.WeakReference;

import org.cojen.classfile.TypeDesc;

/**
 * Basic information regarding a traced method or constructor.
 *
 * @author Brian S O'Neill
 */
public class TracedMethod {
    private static final Class[] NO_PARAMS = new Class[0];

    /**
     * @param mid method identifier
     */
    public static boolean isRoot(int mid) {
        return (mid & 0x80000000) != 0;
    }

    /**
     * @param mid method identifier
     */
    public static boolean isGraft(int mid) {
        return (mid & 0x40000000) != 0;
    }

    private final int mMID;
    private final String mOperation;
    private final WeakReference<Class> mClass;
    private final String mMethodName;
    private final WeakReference<Class> mReturnType;
    private final WeakReference<Class>[] mParamTypes;

    private final String mStr;

    /**
     * @param mid reserved method identifier
     * @param operation optional operation name
     * @param clazz declaring class
     * @param methodName method name or null for constructor
     * @param returnType method return type, or null if void
     * @param paramTypes method or constructor parameter types, or null if none
     */
    TracedMethod(int mid, String operation,
                 Class clazz, String methodName, Class returnType, Class... paramTypes)
    {
        mMID = mid;
        mOperation = operation;

        StringBuilder b = new StringBuilder();

        b.append(returnType == null ? "void" : returnType.getName()).append(' ');

        b.append(TypeDesc.forClass(clazz).getFullName());
        if (methodName != null) {
            b.append('.').append(methodName);
        }
        b.append('(');

        mClass = new WeakReference<Class>(clazz);
        mMethodName = methodName;

        if (returnType == null) {
            mReturnType = null;
        } else {
            mReturnType = new WeakReference<Class>(returnType);
        }

        if (paramTypes == null || paramTypes.length == 0) {
            mParamTypes = null;
        } else {
            mParamTypes = new WeakReference[paramTypes.length];
            for (int i=0; i<paramTypes.length; i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(TypeDesc.forClass(paramTypes[i]).getFullName());
                mParamTypes[i] = new WeakReference<Class>(paramTypes[i]);
            }
        }

        mStr = b.append(')').toString();
    }

    /**
     * Returns the optional traced method operation.
     */
    public String getOperation() {
        return mOperation;
    }

    /**
     * Returns true if traced method is a constructor.
     */
    public boolean isConstructor() {
        return mMethodName == null;
    }

    /**
     * Returns the method's declaring class, or null if garbage collected.
     */
    public Class getDeclaringClass() {
        return mClass.get();
    }

    /**
     * Returns the method's name, or null if a constructor.
     */
    public String getMethodName() {
        return mMethodName;
    }

    /**
     * Returns the method's return type, which is null if void or if class has
     * been garbage collected.
     */
    public Class getReturnType() {
        return mReturnType == null ? null : mReturnType.get();
    }

    /**
     * Returns true if the method returns void.
     */
    public boolean isVoid() {
        return mReturnType == null;
    }

    /**
     * Returns the method's parameter types, which may contain null elements if
     * they have been garbage collected.
     */
    public Class[] getParameterTypes() {
        WeakReference<Class>[] paramTypes = mParamTypes;
        if (paramTypes == null) {
            return NO_PARAMS;
        }
        int length = paramTypes.length;
        Class[] classes = new Class[length];
        for (int i=length; --i>=0; ) {
            classes[i] = paramTypes[i].get();
        }
        return classes;
    }

    public boolean isRoot() {
        return isRoot(mMID);
    }

    public boolean isGraft() {
        return isGraft(mMID);
    }

    public int getMethodID() {
        return mMID;
    }

    @Override
    public String toString() {
        return mStr;
    }
}
