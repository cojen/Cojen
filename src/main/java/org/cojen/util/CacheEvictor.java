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

import java.lang.ref.ReferenceQueue;

/**
 * Singleton background thread which clears entries from caching classes. All cached
 * entries must implement the Ref interface.
 *
 * @author Brian S O'Neill
 */
class CacheEvictor extends Thread {
    private final static CacheEvictor cEvictor;

    static {
        CacheEvictor evictor = new CacheEvictor();
        evictor.setName("Cache Evictor");
        evictor.setDaemon(true);
        evictor.setPriority(Thread.MAX_PRIORITY);
        evictor.start();
        cEvictor = evictor;
    }

    static ReferenceQueue<Object> queue() {
        return cEvictor.mQueue;
    }

    private final ReferenceQueue<Object> mQueue;

    private CacheEvictor() {
        mQueue = new ReferenceQueue<Object>();
    }

    @Override
    public void run() {
        try {
            while (true) {
                ((Ref) mQueue.remove()).remove();
            }
        } catch (ClassCastException e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
        } catch (InterruptedException e) {
        }
    }

    static interface Ref {
        void remove();
    }
}
