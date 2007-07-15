/*
 *  Copyright 2007 Brian S O'Neill
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

package org.cojen.trace;

import java.lang.reflect.Constructor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * TraceHandler which prints method trace information to standard out.
 *
 * @author Brian S O'Neill
 */
public class SimpleHandler extends ScopedTraceHandler {
    private static Constructor cDateTimeCtor;

    static {
        try {
            cDateTimeCtor = Class.forName("org.joda.time.DateTime").getConstructor(long.class);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    private static SimpleHandler cHandler;

    /**
     * Call to set an arbitrary message into the currently traced scope.
     */
    public static void setMessage(String message) {
        SimpleHandler handler = cHandler;
        if (handler != null) {
            handler.scopePut("message", message);
        }
    }

    private static String formatDate(long millis) {
        if (cDateTimeCtor != null) {
            try {
                return cDateTimeCtor.newInstance(millis).toString();
            } catch (Exception e) {
                cDateTimeCtor = null;
            }
        }

        return new Date(millis).toString();
    }

    public SimpleHandler() {
        cHandler = this;
    }

    protected void report(Scope scope) {
        List<MethodData> mdList = scope.getMethodData();
        StringBuilder b = new StringBuilder(500);

        b.append(">>> BEGIN TRACE\n");

	MethodData root = mdList.get(0);
	TracedMethod rootMethod = root.getTracedMethod();
	if (rootMethod.getOperation() != null) {
	    b.append("operation: ").append(rootMethod.getOperation()).append('\n');
	} else {
	    b.append("operation: ").append(rootMethod).append('\n');
	}

        b.append("start:     ").append(formatDate(scope.getStartTimeMillis())).append('\n');
        b.append("thread:    ").append(scope.getThread()).append('\n');

        Object message = scope.getExtraData().get("message");
        if (message != null) {
            b.append("message:   ").append(message).append('\n');
        }

        if (scope.getArguments() != null) {
            b.append("args: ").append(Arrays.deepToString(scope.getArguments())).append('\n');
        }

        if (scope.hasResult()) {
            b.append("result: ").append(scope.getResult()).append('\n');
        }

        if (scope.getException() != null) {
            b.append("exception: ").append(scope.getException()).append('\n');
        }

        b.append("details...").append('\n');
        for (MethodData md : mdList) {
            b.append("  ").append(md.getTracedMethod()).append('\n');
            int calls = md.getCallCount();
            long time = md.getTotalTimeNanos();
            b.append("    invocations:  ").append(calls).append('\n');
	    if (md.hasTotalTimeNanos()) {
		b.append("    total time:   ").append(time).append("ns\n");
		double dTime = (double) time;
		if (calls == 1) {
		    b.append("    average time: ").append(time).append("ns\n");
		} else {
		    double average = dTime / calls;
		    b.append("    average time: ").append(average).append("ns\n");
		}
		if (time == root.getTotalTimeNanos()) {
		    b.append("    percent time: 100\n");
		} else {
		    double percent = 100.0 * (dTime / root.getTotalTimeNanos());
		    b.append("    percent time: ").append(percent).append('\n');
		}
	    }
        }
        b.append("<<< END TRACE");

        System.out.println(b);
    }
}
