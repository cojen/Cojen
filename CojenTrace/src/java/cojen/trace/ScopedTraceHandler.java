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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cojen.util.IntHashMap;

/**
 * Abstract trace handler which gathers data on traced methods and reports it
 * upon scope exit. A scope is created when a thread enters a traced method for
 * the first time. If the traced method is marked as root, then a child scope
 * is created. A traced method marked as graft cannot create scopes -- it can
 * only participate in existing ones.
 *
 * <p>When the traced method that created the scope exits, the scope exits, and
 * gathered data is reported. If a parent scope exists, this data is
 * transferred to it.
 *
 * @author Brian S O'Neill
 */
public abstract class ScopedTraceHandler implements TraceHandler {

    private TraceToolbox mToolbox;
    private ThreadLocal<Scope> mCurrentScope = new ThreadLocal<Scope>();

    public void setToolbox(TraceToolbox toolbox) {
        mToolbox = toolbox;
    }

    /**
     * Always returns true.
     */
    public boolean allowTracing(String className) {
        return true;
    }

    public void enterMethod(int mid) {
        Scope scope = enterScope(mid);
        if (scope != null) {
            scope.enterMethod(mid);
        }
    }

    public void enterMethod(int mid, Object argument) {
        Scope scope = enterScope(mid);
        if (scope != null) {
            scope.enterMethod(mid, argument);
        }
    }

    public void enterMethod(int mid, Object... arguments) {
        Scope scope = enterScope(mid);
        if (scope != null) {
            scope.enterMethod(mid, arguments);
        }
    }

    /*
    public void newObject(int mid, Object obj) {
        Scope scope = mCurrentScope.get();
        if (scope != null) {
            scope.newObject(mid, obj);
        }
    }
    */

    public void exitMethod(int mid) {
        Scope scope = mCurrentScope.get();
        if (scope != null && scope.exitMethod(mid)) {
            exitScope(scope);
        }
    }

    public void exitMethod(int mid, long timeNanos) {
        Scope scope = mCurrentScope.get();
        if (scope != null && scope.exitMethod(mid, timeNanos)) {
            exitScope(scope);
        }
    }

    public void exitMethod(int mid, Object result) {
        Scope scope = mCurrentScope.get();
        if (scope != null && scope.exitMethod(mid, result)) {
            exitScope(scope);
        }
    }

    public void exitMethod(int mid, Object result, long timeNanos) {
        Scope scope = mCurrentScope.get();
        if (scope != null && scope.exitMethod(mid, result, timeNanos)) {
            exitScope(scope);
        }
    }

    public void exitMethod(int mid, Throwable t) {
        Scope scope = mCurrentScope.get();
        if (scope != null && scope.exitMethod(mid, t)) {
            exitScope(scope);
        }
    }

    public void exitMethod(int mid, Throwable t, long timeNanos) {
        Scope scope = mCurrentScope.get();
        if (scope != null && scope.exitMethod(mid, t, timeNanos)) {
            exitScope(scope);
        }
    }

    protected TraceToolbox getToolbox() {
        return mToolbox;
    }

    /**
     * Add an arbitrary key-value pair to the current scope, unless there is no
     * current scope.
     *
     * @return true if in a scope
     */
    protected boolean scopePut(String key, Object value) {
        Scope scope = mCurrentScope.get();
        if (scope == null) {
            return false;
        } else {
            scope.put(key, value);
            return true;
        }
    }

    /**
     * Retrieve an arbitrary key-value pair from the current scope, unless
     * there is no current scope.
     */
    protected Object scopeGet(String key) {
        Scope scope = mCurrentScope.get();
        if (scope != null) {
            return scope.getExtraData().get(key);
        }
        return null;
    }

    /**
     * Called when a scope exits, allowing subclasses to report on the data it
     * gathered.
     */
    protected abstract void report(Scope scope);

    /**
     * Returns null if no current scope and method is graft.
     */
    private Scope enterScope(int mid) {
        Scope scope = mCurrentScope.get();
        if ((scope == null || TracedMethod.isRoot(mid)) && !TracedMethod.isGraft(mid)) {
            scope = new Scope(mToolbox, scope, Thread.currentThread());
            mCurrentScope.set(scope);
        }
        return scope;
    }

    private void exitScope(Scope scope) {
        try {
            report(scope);
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable e) {
            // Don't let a buggy reporter interfere with traced method execution.
            Thread t = Thread.currentThread();
            t.getUncaughtExceptionHandler().uncaughtException(t, e);
        } finally {
            mCurrentScope.set(scope.transferToParent());
        }
    }

    public static class Scope {
        private final long mStartMillis;
        private final TraceToolbox mToolbox;
        private final Scope mParent;
        private final Thread mThread;
        private final IntHashMap mMethodDataMap;
        private final ArrayList<MethodData> mMethodDataSequence;

        private int mExitCount;
        private Object[] mArguments;
        private Object mResult;
        private Throwable mException;

        private Map<String, Object> mExtraData;

        Scope(TraceToolbox toolbox, Scope parent, Thread thread) {
            mStartMillis = System.currentTimeMillis();
            mToolbox = toolbox;
            mParent = parent;
            mThread = thread;
            mMethodDataMap = new IntHashMap();
            mMethodDataSequence = new ArrayList<MethodData>();
        }

        /**
         * Returns the time when the scope was created, as milliseconds from
         * 1970-01-01T00:00:00Z.
         */
        public long getStartTimeMillis() {
            return mStartMillis;
        }

        /**
         * Returns the thread that this scope ran in.
         */
        public Thread getThread() {
            return mThread;
        }

