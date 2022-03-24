package org.cojen.util;

import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.TypeDesc;

public class WriteMethod extends BeanPropertyLoader {

    @Override
    public void convert(CodeBuilder codeBuilder, TypeDesc type) {
        codeBuilder.checkCast(type.toObjectType());
        codeBuilder.convert(type.toObjectType(), type);
    }

    @Override
    public void loadLocal(CodeBuilder codeBuilder, LocalVariable beanVar) {
        codeBuilder.loadLocal(beanVar); // common

    }

    @Override
    public void loadClass(BeanProperty beanProperty) {
        TypeDesc type = TypeDesc.forClass(beanProperty.getType()); //common

    }

    @Override
    public void invoke(CodeBuilder codeBuilder, BeanProperty beanProperty) {
        codeBuilder.invoke(beanProperty.getWriteMethod()); //common

    }

    @Override
    public void updateBuilder(CodeBuilder codeBuilder, BeanProperty beanProperty, TypeDesc type) {
        loadClass(beanProperty);
        convert(codeBuilder, type);
        invoke(codeBuilder, beanProperty);

    }
}
