/*
 *  Copyright 2004-2010 Brian S O'Neill
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

package org.cojen.example;

// Used to generate the class
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.RuntimeClassFile;
import org.cojen.classfile.TypeDesc;

// Used to execute the generated class
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
        // Create the RuntimeClassFile using an automatically selected name.
        RuntimeClassFile cf = createClassFile();
        Class clazz = cf.defineClass();

        // Find the generated main method and invoke it.
        Method m = clazz.getMethod("main", new Class[] {String[].class});
        m.invoke(null, new Object[] {args});
    }

    /**
     * Creates a ClassFile which defines a simple interactive HelloWorld class.
     *
     * @param className name given to class
     */
    private static RuntimeClassFile createClassFile() {
        // Create a ClassFile with the super class of Object.
        RuntimeClassFile cf = new RuntimeClassFile();

        // Default constructor works only if super class has an accessible
        // no-arg constructor.
        cf.addDefaultConstructor();

        // Add the main method, and construct a CodeBuilder for defining the
        // bytecode.
        TypeDesc[] params = new TypeDesc[] {TypeDesc.STRING.toArrayType()};
        MethodInfo mi = cf.addMethod(Modifiers.PUBLIC_STATIC, "main", null, params);
        CodeBuilder b = new CodeBuilder(mi);

        // Create some types which will be needed later.
        TypeDesc bufferedReader = TypeDesc.forClass("java.io.BufferedReader");
        TypeDesc inputStreamReader = TypeDesc.forClass("java.io.InputStreamReader");
        TypeDesc inputStream = TypeDesc.forClass("java.io.InputStream");
        TypeDesc reader = TypeDesc.forClass("java.io.Reader");
        TypeDesc stringBuffer = TypeDesc.forClass("java.lang.StringBuffer");
        TypeDesc printStream = TypeDesc.forClass("java.io.PrintStream");

        // Declare local variables to be used.
        LocalVariable in = b.createLocalVariable("in", bufferedReader);
        LocalVariable name = b.createLocalVariable("name", TypeDesc.STRING);

        // Create the first line of code, corresponding to 
        // in = new BufferedReader(new InputStreamReader(System.in));
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

        // Create and locate a label for the start of the "try" block.
        Label tryStart = b.createLabel().setLocation();

        // Create input prompt.
        b.loadStaticField("java.lang.System", "out", printStream);
        b.loadConstant("Please enter your name> ");
        params = new TypeDesc[] {TypeDesc.STRING};
        b.invokeVirtual(printStream, "print", null, params);

        // Read a line from the reader, and store it in the "name" variable.
        b.loadLocal(in);
        b.invokeVirtual(bufferedReader, "readLine", TypeDesc.STRING, null);
        b.storeLocal(name);

        // If no exception is thrown, branch to a label to print the
        // response. The location of the label has not yet been set.
        Label printResponse = b.createLabel();
        b.branch(printResponse);

        // Create and locate a label for the end of the "try" block.
        Label tryEnd = b.createLabel().setLocation();

        // Create the "catch" block.
        b.exceptionHandler(tryStart, tryEnd, "java.io.IOException");
        b.returnVoid();

        // If no exception, then branch to this location to print the response.
        printResponse.setLocation();


        // Create the line of code, corresponding to 
        // System.out.println("Hello, " + name);
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

        // The last instruction reached must be a return or else the class
        // verifier will complain.
        b.returnVoid();

        return cf;
    }
}