        /**
         * Returns true if this is the root traced scope -- it has no parent.
         */
        public boolean isRoot() {
            return mParent == null;
        }

        /**
         * Returns data for each traced method in this scope. The entry order
         * matches the order in which each method was first executed. The first
         * entry is the traced method that initiated this scope.
         */
        public List<MethodData> getMethodData() {
            return mMethodDataSequence;
        }


        /**
         * Returns the arguments passed to the first invocation of the method
         * that initiated this scope. Returns null if method has no arguments
         * or is not configured to pass arguments.
         */
        public Object[] getArguments() {
            return mArguments;
        }

        /**
         * Returns the return value passed from the first invocation of the
         * method that initiated this scope. Returns null if method has no
         * return value, or it is not configured to pass the return value, or
         * if it threw an exception.
         */
        public Object getResult() {
            return mResult;
        }

        /**
         * Returns the exception thrown from the first invocation of the method
         * that initiated this scope. Returns null if method did not throw an
         * exception or is not configured to pass the exception.
         */
        public Throwable getException() {
            return mException;
        }

        /**
         * Returns extra data stored in this scope, which may be empty.
         */
        public Map<String, Object> getExtraData() {
            Map<String, Object> extra = mExtraData;
            if (extra == null) {
                extra = Collections.emptyMap();
            }
            return extra;
        }

        void enterMethod(int mid) {
            getMethodData(mid).mCallCount++;
        }

        void enterMethod(int mid, Object... arguments) {
            if (mMethodDataSequence.size() == 0) {
                mArguments = arguments;
            }
            getMethodData(mid).mCallCount++;
        }

        /*
        void newObject(int mid, Object obj) {
            getMethodData(mid).mAllocationBytes += mToolbox.getObjectSize(obj);
        }
        */

        /**
         * @return true if scope has exited
         */
        boolean exitMethod(int mid) {
            MethodData md = getMethodData(mid);
            if (md == mMethodDataSequence.get(0)) {
                if (++mExitCount == md.mCallCount) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if scope has exited
         */
        boolean exitMethod(int mid, long timeNanos) {
            MethodData md = getMethodData(mid);
            md.mTotalTimeNanos += timeNanos;
            if (md == mMethodDataSequence.get(0)) {
                if (++mExitCount == md.mCallCount) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if scope has exited
         */
        boolean exitMethod(int mid, Object result) {
            MethodData md = getMethodData(mid);
            if (md == mMethodDataSequence.get(0)) {
                if (++mExitCount == md.mCallCount) {
                    mResult = result;
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if scope has exited
         */
        boolean exitMethod(int mid, Object result, long timeNanos) {
            MethodData md = getMethodData(mid);
            md.mTotalTimeNanos += timeNanos;
            if (md == mMethodDataSequence.get(0)) {
                if (++mExitCount == md.mCallCount) {
                    mResult = result;
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if scope has exited
         */
        boolean exitMethod(int mid, Throwable t) {
            MethodData md = getMethodData(mid);
            if (md == mMethodDataSequence.get(0)) {
                if (++mExitCount == md.mCallCount) {
                    mException = t;
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if scope has exited
         */
        boolean exitMethod(int mid, Throwable t, long timeNanos) {
            MethodData md = getMethodData(mid);
            md.mTotalTimeNanos += timeNanos;
            if (md == mMethodDataSequence.get(0)) {
                if (++mExitCount == md.mCallCount) {
                    mException = t;
                    return true;
                }
            }
            return false;
        }

        void put(String key, Object value) {
            if (mExtraData == null) {
                mExtraData = new HashMap<String, Object>();
            }
            mExtraData.put(key, value);
        }

        /**
         * Transfers gathered data to parent scope, or returns null if no
         * parent.
         */
        Scope transferToParent() {
            Scope parent = mParent;
            if (parent != null) {
                for (MethodData md : mMethodDataSequence) {
                    int mid = md.mMethod.getMethodID();
                    MethodData pmd = (MethodData) parent.mMethodDataMap.get(mid);
                    if (pmd == null) {
                        parent.mMethodDataMap.put(mid, md);
                        parent.mMethodDataSequence.add(md);
                    } else {
                        pmd.mTotalTimeNanos += md.mTotalTimeNanos;
                        pmd.mCallCount += md.mCallCount;
                        //pmd.mAllocationBytes += md.mAllocationBytes;
                    }
                }
            }
            return parent;
        }

        private MethodData getMethodData(int mid) {
            MethodData md = (MethodData) mMethodDataMap.get(mid);
            if (md == null) {
                md = new MethodData(mToolbox.getTracedMethod(mid));
                mMethodDataMap.put(mid, md);
                mMethodDataSequence.add(md);
            }
            return md;
        }
    }

    public static class MethodData {
        final TracedMethod mMethod;
        long mTotalTimeNanos;
        int mCallCount;
        //long mAllocationBytes;

        MethodData(TracedMethod method) {
            mMethod = method;
        }

        /**
         * Returns the method that was traced.
         */
        public TracedMethod getTracedMethod() {
            return mMethod;
        }

        /**
         * Returns the total time spent in executing the traced method, in
         * nanoseconds.
         */
        public long getTotalTimeNanos() {
            return mTotalTimeNanos;
        }

        /**
         * Returns the amount of times the traced method was called, which is
         * always at least one.
         */
        public int getCallCount() {
            return mCallCount;
        }

        /**
         * Returns the total amount of bytes allocated for new objects.
         */
        /*
        public long getAllocationBytes() {
            return mAllocationBytes;
        }
        */
    }
}
