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

import java.util.ArrayList;
import java.util.List;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import cojen.classfile.attr.CodeAttr;
import cojen.classfile.attr.DeprecatedAttr;
import cojen.classfile.attr.ExceptionsAttr;
import cojen.classfile.attr.SignatureAttr;
import cojen.classfile.attr.SourceFileAttr;
import cojen.classfile.attr.SyntheticAttr;

/**
 * This class corresponds to the method_info data structure as defined in
 * section 4.6 of <i>The Java Virtual Machine Specification</i>. 
 * To make it easier to create bytecode for a method's CodeAttr, the 
 * CodeBuilder class is provided.
 * 
 * @author Brian S O'Neill
 * @see ClassFile
 * @see CodeBuilder
 */
public class MethodInfo {
    private ClassFile mParent;
    private ConstantPool mCp;

    private String mName;
    private MethodDesc mDesc;
    
    private int mModifier;

    private ConstantUTFInfo mNameConstant;
    private ConstantUTFInfo mDescriptorConstant;
    
    private List mAttributes = new ArrayList(2);

    private CodeAttr mCode;
    private ExceptionsAttr mExceptions;

    MethodInfo(ClassFile parent,
               Modifiers modifiers,
               String name,
               MethodDesc desc) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = name;
        mDesc = desc;
        
        mModifier = modifiers.getModifier();
        mNameConstant = ConstantUTFInfo.make(mCp, name);
        mDescriptorConstant = ConstantUTFInfo.make(mCp, desc.toString());

        if (!modifiers.isAbstract() && !modifiers.isNative()) {
            addAttribute(new CodeAttr(mCp));
        }
    }

    private MethodInfo(ClassFile parent,
                       int modifiers,
                       ConstantUTFInfo nameConstant,
                       ConstantUTFInfo descConstant) {

        mParent = parent;
        mCp = parent.getConstantPool();
        mName = nameConstant.getValue();
        mDesc = MethodDesc.forDescriptor(descConstant.getValue());

        mModifier = modifiers;
        mNameConstant = nameConstant;
        mDescriptorConstant = descConstant;
    }
    
    /**
     * Returns the parent ClassFile for this MethodInfo.
     */
    public ClassFile getClassFile() {
        return mParent;
    }

    /**
     * Returns the name of this method.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns a MethodDesc which describes return and parameter types
     * of this method.
     */
    public MethodDesc getMethodDescriptor() {
        return mDesc;
    }

    /**
     * Returns a copy of this method's modifiers.
     */
    public Modifiers getModifiers() {
        return new Modifiers(mModifier);
    }
    
    /**
     * Returns a constant from the constant pool with this method's name.
     */
    public ConstantUTFInfo getNameConstant() {
        return mNameConstant;
    }
    
    /**
     * Returns a constant from the constant pool with this method's type 
     * descriptor string.
     * @see MethodDesc
     */
    public ConstantUTFInfo getDescriptorConstant() {
        return mDescriptorConstant;
    }

    /**
     * Returns the exceptions that this method is declared to throw.
     */
    public String[] getExceptions() {
        if (mExceptions == null) {
            return new String[0];
        } else {
            return mExceptions.getExceptions();
        }
    }

    /**
     * Returns a CodeAttr object used to manipulate the method code body, or
     * null if this method is abstract or native.
     */
    public CodeAttr getCodeAttr() {
        return mCode;
    }

    public boolean isSynthetic() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SyntheticAttr) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeprecated() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof DeprecatedAttr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the signature attribute of this method, or null if none is
     * defined.
     */
    // TODO: Eventually remove this method
    public SignatureAttr getSignatureAttr() {
        for (int i = mAttributes.size(); --i >= 0; ) {
            Object obj = mAttributes.get(i);
            if (obj instanceof SignatureAttr) {
                return (SignatureAttr) obj;
            }
        }
        return null;
    }
    
    /** 
     * Add a declared exception that this method may throw.
     */
    public void addException(String className) {
        if (mExceptions == null) {
            addAttribute(new ExceptionsAttr(mCp));
        }

        ConstantClassInfo cci = ConstantClassInfo.make(mCp, className);
        mExceptions.addException(cci);
    }

    /**
     * Mark this method as being synthetic by adding a special attribute.
     */
    public void markSynthetic() {
        addAttribute(new SyntheticAttr(mCp));
    }

    /**
     * Mark this method as being deprecated by adding a special attribute.
     */
    public void markDeprecated() {
        addAttribute(new DeprecatedAttr(mCp));
    }

    public void addAttribute(Attribute attr) {
        if (attr instanceof CodeAttr) {
            if (mCode != null) {
                mAttributes.remove(mCode);
            }
            mCode = (CodeAttr)attr;
        } else if (attr instanceof ExceptionsAttr) {
            if (mExceptions != null) {
                mAttributes.remove(mExceptions);
            }
            mExceptions = (ExceptionsAttr)attr;
        }

        mAttributes.add(attr);
    }

    public Attribute[] getAttributes() {
        Attribute[] attrs = new Attribute[mAttributes.size()];
        return (Attribute[])mAttributes.toArray(attrs);
    }

    /**
     * Returns the length (in bytes) of this object in the class file.
     */
    public int getLength() {
        int length = 8;
        
        int size = mAttributes.size();
        for (int i=0; i<size; i++) {
            length += ((Attribute)mAttributes.get(i)).getLength();
        }
        
        return length;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        dout.writeShort(mModifier);
        dout.writeShort(mNameConstant.getIndex());
        dout.writeShort(mDescriptorConstant.getIndex());
        
        int size = mAttributes.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Attribute attr = (Attribute)mAttributes.get(i);
            try {
                attr.writeTo(dout);
            } catch (IllegalStateException e) {
                IllegalStateException e2 = 
                    new IllegalStateException(e.getMessage() + ": " + toString());
                try {
                    e2.initCause(e);
                } catch (NoSuchMethodError e3) {
                }
                throw e2;
            }
        }
    }

    public String toString() {
        String str = mDesc.toMethodSignature
            (getName(), Modifiers.isVarArgs(mModifier));
        String modStr = Modifier.toString(mModifier);
        if (modStr.length() > 0) {
            str = modStr + ' ' + str;
        }
        return str;
    }

    static MethodInfo readFrom(ClassFile parent, 
                               DataInput din,
                               AttributeFactory attrFactory)
        throws IOException
    {
        ConstantPool cp = parent.getConstantPool();

        int modifier = din.readUnsignedShort();
        int index = din.readUnsignedShort();
        ConstantUTFInfo nameConstant = (ConstantUTFInfo)cp.getConstant(index);
        index = din.readUnsignedShort();
        ConstantUTFInfo descConstant = (ConstantUTFInfo)cp.getConstant(index);

        MethodInfo info = new MethodInfo(parent, modifier,
                                         nameConstant, descConstant);

        // Read attributes.
        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            info.addAttribute(Attribute.readFrom(cp, din, attrFactory));
        }

        return info;
    }
}