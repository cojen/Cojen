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
public class Test3 {
    public static void main(String[] args) throws Exception {
        MethodDesc desc = MethodDesc.forDescriptor(args[0]);
        System.out.println(desc);
        ObjectOutputStream out = new ObjectOutputStream
            (new FileOutputStream(args[1]));
        out.writeObject(desc);
        out.close();
        
        ObjectInputStream in = new ObjectInputStream
            (new FileInputStream(args[1]));
        MethodDesc desc2 = (MethodDesc)in.readObject();
        in.close();
        
        System.out.println(desc2);
        System.out.println(desc == desc2);
    }
}
