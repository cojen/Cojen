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
import cojen.util.BeanPropertyAccessor;
import cojen.util.ClassInjector;
import java.io.*;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestClassGC {
    public static void main(String[] args) throws Exception {
        for (int i=0; i<1000000; i++) {
            String name = "Foo_" + i;

            ClassInjector ci = ClassInjector.create(name, null);
            ClassFile cf = new ClassFile(ci.getClassName());

            cf.addDefaultConstructor();
            
            MethodInfo mi = cf.addMethod(Modifiers.PUBLIC, "getStuff", TypeDesc.STRING, null);
            CodeBuilder b = new CodeBuilder(mi);
            b.loadConstant("Stuff!!!");
            b.returnValue(TypeDesc.STRING);

            Class clazz = ci.defineClass(cf);
            Object obj = clazz.newInstance();

            BeanPropertyAccessor bpa = BeanPropertyAccessor.forClass(clazz);

            bpa.getPropertyValue(obj, "stuff");
        }
    }
}
