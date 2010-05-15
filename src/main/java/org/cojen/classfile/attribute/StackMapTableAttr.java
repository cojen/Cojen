/*
 *  Copyright 2008-2010 Brian S O'Neill
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

package org.cojen.classfile.attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.TypeDesc;

import org.cojen.classfile.constant.ConstantClassInfo;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class StackMapTableAttr extends Attribute {
    private final InitialFrame mInitialFrame;
    private int mSize;
    private int mLength;

    public StackMapTableAttr(ConstantPool cp) {
        super(cp, STACK_MAP_TABLE);
        mInitialFrame = new InitialFrame();
    }

    public StackMapTableAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);

        int size = din.readUnsignedShort();

        InitialFrame first = new InitialFrame();
        StackMapFrame last = first;
        for (int i=0; i<size; i++) {
            StackMapFrame frame = StackMapFrame.read(last, cp, din);
            if (frame != null) {
                last = frame;
            }
        }

        mInitialFrame = first;
        mSize = size;
        mLength = length;
    }

    public int getLength() {
        if (mLength < 0) {
            if (mInitialFrame.getNext() == null) {
                mLength = 0;
            } else {
                int length = 2;
                StackMapFrame frame = mInitialFrame;
                while (frame != null) {
                    length += frame.getLength();
                    frame = frame.getNext();
                }
                mLength = length;
            }
        }
        return mLength;
    }

    @Override
    public void writeTo(DataOutput dout) throws IOException {
        if (mSize == 0) {
            return;
        }
        super.writeTo(dout);
    }

    @Override
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mSize);
        StackMapFrame frame = mInitialFrame;
        while (frame != null) {
            frame.writeTo(dout);
            frame = frame.getNext();
        }
    }

    public StackMapFrame getInitialFrame() {
        return mInitialFrame;
    }

    public void setInitialFrame(MethodInfo method) {
        mInitialFrame.set(getConstantPool(), method);
    }

    public static abstract class StackMapFrame {
        static StackMapFrame read(StackMapFrame prev, ConstantPool cp, DataInput din)
            throws IOException
        {
            int frameType = din.readUnsignedByte();

            if (frameType <= 63) {
                return new SameFrame(prev, frameType);
            } else if (frameType <= 127) {
                return new SameLocalsOneStackItemFrame(prev, frameType - 64, cp, din);
            } else if (frameType <= 246) {
                // reserved frame type range
                return null;
            } else if (frameType == 247) {
                return new SameLocalsOneStackItemFrameExtended(prev, cp, din);
            } else if (frameType <= 250) {
                return new ChopFrame(prev, 251 - frameType, din);
            } else if (frameType == 251) {
                return new SameFrameExtended(prev, din);
            } else if (frameType <= 254) {
                return new AppendFrame(prev, frameType - 251, cp, din);
            } else {
                return new FullFrame(prev, cp, din);
            }
        }

        private final StackMapFrame mPrev;

        StackMapFrame mNext;
        int mOffset = -1;

        StackMapFrame(StackMapFrame prev) {
            if ((mPrev = prev) != null) {
                prev.mNext = this;
            }
        }

        /**
         * Returns number of bytes required to encode frame in class file.
         */
        abstract int getLength();

        /**
         * Returns the instruction offset for which this frame applies to.
         */
        public final int getOffset() {
            if (mOffset < 0) {
                if (mPrev == null || mPrev instanceof InitialFrame) {
                    mOffset = getOffsetDelta();
                } else {
                    mOffset = mPrev.getOffset() + 1 + getOffsetDelta();
                }
            }
            return mOffset;
        }

        abstract int getOffsetDelta();

        /**
         * Returns verification info for all local variables at this frame.
         */
        public abstract VerificationTypeInfo[] getLocalInfos();

        /**
         * Returns verification info for all stack variables at this
         * frame. Element 0 is the bottom of the stack.
         */
        public abstract VerificationTypeInfo[] getStackItemInfos();

        public final StackMapFrame getPrevious() {
            return mPrev;
        }

        public final StackMapFrame getNext() {
            return mNext;
        }

        public abstract void writeTo(DataOutput dout) throws IOException;
    }

    private static class InitialFrame extends StackMapFrame {
        private VerificationTypeInfo[] mLocalInfos;

        InitialFrame() {
            super(null);
            mOffset = 0;
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public int getOffsetDelta() {
            return 0;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            if (mLocalInfos == null) {
                return VerificationTypeInfo.EMPTY_ARRAY;
            }
            return mLocalInfos.clone();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        @Override
        public void writeTo(DataOutput dout) {
        }

        void set(ConstantPool cp, MethodInfo info) {
            TypeDesc[] paramTypes = info.getMethodDescriptor().getParameterTypes();

            VerificationTypeInfo[] infos;
            int offset;

            if (info.getModifiers().isStatic()) {
                infos = new VerificationTypeInfo[paramTypes.length];
                offset = 0;
            } else {
                infos = new VerificationTypeInfo[1 + paramTypes.length];
                if (info.getName().equals("<init>")) {
                    infos[0] = UninitThisVariableInfo.THE;
                } else {
                    infos[0] = VerificationTypeInfo.forType(cp, info.getClassFile().getType());
                }
                offset = 1;
            }

            for (int i=0; i<paramTypes.length; i++) {
                infos[offset + i] = VerificationTypeInfo.forType(cp, paramTypes[i]);
            }

            mLocalInfos = infos;
        }
    }

    private static class SameFrame extends StackMapFrame {
        private final int mOffsetDelta;

        SameFrame(StackMapFrame prev, int offsetDelta) {
            super(prev);
            mOffsetDelta = offsetDelta;
        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(mOffsetDelta);
        }
    }

    private static class SameLocalsOneStackItemFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo mStackItemInfo;

        SameLocalsOneStackItemFrame(StackMapFrame prev,
                                    int offsetDelta, ConstantPool cp, DataInput din)
            throws IOException
        {
            super(prev);
            mOffsetDelta = offsetDelta;
            mStackItemInfo = VerificationTypeInfo.read(cp, din);
        }

        @Override
        public int getLength() {
            return 1 + mStackItemInfo.getLength();
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return new VerificationTypeInfo[]{mStackItemInfo};
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(64 + mOffsetDelta);
            mStackItemInfo.writeTo(dout);
        }
    }

    private static class SameLocalsOneStackItemFrameExtended extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo mStackItemInfo;

        SameLocalsOneStackItemFrameExtended(StackMapFrame prev, ConstantPool cp, DataInput din)
            throws IOException
        {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            mStackItemInfo = VerificationTypeInfo.read(cp, din);
        }

        @Override
        public int getLength() {
            return 3 + mStackItemInfo.getLength();
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return new VerificationTypeInfo[]{mStackItemInfo};
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(257);
            dout.writeShort(mOffsetDelta);
            mStackItemInfo.writeTo(dout);
        }
    }

    private static class ChopFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final int mChop;

        private transient VerificationTypeInfo[] mLocalInfos;

        ChopFrame(StackMapFrame prev, int chop, DataInput din) throws IOException {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            mChop = chop;
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            if (mLocalInfos == null) {
                VerificationTypeInfo[] prevInfos = getPrevious().getLocalInfos();
                VerificationTypeInfo[] infos = new VerificationTypeInfo[prevInfos.length - mChop];
                System.arraycopy(prevInfos, 0, infos, 0, infos.length);
                mLocalInfos = infos;
            }
            return mLocalInfos.clone();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(251 - mChop);
            dout.writeShort(mOffsetDelta);
        }
    }

    private static class SameFrameExtended extends StackMapFrame {
        private final int mOffsetDelta;

        SameFrameExtended(StackMapFrame prev, DataInput din) throws IOException {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(251);
            dout.writeShort(mOffsetDelta);
        }
    }

    private static class AppendFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo[] mAppendInfos;

        private transient VerificationTypeInfo[] mLocalInfos;

        AppendFrame(StackMapFrame prev, int numLocals, ConstantPool cp, DataInput din)
            throws IOException
        {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            mAppendInfos = VerificationTypeInfo.read(cp, din, numLocals);
        }

        @Override
        public int getLength() {
            int length = 3;
            for (VerificationTypeInfo info : mAppendInfos) {
                length += info.getLength();
            }
            return length;
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            if (mLocalInfos == null) {
                VerificationTypeInfo[] prevInfos = getPrevious().getLocalInfos();
                VerificationTypeInfo[] infos =
                    new VerificationTypeInfo[prevInfos.length + mAppendInfos.length];
                System.arraycopy(prevInfos, 0, infos, 0, prevInfos.length);
                System.arraycopy(mAppendInfos, 0, infos, prevInfos.length, mAppendInfos.length);
                mLocalInfos = infos;
            }
            return mLocalInfos.clone();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(251 + mAppendInfos.length);
            dout.writeShort(mOffsetDelta);
            for (VerificationTypeInfo info : mAppendInfos) {
                info.writeTo(dout);
            }
        }
    }

    private static class FullFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo[] mLocalInfos;
        private final VerificationTypeInfo[] mStackItemInfos;

        FullFrame(StackMapFrame prev, ConstantPool cp, DataInput din) throws IOException {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            int numLocals = din.readUnsignedShort();
            mLocalInfos = VerificationTypeInfo.read(cp, din, numLocals);
            int numStackItems = din.readUnsignedShort();
            mStackItemInfos = VerificationTypeInfo.read(cp, din, numStackItems);
        }

        @Override
        public int getLength() {
            int length = 7;
            for (VerificationTypeInfo info : mLocalInfos) {
                length += info.getLength();
            }
            for (VerificationTypeInfo info : mStackItemInfos) {
                length += info.getLength();
            }
            return length;
        }

        @Override
        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        @Override
        public VerificationTypeInfo[] getLocalInfos() {
            return mLocalInfos.clone();
        }

        @Override
        public VerificationTypeInfo[] getStackItemInfos() {
            return mStackItemInfos.clone();
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(255);
            dout.writeShort(mOffsetDelta);
            dout.writeShort(mLocalInfos.length);
            for (VerificationTypeInfo info : mLocalInfos) {
                info.writeTo(dout);
            }
            dout.writeShort(mStackItemInfos.length);
            for (VerificationTypeInfo info : mStackItemInfos) {
                info.writeTo(dout);
            }
        }
    }

    public static abstract class VerificationTypeInfo {
        static final VerificationTypeInfo[] EMPTY_ARRAY = new VerificationTypeInfo[0];

        static VerificationTypeInfo read(ConstantPool cp, DataInput din)
            throws IOException
        {
            int type = din.readUnsignedByte();
            switch (type) {
            case 0:
                return TopVariableInfo.THE;
            case 1:
                return IntegerVariableInfo.THE;
            case 2:
                return FloatVariableInfo.THE;
            case 3:
                return DoubleVariableInfo.THE;
            case 4:
                return LongVariableInfo.THE;
            case 5:
                return NullVariableInfo.THE;
            case 6:
                return UninitThisVariableInfo.THE;
            case 7:
                return new ObjectVariableInfo(cp, din);
            case 8:
                return new UninitVariableInfo(cp, din);
            }
            return null;
        }

        static VerificationTypeInfo forType(ConstantPool cp, TypeDesc type) {
            switch (type.getTypeCode()) {
            default:
                return TopVariableInfo.THE;
            case TypeDesc.OBJECT_CODE:
                return new ObjectVariableInfo(cp, type);
            case TypeDesc.BOOLEAN_CODE:
            case TypeDesc.BYTE_CODE:
            case TypeDesc.CHAR_CODE:
            case TypeDesc.SHORT_CODE:
            case TypeDesc.INT_CODE:
                return IntegerVariableInfo.THE;
            case TypeDesc.LONG_CODE:
                return LongVariableInfo.THE;
            case TypeDesc.FLOAT_CODE:
                return FloatVariableInfo.THE;
            case TypeDesc.DOUBLE_CODE:
                return DoubleVariableInfo.THE;
            }
        }

        private static VerificationTypeInfo[] read(ConstantPool cp, DataInput din, int num)
            throws IOException
        {
            VerificationTypeInfo[] infos = new VerificationTypeInfo[num];
            for (int i=0; i<num; i++) {
                infos[i] = read(cp, din);
            }
            return infos;
        }

        VerificationTypeInfo() {
        }

        /**
         * Returns number of bytes required to encode info in class file.
         */
        int getLength() {
            return 1;
        }

        /**
         * Returns variable type, which is null if unknown or not applicable.
         */
        public abstract TypeDesc getType();

        /**
         * When true, type is unassigned or unused.
         */
        public boolean isTop() {
            return false;
        }

        /**
         * When true, type is the "this" reference.
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

        public abstract void writeTo(DataOutput dout) throws IOException;

        @Override
        public String toString() {
            return getType().getFullName();
        }
    }

    private static class TopVariableInfo extends VerificationTypeInfo {
        static final TopVariableInfo THE = new TopVariableInfo();

        private TopVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return null;
        }

        @Override
        public boolean isTop() {
            return true;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(0);
        }

        @Override
        public String toString() {
            return "<top>";
        }
    }

    private static class IntegerVariableInfo extends VerificationTypeInfo {
        static final IntegerVariableInfo THE = new IntegerVariableInfo();

        private IntegerVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return TypeDesc.INT;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(1);
        }
    }

    private static class FloatVariableInfo extends VerificationTypeInfo {
        static final FloatVariableInfo THE = new FloatVariableInfo();

        private FloatVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return TypeDesc.FLOAT;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(2);
        }
    }

    private static class LongVariableInfo extends VerificationTypeInfo {
        static final LongVariableInfo THE = new LongVariableInfo();

        private LongVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return TypeDesc.LONG;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(4);
        }
    }

    private static class DoubleVariableInfo extends VerificationTypeInfo {
        static final DoubleVariableInfo THE = new DoubleVariableInfo();

        private DoubleVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return TypeDesc.DOUBLE;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(3);
        }
    }

    private static class NullVariableInfo extends VerificationTypeInfo {
        static final NullVariableInfo THE = new NullVariableInfo();

        private NullVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return null;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(5);
        }

        @Override
        public String toString() {
            return "<null>";
        }
    }

    private static class UninitThisVariableInfo extends VerificationTypeInfo {
        static final UninitThisVariableInfo THE = new UninitThisVariableInfo();

        private UninitThisVariableInfo() {
        }

        @Override
        public TypeDesc getType() {
            return null;
        }

        @Override
        public boolean isThis() {
            return true;
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(6);
        }

        @Override
        public String toString() {
            return "<uninitialized this>";
        }
    }

    private static class ObjectVariableInfo extends VerificationTypeInfo {
        private final ConstantClassInfo mClassInfo;

        ObjectVariableInfo(ConstantPool cp, DataInput din) throws IOException {
            mClassInfo = (ConstantClassInfo) cp.getConstant(din.readUnsignedShort());
        }

        ObjectVariableInfo(ConstantPool cp, TypeDesc desc) {
            mClassInfo = cp.addConstantClass(desc);
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public TypeDesc getType() {
            return mClassInfo.getType();
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(7);
            dout.writeShort(mClassInfo.getIndex());
        }
    }

    private static class UninitVariableInfo extends VerificationTypeInfo {
        private final int mOffset;

        UninitVariableInfo(ConstantPool cp, DataInput din) throws IOException {
            mOffset = din.readUnsignedShort();
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public TypeDesc getType() {
            return null;
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        @Override
        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(8);
            dout.writeShort(mOffset);
        }

        @Override
        public String toString() {
            return "<uninitialized>";
        }
    }
}
