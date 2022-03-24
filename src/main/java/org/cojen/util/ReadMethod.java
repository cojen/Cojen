package org.cojen.util;

import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.TypeDesc;

public class ReadMethod extends BeanPropertyLoader {

    @Override
    public void convert(CodeBuilder codeBuilder, TypeDesc type) {
        codeBuilder.convert(type, type.toObjectType());
        codeBuilder.returnValue(TypeDesc.OBJECT);
    }

    @Override
    public void loadLocal(CodeBuilder codeBuilder, LocalVariable beanVar) {
        codeBuilder.loadLocal(beanVar);
    }

    @Override
    public void loadClass(BeanProperty beanProperty) {
        TypeDesc type = TypeDesc.forClass(beanProperty.getType());
    }

    @Override
    public void invoke(CodeBuilder codeBuilder, BeanProperty beanProperty) {
        codeBuilder.invoke(beanProperty.getReadMethod());
    }

    @Override
    public void updateBuilder(CodeBuilder codeBuilder, BeanProperty beanProperty, TypeDesc type) {
        invoke(codeBuilder, beanProperty);
        loadClass(beanProperty);
        convert(codeBuilder, type);
    }

}
