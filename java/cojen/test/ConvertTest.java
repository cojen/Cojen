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

package cojen.test;

import java.io.*;
import cojen.classfile.*;

/**
 * 
 * @author Brian S O'Neill
 */
public class ConvertTest {
    public static void main(String[] args) throws Exception {
        ClassFile cf = new ClassFile("Foo");
        cf.setTarget("1.5");

        cf.addDefaultConstructor();

        Modifiers modifiers = new Modifiers();
        modifiers.setPublic(true);
        modifiers.setStatic(true);
        TypeDesc[] params = {TypeDesc.STRING.toArrayType()};
        MethodInfo mi = cf.addMethod(modifiers, "main", null, params);
        CodeBuilder builder = new CodeBuilder(mi);

        builder.mapLineNumber(1);
        preconvert(builder);
        builder.loadConstant(12345678);
        convert(builder, TypeDesc.INT, TypeDesc.DOUBLE);

        builder.mapLineNumber(2);
        preconvert(builder);
        builder.loadConstant(12345678);
        convert(builder, TypeDesc.INT, TypeDesc.DOUBLE.toObjectType());

        builder.mapLineNumber(3);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.DOUBLE.toObjectType());

        builder.mapLineNumber(4);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.LONG);

        builder.mapLineNumber(5);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.LONG.toObjectType());

        builder.mapLineNumber(6);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.INT);

        builder.mapLineNumber(7);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.BYTE.toObjectType());

        builder.mapLineNumber(8);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.BOOLEAN);

        builder.mapLineNumber(9);
        preconvert(builder);
        builder.loadConstant(12345678.0d);
        convert(builder, TypeDesc.DOUBLE, TypeDesc.BOOLEAN.toObjectType());

        builder.mapLineNumber(10);
        preconvert(builder);
        builder.loadConstant(Float.NaN);
        convert(builder, TypeDesc.FLOAT, TypeDesc.BOOLEAN);

        builder.mapLineNumber(11);
        preconvert(builder);
        builder.loadConstant(Float.NaN);
        convert(builder, TypeDesc.FLOAT, TypeDesc.BOOLEAN.toObjectType());

        builder.mapLineNumber(12);
        preconvert(builder);
        builder.loadConstant(0.0f);
        convert(builder, TypeDesc.FLOAT, TypeDesc.BOOLEAN.toObjectType());

        builder.mapLineNumber(13);
        preconvert(builder);
        builder.loadConstant(true);
        builder.convert(TypeDesc.BOOLEAN, TypeDesc.BOOLEAN.toObjectType());
        convert(builder, TypeDesc.BOOLEAN.toObjectType(), TypeDesc.DOUBLE);

        builder.mapLineNumber(14);
        preconvert(builder);
        builder.loadConstant(true);
        builder.convert(TypeDesc.BOOLEAN, TypeDesc.BOOLEAN.toObjectType());
        convert(builder, TypeDesc.BOOLEAN.toObjectType(),
                TypeDesc.DOUBLE.toObjectType());

        builder.mapLineNumber(15);
        preconvert(builder);
        builder.loadConstant(true);
        builder.convert(TypeDesc.BOOLEAN, TypeDesc.BOOLEAN.toObjectType());
        convert(builder, TypeDesc.BOOLEAN.toObjectType(),
                TypeDesc.SHORT.toObjectType());

        builder.mapLineNumber(16);
        preconvert(builder);
        builder.loadConstant(false);
        builder.convert(TypeDesc.BOOLEAN, TypeDesc.BOOLEAN.toObjectType());
        convert(builder, TypeDesc.BOOLEAN.toObjectType(),
                TypeDesc.LONG.toObjectType());

        builder.mapLineNumber(17);
        preconvert(builder);
        builder.loadConstant(56789);
        builder.convert(TypeDesc.INT, TypeDesc.INT.toObjectType());
        convert(builder, TypeDesc.INT.toObjectType(),
                TypeDesc.LONG.toObjectType());

        builder.mapLineNumber(18);
        preconvert(builder);
        builder.loadConstant(56789);
        builder.convert(TypeDesc.INT, TypeDesc.INT.toObjectType());
        convert(builder, TypeDesc.INT.toObjectType(), TypeDesc.BOOLEAN);

        builder.mapLineNumber(19);
        preconvert(builder);
        builder.loadConstant(65);
        builder.convert(TypeDesc.INT, TypeDesc.INT.toObjectType());
        convert(builder, TypeDesc.INT.toObjectType(),
                TypeDesc.CHAR.toObjectType());

        builder.mapLineNumber(20);
        preconvert(builder);
        builder.loadConstant(65);
        builder.convert(TypeDesc.INT, TypeDesc.CHAR);
        builder.mapLineNumber(21);
        convert(builder, TypeDesc.CHAR, TypeDesc.OBJECT);

        builder.returnVoid();

        OutputStream out = new FileOutputStream("Foo.class");
        cf.writeTo(out);
        out.close();
    }

    private static void preconvert(CodeBuilder builder) {
        TypeDesc stream = TypeDesc.forClass("java.io.PrintStream");
        builder.loadStaticField("java.lang.System", "out", stream);
    }

    private static void convert(CodeBuilder builder,
                                TypeDesc from, TypeDesc to) {
        builder.convert(from, to);
        if (!to.isPrimitive()) {
            to = TypeDesc.OBJECT;
        }
        builder.invokeVirtual("java.io.PrintStream", "println",
                              null, new TypeDesc[]{to});
    }
}
