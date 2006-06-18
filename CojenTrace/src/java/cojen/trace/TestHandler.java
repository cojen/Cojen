/*
 *  Copyright 2006 Brian S O'Neill
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

package cojen.trace;

import java.util.Arrays;
import java.util.List;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestHandler extends ScopedTraceHandler {
    private static TestHandler cHandler;

    public static void setMessage(String message) {
        TestHandler handler = cHandler;
        if (handler != null) {
            handler.scopePut("message", message);
        }
    }

    public TestHandler() {
        cHandler = this;
    }

    protected void report(Scope scope) {
        List<MethodData> mdList = scope.getMethodData();
        System.out.println("--------------------");
        System.out.println("operation: " + mdList.get(0).getTracedMethod());
        System.out.println("args: " + Arrays.deepToString(scope.getArguments()));
        System.out.println("result: " + scope.getResult());
        System.out.println("exception: " + scope.getException());
        System.out.println("message: " + scope.getExtraData().get("message"));
        //System.out.println("start: " + new org.joda.time.DateTime(scope.getStartTimeMillis()));
        //System.out.println("end:   " + new org.joda.time.DateTime());
        System.out.println("thread: " + scope.getThread());
        for (MethodData md : mdList) {
            System.out.println("  " + md.getTracedMethod());
            System.out.println("  " + md.getTotalTimeNanos() + "ns");
            System.out.println("  " + md.getCallCount() + " invocations");
        }
        System.out.println("EOM");
    }
}
