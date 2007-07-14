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

package org.cojen.trace;

import static org.cojen.trace.TraceMode.*;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TraceModes {
    /** All modes on, regardles off user Trace annotation */
    public static final TraceModes ALL_ON;

    /** All modes off, regardles off user Trace annotation */
    public static final TraceModes ALL_OFF;

    /** All modes selected by user Trace annotation */
    public static final TraceModes ALL_USER;

    static {
        ALL_ON = new TraceModes(ON, ON, ON, ON, ON);
        ALL_OFF = new TraceModes(OFF, OFF, OFF, OFF, OFF);
        ALL_USER = new TraceModes(USER, USER, USER, USER, USER);
    }

    private final TraceMode mTraceCalls;
    private final TraceMode mTraceArguments;
    private final TraceMode mTraceResult;
    private final TraceMode mTraceException;
    private final TraceMode mTraceTime;

    public TraceModes(TraceMode traceCalls,
                      TraceMode traceArguments,
                      TraceMode traceResult,
                      TraceMode traceException,
                      TraceMode traceTime)
    {
        mTraceCalls = traceCalls;
        mTraceArguments = traceArguments;
        mTraceResult = traceResult;
        mTraceException = traceException;
        mTraceTime = traceTime;
    }

    /**
     * Returns mode for tracing method calls, which is implicitly on if any
     * other tracing feature is enabled.
     */
    public TraceMode getTraceCalls() {
        return mTraceCalls;
    }

    /**
     * Returns mode for tracing the arguments of methods.
     */
    public TraceMode getTraceArguments() {
        return mTraceArguments;
    }

    /**
     * Returns mode for tracing the return value of methods.
     */
    public TraceMode getTraceResult() {
        return mTraceResult;
    }

    /**
     * Returns mode for tracing the thrown exception of methods.
     */
    public TraceMode getTraceException() {
        return mTraceException;
    }

    /**
     * Returns mode for tracing the execution time of methods.
     */
    public TraceMode getTraceTime() {
        return mTraceTime;
    }

    @Override
    public int hashCode() {
        return mTraceCalls.hashCode() + mTraceArguments.hashCode() + mTraceResult.hashCode() +
            mTraceException.hashCode() + mTraceTime.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TraceModes) {
            TraceModes other = (TraceModes) obj;
            return mTraceCalls == other.mTraceCalls &&
                mTraceArguments == other.mTraceArguments &&
                mTraceResult == other.mTraceResult &&
                mTraceException == other.mTraceException &&
                mTraceTime == other.mTraceTime;
        }
        return false;
    }
}
