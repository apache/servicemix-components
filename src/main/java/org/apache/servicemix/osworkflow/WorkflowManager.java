/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.osworkflow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lhe
 */
public final class WorkflowManager {
    private static final int WORKER_COUNT = 30;

    private static WorkflowManager manager;

    private ExecutorService executor;

    /**
     * private constructor - singleton pattern
     */
    private WorkflowManager() {
        executor = Executors.newFixedThreadPool(WORKER_COUNT);
    }

    /**
     * returns the instance of the task queue
     * 
     * @return manager instance
     */
    public static synchronized WorkflowManager getInstance() {
        if (manager == null) {
            manager = new WorkflowManager();
        }
        return manager;
    }

    /**
     * executes the given runnable inside the task queue
     * 
     * @param r
     *            a runnable object
     */
    public void executeWorkflow(Runnable r) {
        executor.execute(r);
    }

    /**
     * shuts the task queue down
     * 
     * @param firstFinishRunningTasks
     *            true if running task should be finished before shutdown
     */
    public void prepareShutdown(boolean firstFinishRunningTasks) {
        if (firstFinishRunningTasks) {
            executor.shutdown();
        } else {
            executor.shutdownNow();
        }
    }
}
