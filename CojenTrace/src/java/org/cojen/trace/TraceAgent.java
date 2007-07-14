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

import java.lang.instrument.Instrumentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.security.SecureRandom;

import static org.cojen.trace.TraceMode.*;

/**
 * Instrumentation agent for tracing.
 *
 * @author Brian S O'Neill
 */
public class TraceAgent {
    private static final Random cRandom = new SecureRandom();
    private static final Map<Long, TraceAgent> cAgents = new HashMap<Long, TraceAgent>();

    /**
     * Premain method, as required by instrumentation agents.
     *
     * @param agentArgs specify trace handler class name
     * @param inst instrumentation instance passed in by JVM
     * @see TraceHandler
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        TraceAgent agent = new TraceAgent(agentArgs, inst);
        inst.addTransformer(new Transformer(agent));
    }

    /**
     * Method called by instrumented class to get reference to agent.
     */
    public static TraceAgent getTraceAgent(long id) {
        return cAgents.get(id);
    }

    private static synchronized long registerAgent(TraceAgent agent) {
        Long id;
        do {
            id = cRandom.nextLong();
        } while (cAgents.containsKey(id));
        cAgents.put(id, agent);
        return id;
    }

    private final long mAgentID;
    private final TracedMethodRegistry mRegistry = new TracedMethodRegistry();
    private final TraceHandler mHandler;

    private TraceAgent(String agentArgs, Instrumentation inst) throws Exception {
        if (agentArgs == null) {
            throw new IllegalArgumentException
                ("Must pass handler class name. For example, \"" +
                 "java -javaagent:<path to agent jar file>=<class name of trace handler> ...\"");
        }

        TraceHandler handler = (TraceHandler) Class.forName(agentArgs).newInstance();
        handler.setToolbox(new TraceToolbox(mRegistry, inst));

        mAgentID = registerAgent(this);
        mHandler = handler;
    }

    /**
     * Method called by instrumented class to get the trace handler.
     */
    public TraceHandler getTraceHandler() {
        return mHandler;
    }

    /**
     * Method called by instrumented class to register traced methods.
     */
    public void registerTraceMethod(int mid, Class clazz, String name, Class... paramTypes) {
        mRegistry.registerMethod(mid, new TracedMethod(mid, clazz, name, paramTypes));
    }

    public long getAgentID() {
        return mAgentID;
    }

    TraceModes getTraceModes(String className) {
        return mHandler.getTraceModes(className);
    }

    int reserveMethod(boolean root, boolean graft) {
        return mRegistry.reserveMethod(root, graft);
    }
}
