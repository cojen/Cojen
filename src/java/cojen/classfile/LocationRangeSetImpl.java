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

package cojen.classfile;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 
 * @author Brian S O'Neill
 */
class LocationRangeSetImpl extends AbstractSet implements SortedSet {
    private final SortedSet mSet;

    public LocationRangeSetImpl() {
        mSet = new TreeSet(LocationRangeComparator.SINGLETON);
    }

    private LocationRangeSetImpl(SortedSet set) {
        mSet = set;
    }

    public Iterator iterator() {
        return mSet.iterator();
    }

    public int size() {
        return mSet.size();
    }
    
    public boolean isEmpty() {
        return mSet.isEmpty();
    }

    public boolean contains(Object obj) {
        return contains((LocationRange)obj);
    }

    public boolean contains(LocationRange range) {
        if (range.getStartLocation().compareTo(range.getEndLocation()) >= 0) {
            // Always contains empty range.
            return true;
        }

        // TODO
        return mSet.contains(range);
    }

    public Object[] toArray() {
        return mSet.toArray();
    }

    public Object[] toArray(Object[] array) {
        return mSet.toArray(array);
    }

    public boolean add(Object obj) {
        return add((LocationRange)obj);
    }

    public boolean add(LocationRange range) {
        if (contains(range)) {
            return false;
        }

        // Try to reduce the set by joining adjacent ranges or eliminating
        // overlap.
        
        LocationRange before, after;

        SortedSet head = mSet.headSet(range);
        if (head.size() == 0) {
            before = null;
        } else {
            before = (LocationRange)head.last();
        }

        Location newStart = range.getStartLocation();
        Location newEnd = range.getEndLocation();

        if (before != null) {
            if (newStart.compareTo(before.getEndLocation()) <= 0) {
                if (before.getEndLocation().compareTo(newEnd) <= 0) {
                    mSet.remove(before);
                    return add(new LocationRangeImpl(before, range));
                } else {
                    return false;
                }
            }
        }

        SortedSet tail = mSet.tailSet(range);
        if (tail.size() == 0) {
            after = null;
        } else {
            after = (LocationRange)tail.first();
        }

        if (after != null) {
            if (newEnd.compareTo(after.getStartLocation()) >= 0) {
                if (after.getStartLocation().compareTo(newStart) >= 0) {
                    mSet.remove(after);
                    return add(new LocationRangeImpl(range, after));
                } else {
                    return false;
                }
            }
        }

        return mSet.add(range);
    }

    public boolean remove(Object obj) {
        return remove((LocationRange)obj);
    }

    public boolean remove(LocationRange range) {
        // TODO
        return mSet.remove(range);
        /*
        if (!contains(range)) {
            return false;
        }

        // Try to reduce the set by joining adjacent ranges or eliminating
        // overlap.
        
        LocationRange before, after;

        SortedSet head = mSet.headSet(range);
        if (head.size() == 0) {
            before = null;
        } else {
            before = (LocationRange)head.last();
        }

        Location newStart = range.getStartLocation();
        Location newEnd = range.getEndLocation();

        if (before != null) {
            if (newStart.compareTo(before.getEndLocation()) <= 0) {
                if (before.getEndLocation().compareTo(newEnd) <= 0) {
                    mSet.remove(before);
                    return add(new LocationRangeImpl(before, range));
                } else {
                    return false;
                }
            }
        }

        SortedSet tail = mSet.tailSet(range);
        if (tail.size() == 0) {
            after = null;
        } else {
            after = (LocationRange)tail.first();
        }

        if (after != null) {
            if (newEnd.compareTo(after.getStartLocation()) >= 0) {
                if (after.getStartLocation().compareTo(newStart) >= 0) {
                    mSet.remove(after);
                    return add(new LocationRangeImpl(range, after));
                } else {
                    return false;
                }
            }
        }

        return mSet.add(range);
        */
    }

    public void clear() {
        mSet.clear();
    }

    public Comparator comparator() {
        return mSet.comparator();
    }

    public SortedSet subSet(Object from, Object to) {
        return new LocationRangeSetImpl(mSet.subSet(from, to));
    }

    public SortedSet headSet(Object to) {
        return new LocationRangeSetImpl(mSet.headSet(to));
    }

    public SortedSet tailSet(Object from) {
        return new LocationRangeSetImpl(mSet.tailSet(from));
    }

    public Object first() {
        return mSet.first();
    }

    public Object last() {
        return mSet.last();
    }
}
