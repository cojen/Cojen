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

package cojen.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A thread-safe Set that manages flyweights: sharable objects that are usually
 * immutable. Call the {@link #put put} method for supplying the
 * WeakFlyweightSet with candidate flyweight instances.
 * <p>
 * Objects that do not customize the hashCode and equals methods don't make
 * sense to use as flyweights because each instance will be considered unique.
 * The object returned from the {@link #put put} method will always be the same
 * as the one passed in.
 *
 * @author Brian S O'Neill
 */
public class WeakFlyweightSet extends AbstractSet {
    private Entry[] table;
    private int count;
    private int threshold;
    private final float loadFactor;
    private final ReferenceQueue queue;

    public WeakFlyweightSet() {
        final int initialCapacity = 101;
        final float loadFactor = 0.75f;
        this.loadFactor = loadFactor;
        this.table = new Entry[initialCapacity];
        this.threshold = (int)(initialCapacity * loadFactor);
        this.queue = new ReferenceQueue();
    }

    /**
     * Pass in a candidate flyweight object and get a unique instance from this
     * set. The returned object will always be of the same type as that passed
     * in. If the object passed in does not equal any object currently in the
     * set, it will be added to the set, becoming a flyweight.
     *
     * @param obj candidate flyweight; null is also accepted
     */
    public synchronized Object put(Object obj) {
        // This implementation is based on the WeakIdentityMap.put method.

        if (obj == null) {
            return null;
        }

        Entry[] tab = this.table;

        // Cleanup after cleared References.
        {
            ReferenceQueue queue = this.queue;
            Reference ref;
            while ((ref = queue.poll()) != null) {
                // Since buckets are single-linked, traverse entire list and
                // cleanup all cleared references in it.
                int index = (((Entry) ref).hash & 0x7fffffff) % tab.length;
                for (Entry e = tab[index], prev = null; e != null; e = e.next) {
                    if (e.get() == null) {
                        if (prev != null) {
                            prev.next = e.next;
                        } else {
                            tab[index] = e.next;
                        }
                        this.count--;
                    } else {
                        prev = e;
                    }
                }
            }
        }

        int hash = hashCode(obj);
        int index = (hash & 0x7fffffff) % tab.length;

        for (Entry e = tab[index], prev = null; e != null; e = e.next) {
            Object iobj = e.get();
            if (iobj == null) {
                // Clean up after a cleared Reference.
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                this.count--;
            } else if (e.hash == hash &&
                       obj.getClass() == iobj.getClass() &&
                       equals(obj, iobj)) {
                // Found flyweight instance.
                return iobj;
            } else {
                prev = e;
            }
        }

        if (this.count >= this.threshold) {
            // Rehash the table if the threshold is exceeded.
            rehash();
            tab = this.table;
            index = (hash & 0x7fffffff) % tab.length;
        }

        // Create a new entry.
        tab[index] = new Entry(obj, this.queue, hash, tab[index]);
        this.count++;
        return obj;
    }

    public Iterator iterator() {
        return new SetIterator();
    }

    public int size() {
        return this.count;
    }

    public synchronized boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }

        Entry[] tab = this.table;
        int hash = hashCode(obj);
        int index = (hash & 0x7fffffff) % tab.length;

        for (Entry e = tab[index], prev = null; e != null; e = e.next) {
            Object iobj = e.get();
            if (iobj == null) {
                // Clean up after a cleared Reference.
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                this.count--;
            } else if (e.hash == hash &&
                     obj.getClass() == iobj.getClass() &&
                     equals(obj, iobj)) {
                // Found flyweight instance.
                return true;
            } else {
                prev = e;
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

    private void rehash() {
        int oldCapacity = this.table.length;
        Entry[] tab = this.table;

        int newCapacity = oldCapacity * 2 + 1;
        Entry[] newTab = new Entry[newCapacity];

        this.threshold = (int)(newCapacity * this.loadFactor);
        this.table = newTab;

        for (int i = oldCapacity; i-- > 0; ) {
            for (Entry old = tab[i]; old != null; ) {
                Entry e = old;
                old = old.next;

                // Only copy entry if it hasn't been cleared.
                if (e.get() == null) {
                    this.count--;
                } else {
                    int index = (e.hash & 0x7fffffff) % newCapacity;
                    e.next = newTab[index];
                    newTab[index] = e;
                }
            }
        }
    }

    private static class Entry extends WeakReference {
        int hash;
        Entry next;

        Entry(Object flyweight, ReferenceQueue queue, int hash, Entry next) {
            super(flyweight, queue);
            this.hash = hash;
            this.next = next;
        }
    }

    private class SetIterator implements Iterator {
        private final Entry[] table;

        private int index;

        // To ensure that the iterator doesn't return cleared entries, keep a
        // hard reference to the flyweight. Its existence will prevent the weak
        // reference from being cleared.
        private Object entryFlyweight;
        private Entry entry;

        SetIterator() {
            this.table = WeakFlyweightSet.this.table;
            this.index = table.length;
        }

        public boolean hasNext() {
            while (this.entry == null || (this.entryFlyweight = this.entry.get()) == null) {
                if (this.entry != null) {
                    // Skip past a cleared Reference.
                    this.entry = this.entry.next;
                } else {
                    if (this.index <= 0) {
                        return false;
                    } else {
                        this.entry = this.table[--this.index];
                    }
                }
            }

            return true;
        }

        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            this.entry = this.entry.next;
            return this.entryFlyweight;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
