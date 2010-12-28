/*
 *  Copyright 2010 Brian S O'Neill
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

package org.cojen.test;

import org.cojen.util.WeakCanonicalSet;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestWeakCanonicalSet {
    public static void main(String[] arg) throws Exception {
        WeakCanonicalSet<Integer> cache = new WeakCanonicalSet<Integer>();

        for (int i = 0; i < 1000000; i++) {
            if (i % 100 == 0) {
                System.out.println("Size = " + cache.size());
            }
            Integer value = new Integer(i % 10000);
            cache.put(value);
        }
  
        while (true) {
            System.out.println(cache.size());
            System.gc();
            Thread.sleep(1000);
            System.out.println(cache);
        }
    }
}
