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

import java.util.Arrays;

/**
 * KeyFactory generates keys which can be hashed or compared for any kind of
 * object including arrays, arrays of arrays, and null. All hashcode
 * computations, equality tests, and ordering comparsisons fully recurse into
 * arrays.
 *
 * @author Brian S O'Neill
 */
public class KeyFactory {
    static final Object NULL = new Comparable() {
        public int compareTo(Object obj) {
            return obj == this || obj == null ? 0 : 1;
        }
    };

    public static Object createKey(boolean[] obj) {
        return obj == null ? NULL : new BooleanArrayKey(obj);
    }

    public static Object createKey(byte[] obj) {
        return obj == null ? NULL : new ByteArrayKey(obj);
    }

    public static Object createKey(char[] obj) {
        return obj == null ? NULL : new CharArrayKey(obj);
    }

    public static Object createKey(double[] obj) {
        return obj == null ? NULL : new DoubleArrayKey(obj);
    }

    public static Object createKey(float[] obj) {
        return obj == null ? NULL : new FloatArrayKey(obj);
    }

    public static Object createKey(int[] obj) {
        return obj == null ? NULL : new IntArrayKey(obj);
    }

    public static Object createKey(long[] obj) {
        return obj == null ? NULL : new LongArrayKey(obj);
    }

    public static Object createKey(short[] obj) {
        return obj == null ? NULL : new ShortArrayKey(obj);
    }

    public static Object createKey(Object[] obj) {
        return obj == null ? NULL : new ObjectArrayKey(obj);
    }

    public static Object createKey(Object obj) {
        if (obj == null) {
            return NULL;
        }
        if (!obj.getClass().isArray()) {
            return obj;
        }
        if (obj instanceof Object[]) {
            return createKey((Object[])obj);
        } else if (obj instanceof int[]) {
            return createKey((int[])obj);
        } else if (obj instanceof float[]) {
            return createKey((float[])obj);
        } else if (obj instanceof long[]) {
            return createKey((long[])obj);
        } else if (obj instanceof double[]) {
            return createKey((double[])obj);
        } else if (obj instanceof byte[]) {
            return createKey((byte[])obj);
        } else if (obj instanceof char[]) {
            return createKey((char[])obj);
        } else if (obj instanceof boolean[]) {
            return createKey((boolean[])obj);
        } else if (obj instanceof short[]) {
            return createKey((short[])obj);
        } else {
            return obj;
        }
    }

    static int booleanArrayHashCode(boolean[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + (a[i] ? 0 : 1);
        }
        return hash == 0 ? -1 : hash;
    }

