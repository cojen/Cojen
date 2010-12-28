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
 * thread. Cache permits null keys, but not null values.
 *
 * @author Brian S O'Neill
 * @see SoftValueCache
 * @see WeakKeyCache
 */
public class WeakValueCache<K, V> extends RefCache<K, V> {
    private Entry<K, V>[] mEntries;

    public WeakValueCache(int capacity) {
        super(capacity);
        mEntries = new Entry[capacity];
        mThreshold = (int) (capacity * LOAD_FACTOR);
    }

    @Override
    public synchronized V get(K key) {
        int hash = key == null ? 0 : keyHashCode(key);
        Entry<K, V>[] entries = mEntries;
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index]; e != null; e = e.mNext) {
            if (matches(e, key, hash)) {
                return e.get();
            }
        }
        return null;
    }

    @Override
    public synchronized V put(K key, V value) {
        int hash = key == null ? 0 : keyHashCode(key);
        Entry<K, V>[] entries = mEntries;
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index], prev = null; e != null; e = e.mNext) {
            if (matches(e, key, hash)) {
                V old = e.get();
                e.clear();
                Entry<K, V> newEntry;
                if (prev == null) {
                    newEntry = new Entry<K, V>(this, hash, key, value, e.mNext);
                } else {
                    prev.mNext = e.mNext;
                    newEntry = new Entry<K, V>(this, hash, key, value, entries[index]);
                }
                entries[index] = newEntry;
                return old;
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
        int hash = key == null ? 0 : keyHashCode(key);
        Entry<K, V>[] entries = mEntries;
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<K, V> e = entries[index], prev = null; e != null; e = e.mNext) {
            if (matches(e, key, hash)) {
                V old = e.get();
                e.clear();
                if (prev == null) {
                    entries[index] = e.mNext;
                } else {
                    prev.mNext = e.mNext;
                }
                mSize--;
                return old;
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
                V value = e.get();
                if (value != null) {
                    if (any) {
                        b.append(',').append(' ');
                    }
                    K key = e.mKey;
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

    private boolean matches(Entry<K, V> e, K key, int hash) {
        return hash == e.mHash && (key == null ? e.mKey == null : keyEquals(key, e.mKey));
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

    private static class Entry<K, V> extends WeakReference<V> implements CacheEvictor.Ref {
        final WeakValueCache<K, V> mCache;
        final int mHash;
        final K mKey;
        Entry<K, V> mNext;

        Entry(WeakValueCache<K, V> cache, int hash, K key, V value, Entry<K, V> next) {
            super(value, CacheEvictor.queue());
            mCache = cache;
            mHash = hash;
            mKey = key;
            mNext = next;
        }

        @Override
        public void remove() {
            mCache.removeCleared(this);
        }
    }
}
