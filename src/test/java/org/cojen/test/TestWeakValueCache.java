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

import org.cojen.util.Cache;
import org.cojen.util.WeakValueCache;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestWeakValueCache {
    public static void main(String[] arg) throws Exception {
        Cache<Integer, Integer> cache = new WeakValueCache<Integer, Integer>(100);

        for (int i = 0, j = 0; i < 100000; i++, j += 15) {
            if (i % 100 == 0) {
                System.out.println("Size = " + cache.size());
            }
            Integer key = new Integer(i);
            Integer value = new Integer(j);
            cache.put(key, value);
        }
  
        int originalSize = cache.size();

        while (true) {
            System.out.println(cache.size());
            System.gc();
            Thread.sleep(1000);
            System.out.println(cache);
        }
    }
}
