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

package cojen.example;

// Used to generate the class
import cojen.classfile.ClassFile;
import cojen.classfile.CodeBuilder;
import cojen.classfile.Label;
import cojen.classfile.LocalVariable;
import cojen.classfile.MethodInfo;
import cojen.classfile.Modifiers;
import cojen.classfile.TypeDesc;

// Used to load the generated class
import cojen.util.ClassInjector;

// used to execute the generated class
import java.lang.reflect.Method;

/**
 * This example generates a class equivalent to the following, and runs it from
 * the main method.
 *
 * <pre>
 * import java.io.*;
 *
 * public class HelloWorld {
 *     public static void main(String[] argv) {
 *         BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
 *         String name = null;
 *
 *         try {
 *             System.out.print("Please enter your name> ");
 *             name = in.readLine();
 *         } catch(IOException e) { return; }
 *
 *         System.out.println("Hello, " + name);
 *     }
 * }
 * </pre>
 *
 * @author Brian S O'Neill
 */
public class HelloWorld {
    public static void main(String[] args) throws Exception {
        ClassInjector ci = ClassInjector.create(null, null);
        Class clazz = ci.defineClass(createClassFile(ci.getClassName()));
        Method m = clazz.getMethod("main", new Class[] {String[].class});
        m.invoke(null, new Object[] {args});
    }

    private static ClassFile createClassFile(String className) {
        ClassFile cf = new ClassFile(className);
        cf.addDefaultConstructor();

        TypeDesc[] params = new TypeDesc[] {TypeDesc.STRING.toArrayType()};
        MethodInfo mi = cf.addMethod(Modifiers.PUBLIC_STATIC, "main", null, params);
        CodeBuilder b = new CodeBuilder(mi);

        TypeDesc bufferedReader = TypeDesc.forClass("java.io.BufferedReader");
        TypeDesc inputStreamReader = TypeDesc.forClass("java.io.InputStreamReader");
        TypeDesc inputStream = TypeDesc.forClass("java.io.InputStream");
        TypeDesc reader = TypeDesc.forClass("java.io.Reader");
        TypeDesc stringBuffer = TypeDesc.forClass("java.lang.StringBuffer");
        TypeDesc printStream = TypeDesc.forClass("java.io.PrintStream");

        LocalVariable in = b.createLocalVariable("in", bufferedReader);
        LocalVariable name = b.createLocalVariable("name", TypeDesc.STRING);

        b.newObject(bufferedReader);
        b.dup();
        b.newObject(inputStreamReader);
        b.dup();
        b.loadStaticField("java.lang.System", "in", inputStream);
        params = new TypeDesc[] {inputStream};
        b.invokeConstructor(inputStreamReader.getRootName(), params);
        params = new TypeDesc[] {reader};
        b.invokeConstructor(bufferedReader.getRootName(), params);
        b.storeLocal(in);

        Label tryStart = b.createLabel().setLocation();
        b.loadStaticField("java.lang.System", "out", printStream);
        b.loadConstant("Please enter your name> ");
        params = new TypeDesc[] {TypeDesc.STRING};
        b.invokeVirtual(printStream, "print", null, params);

        b.loadLocal(in);
        b.invokeVirtual(bufferedReader, "readLine", TypeDesc.STRING, null);
        b.storeLocal(name);
        Label printResponse = b.createLabel();
        b.branch(printResponse);
        Label tryEnd = b.createLabel().setLocation();

        b.exceptionHandler(tryStart, tryEnd, "java.io.IOException");
        b.returnVoid();

        printResponse.setLocation();
        b.loadStaticField("java.lang.System", "out", printStream);
        b.newObject(stringBuffer);
        b.dup();
        b.loadConstant("Hello, ");
        params = new TypeDesc[] {TypeDesc.STRING};
        b.invokeConstructor(stringBuffer, params);
        b.loadLocal(name);
        b.invokeVirtual(stringBuffer, "append", stringBuffer, params);
        b.invokeVirtual(stringBuffer, "toString", TypeDesc.STRING, null);
        params = new TypeDesc[] {TypeDesc.STRING};
        b.invokeVirtual(printStream, "println", null, params);

        b.returnVoid();

        return cf;
    }
}
