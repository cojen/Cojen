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

package org.cojen.classfile;

import java.util.List;

/**
 * Describes runtime value types on the operand stack and in local variables.
 * VerificationInfo instances are created only at branch target locations.
 *
 * @author Brian S O'Neill
 */
public class VerificationInfo implements Comparable<VerificationInfo> {
    private final Location mLocation;
    private final List<Type> mStackTypes;
    private final List<Type> mLocalTypes;

    public VerificationInfo(Location location, List<Type> stackTypes, List<Type> localTypes) {
        mLocation = location;
        mStackTypes = stackTypes;
        mLocalTypes = localTypes;
    }

    /**
     * Returns the branch target location for which this VerificationInfo applies.
     */
    public Location getLocation() {
        return mLocation;
    }

    /**
     * Returns value types for the operand stack, where array index zero
     * represents the bottom of the stack.
     */
    public List<Type> getOperandStackTypes() {
        return mStackTypes;
    }

    /**
     * Returns value types for local variables.
     */
    public List<Type> getLocalVariableTypes() {
        return mLocalTypes;
    }

    /**
     * Compares based on location.
     */
    public int compareTo(VerificationInfo other) {
        return mLocation.compareTo(other.mLocation);
    }

    @Override
    public String toString() {
        return "VerificationInfo: {location=" + getLocation().getLocation() +
            ", operandStackTypes=" + getOperandStackTypes() +
            ", localVariableTypes=" + getLocalVariableTypes() + '}';
    }

    public static Type toType(TypeDesc type) {
        return (type == null || type == TypeDesc.VOID) ? null : new Normal(type);
    }

    public static Type nullType() {
        return Null.THE;
    }

    public static Type topType() {
        return Top.THE;
    }

    public static Type uninitializedThisType() {
        return UninitThis.THE;
    }

    /**
     * @param newLocation location of instruction which created object
     */
    public static Type uninitializedType(Location newLocation) {
        return newLocation == null ? Uninit.NULL_LOCATION : new Uninit(newLocation);
    }

    public static abstract class Type {
        /**
         * @return null if uninitialized or not applicable
         */
        public TypeDesc getType() {
            return null;
        }

        public boolean isDoubleWord() {
            return false;
        }

        public abstract boolean isReference();

        /*
         * When true, type is second part of double word or is unused.
         */
        public boolean isTop() {
            return false;
        }

        /**
         * When true, type is an uninitialized "this" reference.
         */
        public boolean isThis() {
            return false;
        }

        /**
         * When true, type is an object whose constructor has not been called.
         */
        public boolean isUninitialized() {
            return false;
        }

        /**
         * If type is uninitialized and not "this", return the location of the
         * new instruction that created it. Otherwise, return null.
         */
        public Location getNewLocation() {
            return null;
        }

        /**
         * When true, type is null.
         */
        public boolean isNull() {
            return false;
        }
    }

    private static class Normal extends Type {
        private final TypeDesc mType;

        Normal(TypeDesc type) {
            switch (type.getTypeCode()) {
            case TypeDesc.BOOLEAN_CODE:
            case TypeDesc.BYTE_CODE:
            case TypeDesc.SHORT_CODE:
            case TypeDesc.CHAR_CODE:
                type = TypeDesc.INT;
            }
            mType = type;
        }

        @Override
        public TypeDesc getType() {
            return mType;
        }

        @Override
        public boolean isDoubleWord() {
            return mType.isDoubleWord();
        }

        @Override
        public boolean isReference() {
            return !mType.isPrimitive();
        }

        @Override
        public String toString() {
            return mType.toString();
        }

        @Override
        public int hashCode() {
            return mType.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Normal) {
                Normal other = (Normal) obj;
                return mType.equals(other.mType);
            }
            return false;
        }
    }

    private static class Top extends Type {
        static final Top THE = new Top();

        private Top() {
        }

        @Override
        public boolean isTop() {
            return true;
        }

        @Override
        public boolean isReference() {
            return false;
        }

        @Override
        public String toString() {
            return "<top>";
        }
    }

    private static class Null extends Type {
        static final Null THE = new Null();

        private Null() {
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public String toString() {
            return "<null>";
        }
    }

    private static class UninitThis extends Type {
        static final UninitThis THE = new UninitThis();

        private UninitThis() {
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        @Override
        public boolean isThis() {
            return true;
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public String toString() {
            return "<uninitialized this>";
        }
    }

    private static class Uninit extends Type {
        static final Uninit NULL_LOCATION = new Uninit(null);

        private final Location mNewLocation;

        Uninit(Location newLocation) {
            mNewLocation = newLocation;
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        @Override
        public Location getNewLocation() {
            return mNewLocation;
        }

        @Override
        public boolean isReference() {
            return true;
        }

        public String toString() {
            return "<uninitialized>";
        }
    }
}
