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

import java.lang.ref.WeakReference;

/**
 * Simple thread-safe cache which evicts entries via a shared background
 * thread. Cache permits null values, but not null keys.
 *
 * @author Brian S O'Neill
 * @see WeakValueCache
 * @see WeakIdentityCache
 */
public class WeakKeyCache<K, V> extends RefCache<K, V> {
    private Entry<K, V>[] mEntries;

    public WeakKeyCache(int capacity) {
        super(capacity);
        mEntries = new Entry[capacity];
        mThreshold = (int) (capacity * LOAD_FACTOR);
    }

    @Override
    public synchronized V get(K key) {
        int hash = keyHashCode(key);
        Entry<K, V>[] entries = mEntries;
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index]; e != null; e = e.mNext) {
            K k = e.get();
            if (k != null && e.mHash == hash && keyEquals(k, key)) {
                return e.mValue;
            }
        }
        return null;
    }

    @Override
    public synchronized V put(K key, V value) {
        int hash = keyHashCode(key);
        Entry<K, V>[] entries = mEntries;
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index], prev = null; e != null; e = e.mNext) {
            K k = e.get();
            if (k == null) {
                // Clean up after a cleared Reference.
                if (prev == null) {
                    entries[index] = e.mNext;
                } else {
                    prev.mNext = e.mNext;
                }
                mSize--;
            } else if (e.mHash == hash && keyEquals(k, key)) {
                Entry<K, V> newEntry;
                if (prev == null) {
                    newEntry = new Entry<K, V>(this, hash, key, value, e.mNext);
                } else {
                    prev.mNext = e.mNext;
                    newEntry = new Entry<K, V>(this, hash, key, value, entries[index]);
                }
                entries[index] = newEntry;
                return e.mValue;
            } else {
                prev = e;
            }
        }

        if (mSize >= mThreshold) {
            cleanup();
            if (mSize >= mThreshold) {
                rehash();
                entries = mEntries;
                index = (hash & 0x7fffffff) % entries.length;
            }
        }

        entries[index] = new Entry<K, V>(this, hash, key, value, entries[index]);
        mSize++;
        return null;
    }

    @Override
    public synchronized V remove(K key) {
        int hash = keyHashCode(key);
        Entry<K, V>[] entries = mEntries;
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index], prev = null; e != null; e = e.mNext) {
            K k = e.get();
            if (k == null) {
                // Clean up after a cleared Reference.
                if (prev == null) {
                    entries[index] = e.mNext;
                } else {
                    prev.mNext = e.mNext;
                }
                mSize--;
            } else if (e.mHash == hash && keyEquals(k, key)) {
                if (prev == null) {
                    entries[index] = e.mNext;
                } else {
                    prev.mNext = e.mNext;
                }
                mSize--;
                return e.mValue;
            } else {
                prev = e;
            }
        }
        return null;
    }

    @Override
    public synchronized void clear() {
        Entry[] entries = mEntries;
        for (int i=entries.length; --i>=0; ) {
            Entry e = entries[i];
            if (e != null) {
                e.clear();
                entries[i] = null;
            }
        }
        mSize = 0;
    }

    @Override
    public synchronized String toString() {
        if (isEmpty()) {
            return "{}";
        }

        StringBuilder b = new StringBuilder();
        b.append('{');

        Entry<K, V>[] entries = mEntries;
        boolean any = false;

        for (int i=entries.length; --i>=0 ;) {
            for (Entry<K, V> e = entries[i]; e != null; e = e.mNext) {
                K key = e.get();
                if (key != null) {
                    if (any) {
                        b.append(',').append(' ');
                    }
                    V value = e.mValue;
                    b.append(key).append('=').append(value);
                    any = true;
                }
            }
        }

        b.append('}');
        return b.toString();
    }

    synchronized void removeCleared(Entry<K, V> cleared) {
        Entry<K, V>[] entries = mEntries;
        int index = (cleared.mHash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index], prev = null; e != null; e = e.mNext) {
            if (e == cleared) {
                if (prev == null) {
                    entries[index] = e.mNext;
                } else {
                    prev.mNext = e.mNext;
                }
                mSize--;
                return;
            } else {
                prev = e;
            }
        }
    }

    private void cleanup() {
        Entry<K, V>[] entries = mEntries;
        int size = 0;

        for (int i=entries.length; --i>=0 ;) {
            for (Entry<K, V> e = entries[i], prev = null; e != null; e = e.mNext) {
                if (e.get() == null) {
                    // Clean up after a cleared Reference.
                    if (prev == null) {
                        entries[i] = e.mNext;
                    } else {
                        prev.mNext = e.mNext;
                    }
                } else {
                    size++;
                    prev = e;
                }
            }
        }

        mSize = size;
    }

    private void rehash() {
        Entry<K, V>[] oldEntries = mEntries;
        int newCapacity = oldEntries.length * 2 + 1;
        Entry<K, V>[] newEntries = new Entry[newCapacity];
        int size = 0;

        for (int i=oldEntries.length; --i>=0 ;) {
            for (Entry<K, V> old = oldEntries[i]; old != null; ) {
                Entry<K, V> e = old;
                old = old.mNext;
                // Only copy entry if its value hasn't been cleared.
                if (e.get() != null) {
                    size++;
                    int index = (e.mHash & 0x7fffffff) % newCapacity;
                    e.mNext = newEntries[index];
                    newEntries[index] = e;
                }
            }
        }

        mEntries = newEntries;
        mSize = size;
        mThreshold = (int) (newCapacity * LOAD_FACTOR);
    }

    private static class Entry<K, V> extends WeakReference<K> implements CacheEvictor.Ref {
        final WeakKeyCache<K, V> mCache;
        final int mHash;
        final V mValue;
        Entry<K, V> mNext;

        Entry(WeakKeyCache<K, V> cache, int hash, K key, V value, Entry<K, V> next) {
            super(key, CacheEvictor.queue());
            mCache = cache;
            mHash = hash;
            mValue = value;
            mNext = next;
        }

        @Override
        public void remove() {
            mCache.removeCleared(this);
        }
    }
}
