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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cojen.classfile.attr.CodeAttr;
import cojen.classfile.attr.SignatureAttr;

/**
 * 
 * @author Brian S O'Neill
 */
public class DisassemblyTool {
    public static void main(String[] args) throws Exception {
        ClassFileDataLoader loader;
        InputStream in;

        try {
            final File file = new File(args[0]);
            in = new FileInputStream(file);
            loader = new ClassFileDataLoader() {
                public InputStream getClassData(String name)
                    throws IOException
                {
                    name = name.substring(name.lastIndexOf('.') + 1);
                    File f = new File(file.getParentFile(), name + ".class");

                    if (f.exists()) {
                        return new FileInputStream(f);
                    }

                    return null;
                }
            };
        } catch (FileNotFoundException e) {
            if (args[0].endsWith(".class")) {
                System.err.println(e);
                return;
            }

            loader = new ResourceClassFileDataLoader();
            in = loader.getClassData(args[0]);

            if (in == null) {
                System.err.println(e);
                return;
            }
        }

        in = new BufferedInputStream(in);
        ClassFile cf = ClassFile.readFrom(in, loader, null);

        PrintWriter out = new PrintWriter(System.out);

        String style = null;
        if (args.length > 1) {
            style = args[1];
        }

        Printer p;
        if (style == null || style.equals("assembly")) {
            p = new AssemblyStylePrinter();
        } else if (style.equals("builder")) {
            p = new BuilderStylePrinter();
        } else {
            System.err.println("Unknown format style: " + style);
            return;
        }

        p.disassemble(cf, out);

        out.flush();
    }

    public static interface Printer {
        void disassemble(ClassFile cf, PrintWriter out);
    }
}
