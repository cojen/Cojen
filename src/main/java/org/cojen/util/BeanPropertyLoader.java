package org.cojen.util;

import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.TypeDesc;

public abstract class BeanPropertyLoader {
    public abstract void loadLocal(CodeBuilder codeBuilder, LocalVariable beanVar);
    public abstract void loadClass(BeanProperty beanProperty);
    public abstract void invoke(CodeBuilder codeBuilder, BeanProperty beanProperty);
    public abstract void updateBuilder(CodeBuilder codeBuilder,BeanProperty beanProperty, TypeDesc type);
    public abstract void convert(CodeBuilder codeBuilder, TypeDesc type);
}
