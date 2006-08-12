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

import cojen.classfile.*;

/**
 * 
 * @author Brian S O'Neill
 */
public class Test2 {
    public static void main(String[] args) throws Exception {
        boolean x = ((float)args.length) != 0;

        TypeDesc td = TypeDesc.forClass(args[0]);
        System.out.println(td);
        System.out.println(td.getDescriptor());
        System.out.println(td.getRootName());
        System.out.println(td.getFullName());
        System.out.println(td.getTypeCode());
        System.out.println(td.isPrimitive());
        System.out.println(td.isDoubleWord());
        System.out.println(td.isArray());
        System.out.println(td.getDimensions());
        System.out.println(td.getComponentType());
        System.out.println(td.toArrayType());
        System.out.println(td.toObjectType());
        System.out.println(td.toPrimitiveType());
        System.out.println(td.toClass());
        System.out.println(td.toClass(Test2.class.getClassLoader()));
        System.out.println(td == TypeDesc.INT);
        System.out.println(td == TypeDesc.forClass(Test2.class));

        System.out.println("--------------");

        TypeDesc[] params = new TypeDesc[args.length - 1];
        for (int i=1; i<args.length; i++) {
            params[i - 1] = TypeDesc.forClass(args[i]);
        }
        MethodDesc md = MethodDesc.forArguments(td, params);
        System.out.println(md);
        System.out.println(md.getDescriptor());
        System.out.println(md.toMethodSignature("foo"));
        System.out.println(md.getReturnType());
        System.out.println(md.getParameterCount());
        params = md.getParameterTypes();
        for (int i=0; i<params.length; i++) {
            System.out.println(params[i]);
        }

        System.out.println("--------------");

        MethodDesc md2 = MethodDesc.forDescriptor("(Ljava/lang/String;[ID[[LVector;)Lcojen/test/Test2;");
        System.out.println(md == md2);
        System.out.println(md2);
        System.out.println(md2.getDescriptor());
        System.out.println(md2.toMethodSignature("foo"));
        System.out.println(md2.getReturnType());
        System.out.println(md2.getParameterCount());
        params = md2.getParameterTypes();
        for (int i=0; i<params.length; i++) {
            System.out.println(params[i]);
        }
    }
}
