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

import java.util.*;
import cojen.util.SoftValuedHashMap;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestSoftValuedHashMap {
    /**
     * Test program.
     */
    public static void main(String[] arg) throws Exception {
        Map cache = new SoftValuedHashMap();

        for (int i = 0, j = 0; i < 100000; i++, j += 15) {
            if (i % 100 == 0) {
                System.out.println("Size = " + cache.size());
            }

            //Thread.sleep(1);

            Integer key = new Integer(i);
            Integer value = new Integer(j);

            cache.put(key, value);
        }
  
        Map.Entry entry = (Map.Entry)cache.entrySet().iterator().next();
        System.out.println(entry);
        //entry = null;

        System.out.println(cache);

        int originalSize = cache.size();

        //cache = null;

        for (int i=0; i<100; i++) {
            System.gc();
        }

        System.out.println(cache);

        System.out.println(originalSize);
        System.out.println(cache.size());
        System.out.println(entry);

        Thread.sleep(1000000);
    }
}
