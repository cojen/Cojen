/*
 *  Copyright 2004-2010 Brian S O'Neill
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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A thread-safe Set that manages canonical objects: sharable objects that are
 * typically immutable. Call the {@link #put put} method for supplying the
 * WeakCanonicalSet with candidate canonical instances.
 * <p>
 * Objects that do not customize the hashCode and equals methods don't make
 * sense to be canonicalized because each instance will be considered unique.
 * The object returned from the {@link #put put} method will always be the same
 * as the one passed in.
 *
 * @author Brian S O'Neill
 */
public class WeakCanonicalSet<T> extends AbstractSet<T> {
    private final float LOAD_FACTOR = 0.75f;

    private Entry<T>[] mEntries;
    private int mSize;
    private int mThreshold;

    public WeakCanonicalSet() {
        final int initialCapacity = 17;
        mEntries = new Entry[initialCapacity];
        mThreshold = (int)(initialCapacity * LOAD_FACTOR);
    }

    /**
     * Pass in a candidate canonical object and get a unique instance from this
     * set. The returned object will always be of the same type as that passed
     * in. If the object passed in does not equal any object currently in the
     * set, it will be added to the set, becoming canonical.
     *
     * @param obj candidate canonical object; null is also accepted
     */
    public synchronized <U extends T> U put(U obj) {
        if (obj == null) {
            return null;
        }

        Entry<T>[] entries = mEntries;
        int hash = hashCode(obj);
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<T> e = entries[index], prev = null; e != null; e = e.mNext) {
            T iobj = e.get();
            if (iobj == null) {
                // Clean up after a cleared Reference.
                if (prev == null) {
                    entries[index] = e.mNext;
                } else {
                    prev.mNext = e.mNext;
                }
                mSize--;
            } else if (e.mHash == hash && obj.getClass() == iobj.getClass() && equals(obj, iobj)) {
                // Found canonical instance.
                return (U) iobj;
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

        entries[index] = new Entry<T>(this, obj, hash, entries[index]);
        mSize++;
        return obj;
    }

    /**
     * Iterator is only thread safe if accessed while synchronized on WeakCanonicalSet
     * instance.
     */
    public synchronized Iterator<T> iterator() {
        return new SetIterator(mEntries);
    }

    public synchronized int size() {
        return mSize;
    }

    public synchronized boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }

        Entry<T>[] entries = mEntries;
        int hash = hashCode(obj);
        int index = (hash & 0x7fffffff) % entries.length;
        for (Entry<T> e = entries[index]; e != null; e = e.mNext) {
            Object iobj = e.get();
            if (iobj != null && e.mHash == hash && obj.getClass() == iobj.getClass() &&
                equals(obj, iobj))
            {
                // Found canonical instance.
                return true;
            }
        }

        return false;
    }

    public synchronized String toString() {
        return WeakIdentityMap.toString(this);
    }

    protected int hashCode(Object obj) {
        return obj.hashCode();
    }

    protected boolean equals(Object a, Object b) {
        return a.equals(b);
    }

    synchronized void removeCleared(Entry<T> cleared) {
        Entry<T>[] entries = mEntries;
        int index = (cleared.mHash & 0x7fffffff) % entries.length;

        for (Entry<T> e = entries[index], prev = null; e != null; e = e.mNext) {
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
        Entry<T>[] entries = mEntries;
        int size = 0;

        for (int i=entries.length; --i>=0 ;) {
            for (Entry<T> e = entries[i], prev = null; e != null; e = e.mNext) {
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
        Entry<T>[] oldEntries = mEntries;
        int newCapacity = oldEntries.length * 2 + 1;
        Entry<T>[] newEntries = new Entry[newCapacity];
        int size = 0;

        for (int i=oldEntries.length; --i>=0 ;) {
            for (Entry<T> old = oldEntries[i]; old != null; ) {
                Entry<T> e = old;
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

    private static class Entry<T> extends WeakReference<T> implements CacheEvictor.Ref {
        final WeakCanonicalSet mSet;
        final int mHash;
        Entry<T> mNext;

        Entry(WeakCanonicalSet<T> set, T canonical, int hash, Entry<T> next) {
            super(canonical, CacheEvictor.queue());
            mSet = set;
            mHash = hash;
            mNext = mNext;
        }

        @Override
        public void remove() {
            mSet.removeCleared(this);
        }
    }

    private class SetIterator implements Iterator<T> {
        private final Entry<T>[] mEntries;

        private int mIndex;

        // To ensure that the iterator doesn't return cleared entries, keep a
        // hard reference to the canonical object. Its existence will prevent
        // the weak reference from being cleared.
        private T mEntryCanonical;
        private Entry<T> mEntry;

        SetIterator(Entry<T>[] entries) {
            mEntries = entries;
            mIndex = mEntries.length;
        }

        public boolean hasNext() {
            while (mEntry == null || (mEntryCanonical = mEntry.get()) == null) {
                if (mEntry != null) {
                    // Skip past a cleared Reference.
                    mEntry = mEntry.mNext;
                } else {
                    if (mIndex <= 0) {
                        return false;
                    } else {
                        mEntry = mEntries[--mIndex];
                    }
                }
            }

            return true;
        }

        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            mEntry = mEntry.mNext;
            return mEntryCanonical;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
