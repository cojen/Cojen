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

/**
 * A specialized, faster BitSet.
 *
 * @author Brian S O'Neill
 */
// TODO: Remove this class
class BitList implements Cloneable, java.io.Serializable {
    /**
     * Test program.
     */
    public static void main(String[] args) throws Exception {
        BitList a = new BitList(50);
        a.set(4);
        a.set(5);

        BitList b = new BitList(50);
        b.set(6);
        b.set(7);
        System.out.println(b);

        BitList c = (BitList)a.clone();
        c.and(b);
        System.out.println(c);

        a.set(6);
        c = (BitList)a.clone();
        c.and(b);
        System.out.println(c);
        c.not();
        System.out.println(c);
        System.out.println(c.get(0));
        System.out.println(c.get(40));
        System.out.println(c.get(6));

        BitList bl = new BitList(200);
        for (int i=0; i<200; i += 2) {
            bl.set(i);
            System.out.println(bl);
        }
    }

    // Bits are stored little endian.
    private long[] mData;
    
    public BitList(int capacity) {
        mData = new long[(capacity + 63) / 64];
    }

    private void ensureCapacity(int capacity) {
        int len = (capacity + 63) / 64;
        if (len > mData.length) {
            long[] newData = new long[len];
            System.arraycopy(mData, 0, newData, 0, mData.length);
            mData = newData;
        }
    }

    public boolean get(int index) {
        long val = mData[index / 64];
        if (val == 0) {
            return false;
        } else if (val == 0xffffffffffffffffL) {
            return true;
        } else {
            return (val & (0x8000000000000000L >>> index)) != 0;
        }
    }

    public void set(int index) {
        mData[index / 64] |= (0x8000000000000000L >>> index);
    }

    public void set() {
        for (int i=mData.length; --i >= 0; ) {
            mData[i] = 0xffffffffffffffffL;
        }
    }

    public void clear(int index) {
        mData[index / 64] &= ~(0x8000000000000000L >>> index);
    }

    public void clear() {
        for (int i=mData.length; --i >= 0; ) {
            mData[i] = 0;
        }
    }

    public void not() {
        for (int i=mData.length; --i >= 0; ) {
            mData[i] ^= 0xffffffffffffffffL;
        }
    }

    public void and(BitList list) {
        ensureCapacity(list.capacity());
        for (int i=list.mData.length; --i >= 0; ) {
            mData[i] &= list.mData[i];
        }
    }

    public void andNot(BitList list) {
        ensureCapacity(list.capacity());
        for (int i=list.mData.length; --i >= 0; ) {
            mData[i] &= ~list.mData[i];
        }
    }

    public void or(BitList list) {
        ensureCapacity(list.capacity());
        for (int i=list.mData.length; --i >= 0; ) {
            mData[i] |= list.mData[i];
        }
    }

    public void xor(BitList list) {
        ensureCapacity(list.capacity());
        for (int i=list.mData.length; --i >= 0; ) {
            mData[i] ^= list.mData[i];
        }
    }

    public boolean isAllClear() {
        for (int i=mData.length; --i >= 0; ) {
            if (mData[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isAllSet() {
        for (int i=mData.length; --i >= 0; ) {
            if (mData[i] != 0xffffffffffffffffL) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        long hash = 0;
        for (int i=mData.length; --i >= 0; ) {
            hash = hash * 63 + mData[i];
        }
        return (int)hash;
    }

    public int capacity() {
        return mData.length * 64;
    }

    public boolean equals(Object obj) {
        if (obj instanceof BitList) {
            return java.util.Arrays.equals(mData, ((BitList)obj).mData);
        }
        return false;
    }

    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(mData.length + 2);
        buf.append('[');
        for (int i=0; i<mData.length; i++) {
            String binary = Long.toBinaryString(mData[i]);
            for (int j=binary.length(); j<64; j++) {
                buf.append('0');
            }
            buf.append(binary);
        }
        buf.append(']');
        return buf.toString();
    }
}