    static int booleanArrayCompare(boolean[] a, boolean[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int av = a[i] ? 0 : 1;
            int bv = b[i] ? 0 : 1;
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int byteArrayHashCode(byte[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int byteArrayCompare(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            byte av = a[i];
            byte bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int charArrayHashCode(char[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int charArrayCompare(char[] a, char[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            char av = a[i];
            char bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int doubleArrayHashCode(double[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            long v = Double.doubleToLongBits(a[i]);
            hash = hash * 31 + (int)(v ^ v >>> 32);
        }
        return hash == 0 ? -1 : hash;
    }

    static int doubleArrayCompare(double[] a, double[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int v = Double.compare(a[i], b[i]);
            if (v != 0) {
                return v;
            }
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int floatArrayHashCode(float[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = hash * 31 + Float.floatToIntBits(a[i]);
        }
        return hash == 0 ? -1 : hash;
    }

    static int floatArrayCompare(float[] a, float[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int v = Float.compare(a[i], b[i]);
            if (v != 0) {
                return v;
            }
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int intArrayHashCode(int[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int intArrayCompare(int[] a, int[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int av = a[i];
            int bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int longArrayHashCode(long[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            long v = a[i];
            hash = hash * 31 + (int)(v ^ v >>> 32);
        }
        return hash == 0 ? -1 : hash;
    }

    static int longArrayCompare(long[] a, long[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            long av = a[i];
            long bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int shortArrayHashCode(short[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = (hash << 1) + a[i];
        }
        return hash == 0 ? -1 : hash;
    }

    static int shortArrayCompare(short[] a, short[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            short av = a[i];
            short bv = b[i];
            return av < bv ? -1 : (av > bv ? 1 : 0);
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    static int objectArrayHashCode(Object[] a) {
        int hash = 0;
        for (int i = a.length; --i >= 0; ) {
            hash = hash * 31 + objectHashCode(a[i]);
        }
        return hash == 0 ? -1 : hash;
    }

    // Compares object arrays and recurses into arrays within.
    static boolean objectArrayEquals(Object[] a, Object[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        int i;
        if ((i = a.length) != b.length) {
            return false;
        }
        while (--i >= 0) {
            if (!objectEquals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    // Compares object arrays and recurses into arrays within.
    static int objectArrayCompare(Object[] a, Object[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int length = Math.min(a.length, b.length);
        for (int i=0; i<length; i++) {
            int v = objectCompare(a[i], b[i]);
            if (v != 0) {
                return v;
            }
        }
        return a.length < b.length ? -1 : (a.length > b.length ? 1 : 0);
    }

    // Compute object or array hashcode and recurses into arrays within.
    static int objectHashCode(Object a) {
        if (a == null) {
            return -1;
        }
        if (!a.getClass().isArray()) {
            return a.hashCode();
        }
        if (a instanceof Object[]) {
            return objectArrayHashCode((Object[])a);
        } else if (a instanceof int[]) {
            return intArrayHashCode((int[])a);
        } else if (a instanceof float[]) {
            return floatArrayHashCode((float[])a);
        } else if (a instanceof long[]) {
            return longArrayHashCode((long[])a);
        } else if (a instanceof double[]) {
            return doubleArrayHashCode((double[])a);
        } else if (a instanceof byte[]) {
            return byteArrayHashCode((byte[])a);
        } else if (a instanceof char[]) {
            return charArrayHashCode((char[])a);
        } else if (a instanceof boolean[]) {
            return booleanArrayHashCode((boolean[])a);
        } else if (a instanceof short[]) {
            return shortArrayHashCode((short[])a);
        } else {
            int hash = a.getClass().hashCode();
            return hash == 0 ? -1 : hash;
        }
    }

    // Compares objects or arrays and recurses into arrays within.
    static boolean objectEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Class ac = a.getClass();
        if (!(ac.isArray())) {
            return a.equals(b);
        }
        if (ac != b.getClass()) {
            return false;
        }
        if (a instanceof Object[]) {
            return objectArrayEquals((Object[])a, (Object[])b);
        } else if (a instanceof int[]) {
            return Arrays.equals((int[])a, (int[])b);
        } else if (a instanceof float[]) {
            return Arrays.equals((float[])a, (float[])b);
        } else if (a instanceof long[]) {
            return Arrays.equals((long[])a, (long[])b);
        } else if (a instanceof double[]) {
            return Arrays.equals((double[])a, (double[])b);
        } else if (a instanceof byte[]) {
            return Arrays.equals((byte[])a, (byte[])b);
        } else if (a instanceof char[]) {
            return Arrays.equals((char[])a, (char[])b);
        } else if (a instanceof boolean[]) {
            return Arrays.equals((boolean[])a, (boolean[])b);
        } else if (a instanceof short[]) {
            return Arrays.equals((short[])a, (short[])b);
        } else {
            return a.equals(b);
        }
    }

    // Compares objects or arrays and recurses into arrays within.
    static int objectCompare(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        // TODO
        return 0;
    }

    protected KeyFactory() {
    }

    private static interface ArrayKey extends Comparable, java.io.Serializable {
        int hashCode();

        boolean equals(Object obj);

        int compareTo(Object obj);
    }

    private static class BooleanArrayKey implements ArrayKey {
        protected final boolean[] mArray;
        private transient int mHash;

        BooleanArrayKey(boolean[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = booleanArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof BooleanArrayKey ?
                 Arrays.equals(mArray, ((BooleanArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return booleanArrayCompare(mArray, ((BooleanArrayKey) obj).mArray);
        }
    }

    private static class ByteArrayKey implements ArrayKey {
        protected final byte[] mArray;
        private transient int mHash;

        ByteArrayKey(byte[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = byteArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof ByteArrayKey ?
                 Arrays.equals(mArray, ((ByteArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return byteArrayCompare(mArray, ((ByteArrayKey) obj).mArray);
        }
    }

    private static class CharArrayKey implements ArrayKey {
        protected final char[] mArray;
        private transient int mHash;

        CharArrayKey(char[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = charArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof CharArrayKey ?
                 Arrays.equals(mArray, ((CharArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return charArrayCompare(mArray, ((CharArrayKey) obj).mArray);
        }
    }

    private static class DoubleArrayKey implements ArrayKey {
        protected final double[] mArray;
        private transient int mHash;

        DoubleArrayKey(double[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = doubleArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof DoubleArrayKey ?
                 Arrays.equals(mArray, ((DoubleArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return doubleArrayCompare(mArray, ((DoubleArrayKey) obj).mArray);
        }
    }

    private static class FloatArrayKey implements ArrayKey {
        protected final float[] mArray;
        private transient int mHash;

        FloatArrayKey(float[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = floatArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof FloatArrayKey ?
                 Arrays.equals(mArray, ((FloatArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return floatArrayCompare(mArray, ((FloatArrayKey) obj).mArray);
        }
    }

    private static class IntArrayKey implements ArrayKey {
        protected final int[] mArray;
        private transient int mHash;

        IntArrayKey(int[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = intArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof IntArrayKey ?
                 Arrays.equals(mArray, ((IntArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return intArrayCompare(mArray, ((IntArrayKey) obj).mArray);
        }
    }

    private static class LongArrayKey implements ArrayKey {
        protected final long[] mArray;
        private transient int mHash;

        LongArrayKey(long[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = longArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof LongArrayKey ?
                 Arrays.equals(mArray, ((LongArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return longArrayCompare(mArray, ((LongArrayKey) obj).mArray);
        }
    }

    private static class ShortArrayKey implements ArrayKey {
        protected final short[] mArray;
        private transient int mHash;

        ShortArrayKey(short[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = shortArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof ShortArrayKey ?
                 Arrays.equals(mArray, ((ShortArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return shortArrayCompare(mArray, ((ShortArrayKey) obj).mArray);
        }
    }

    private static class ObjectArrayKey implements ArrayKey {
        protected final Object[] mArray;
        private transient int mHash;

        ObjectArrayKey(Object[] array) {
            mArray = array;
        }

        public int hashCode() {
            int hash = mHash;
            return hash == 0 ? mHash = objectArrayHashCode(mArray) : hash;
        }

        public boolean equals(Object obj) {
            return this == obj ? true :
                (obj instanceof ObjectArrayKey ?
                objectArrayEquals(mArray, ((ObjectArrayKey) obj).mArray) : false);
        }

        public int compareTo(Object obj) {
            return objectArrayCompare(mArray, ((ObjectArrayKey) obj).mArray);
        }
    }
}
