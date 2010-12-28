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

package org.cojen.util;

import java.util.Collection;
import java.util.Map;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public interface Cache<K, V> {
    int size();

    boolean isEmpty();

    V get(K key);

    V put(K key, V value);

    V putIfAbsent(K key, V value);

    V remove(K key);

    boolean remove(K key, V value);

    boolean replace(K key, V oldValue, V newValue);

    V replace(K key, V value);

    void copyKeysInto(Collection<? super K> c);

    void copyValuesInto(Collection<? super V> c);

    void copyEntriesInto(Collection<? super Map.Entry<K, V>> c);

    void clear();
}
