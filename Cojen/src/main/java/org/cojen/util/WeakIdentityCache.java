/*
 *  Copyright 2010 Brian S O'Neill
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

/**
 * Simple thread-safe cache which evicts entries via a shared background thread. Cache
 * permits null values, but not null keys. Keys are compared for equality via identity
 * comparison instead of using the key's built-in hashcode and equals methods.
 *
 * @author Brian S O'Neill
 * @see WeakKeyCache
 */
public class WeakIdentityCache<K, V> extends WeakKeyCache<K, V> {
    public WeakIdentityCache(int capacity) {
        super(capacity);
    }

    @Override
    protected final int keyHashCode(K key) {
        return System.identityHashCode(key);
    }

    @Override
    protected final boolean keyEquals(K a, K b) {
        return a == b;
    }
}
