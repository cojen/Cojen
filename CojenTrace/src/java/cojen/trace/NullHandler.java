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

/**
 * TraceHandler which effectively disables tracing.
 *
 * @author Brian S O'Neill
 */
public class NullHandler implements TraceHandler {
    public void setToolbox(TraceToolbox toolbox) {
    }

    public TraceModes getTraceModes(String className) {
        return null;
    }

    public void enterMethod(int mid) {
    }

    public void enterMethod(int mid, Object argument) {
    }

    public void enterMethod(int mid, Object... arguments) {
    }

    /*
    public void newObject(int mid, Object obj) {
    }
    */

    public void exitMethod(int mid) {
    }

    public void exitMethod(int mid, long timeNanos) {
    }

    public void exitMethod(int mid, Object result) {
    }

    public void exitMethod(int mid, Object result, long timeNanos) {
    }

    public void exitMethod(int mid, Throwable t) {
    }

    public void exitMethod(int mid, Throwable t, long timeNanos) {
    }
}
