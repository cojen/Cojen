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
 *
 * @author Brian S O'Neill
 */
public abstract class RefCache<K, V> implements Cache<K, V> {
    static final float LOAD_FACTOR = 0.75f;

    int mSize;
    int mThreshold;

    RefCache(int capacity) {
        mThreshold = (int) (capacity * LOAD_FACTOR);
    }

    public final synchronized int size() {
        return mSize;
    }

    public final synchronized boolean isEmpty() {
        return mSize == 0;
    }

    public final synchronized V putIfAbsent(K key, V value) {
        V existing = get(key);
        return existing == null ? put(key, value) : existing;
    }

    public final synchronized boolean remove(K key, V value) {
        V existing = get(key);
        if (existing != null && existing.equals(value)) {
            remove(key);
            return true;
        } else {
            return false;
        }
    }

    public final synchronized boolean replace(K key, V oldValue, V newValue) {
        V existing = get(key);
        if (existing != null && existing.equals(oldValue)) {
            put(key, newValue);
            return true;
        } else {
            return false;
        }
    }

    public final synchronized V replace(K key, V value) {
        return get(key) == null ? null : put(key, value);
    }

    protected int keyHashCode(K key) {
        return key.hashCode();
    }

    protected boolean keyEquals(K a, K b) {
        return a.equals(b);
    }
}
