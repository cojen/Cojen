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

import org.cojen.util.IntHashMap;

/**
 * Thread-safe registry of traced methods.
 *
 * @author Brian S O'Neill
 */
class TracedMethodRegistry {
    private final IntHashMap<TracedMethod> mMethodMap;

    private int mNextId;

    public TracedMethodRegistry() {
        mMethodMap = new IntHashMap<TracedMethod>();
    }

    /**
     * Returns a previously registered method.
     *
     * @param mid method identifier
     * @return registered method or null if not found
     */
    public synchronized TracedMethod getTracedMethod(int mid) {
        return mMethodMap.get(mid);
    }

    /**
     * Reserve an identifier for a method.
     *
     * @return new method identifier
     */
    public synchronized int reserveMethod(boolean root, boolean graft) {
        int id = ++mNextId;
        if (root) {
            id |= 0x80000000;
        }
        if (graft) {
            id |= 0x40000000;
        }
        mMethodMap.put(id, null);
        return id;
    }

    /**
     * Finish registering a method once it has been loaded.
     *
     * @param mid reserved method identifier
     * @param method method to assign to identifier
     * @throws IllegalStateException if id has already been registered or is unknown
     */
    public synchronized void registerMethod(int mid, TracedMethod method) {
        if (mMethodMap.get(mid) == null && mMethodMap.containsKey(mid)) {
            mMethodMap.put(mid, method);
        } else {
            throw new IllegalStateException();
        }
    }
}
