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
    private final WeakReference<Class> mClass;
    private final String mName;
    private final WeakReference<Class>[] mParamTypes;

    private final String mStr;

    /**
     * @param mid reserved method identifier
     * @param clazz declaring class
     * @param name method name or null for constructor
     * @param paramTypes method or constructor parameter types, or null if none
     */
    TracedMethod(int mid, Class clazz, String name, Class... paramTypes) {
        mMID = mid;

        StringBuilder b = new StringBuilder();
        b.append(TypeDesc.forClass(clazz).getFullName());
        if (name != null) {
            b.append('.').append(name);
        }
        b.append('(');

        mClass = new WeakReference<Class>(clazz);
        mName = name;

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
     * Returns true if traced method is a constructor.
     */
    public boolean isConstructor() {
        return mName == null;
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
    public String getName() {
        return mName;
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

    public String toString() {
        return mStr;
    }
}
