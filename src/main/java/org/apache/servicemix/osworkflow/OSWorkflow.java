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

import java.util.Map;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.RobustInOnly;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.InvalidRoleException;
import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.basic.BasicWorkflow;
import com.opensymphony.workflow.config.DefaultConfiguration;

/**
 * @author lhe
 */
public class OSWorkflow extends Thread {
    private static Log log = LogFactory.getLog(OSWorkflow.class);

    public static final String KEY_EXCHANGE = "exchange";

    public static final String KEY_IN_MESSAGE = "in-message";

    public static final String KEY_ENDPOINT = "endpoint";

    public static final String KEY_CALLER = "caller";

    public static final String KEY_ASYNC_PROCESSING = "asynchronous";

    private Workflow osWorkflowInstance = null;

    private String caller = null;

    private String osWorkflowName = null;

    private Map map = null;

    private int action = -1;

    private long workflowId = -1l;

    private boolean finished = false;

    private boolean aborted = false;

    private OSWorkflowEndpoint endpoint = null;

    private MessageExchange exchange = null;

    /**
     * creates and initializes a new workflow object
     * 
     * @param ep
     *            the endpoint reference
     * @param workflowName
     *            the unique workflow name as defined in workflows.xml
     * @param action
     *            the initial action
     * @param map
     *            the value map
     * @param caller
     *            the caller
     * @param exchange
     *            the received message exchange
     */
    public OSWorkflow(OSWorkflowEndpoint ep, String workflowName, int action,
            Map map, String caller, MessageExchange exchange) {
        super(workflowName);
        setDaemon(true);

        this.endpoint = ep; // remember the endpoint which called the osworkflow
        this.osWorkflowName = workflowName;
        this.osWorkflowInstance = null;
        this.action = action;
        this.map = map;
        this.caller = caller;
        this.exchange = exchange;

        // now fill the transient vars with some useful objects
        this.map.put(KEY_ENDPOINT, this.endpoint);
        this.map.put(KEY_CALLER, this.caller);
        this.map.put(KEY_IN_MESSAGE, this.exchange.getMessage("in"));
        this.map.put(KEY_EXCHANGE, this.exchange);
        this.map
                .put(
                        KEY_ASYNC_PROCESSING,
                        (this.exchange instanceof InOnly || this.exchange instanceof RobustInOnly));
    }

    /**
     * initializes the workflow and a default config
     * 
     * @return the unique workflow id
     */
    private long createWorkflow() throws InvalidRoleException,
            InvalidInputException, WorkflowException {
        this.osWorkflowInstance = new BasicWorkflow(this.caller);
        DefaultConfiguration config = new DefaultConfiguration();
        this.osWorkflowInstance.setConfiguration(config);
        long workflowId = this.osWorkflowInstance.initialize(
                this.osWorkflowName, this.action, this.map);
        return workflowId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        // call the endpoint method for init actions
        this.endpoint.preWorkflow();

        log.debug("Starting workflow...");
        log.debug("Name:       " + this.osWorkflowName);
        log.debug("Action:     " + this.action);
        log.debug("Caller:     " + this.caller);
        log.debug("Map:        " + this.map);

        // loop as long as there are more actions to do and the workflow is not
        // finished or aborted
        while (!finished && !aborted) {
            // initial creation
            if (this.osWorkflowInstance == null) {
                try {
                    this.workflowId = createWorkflow();
                } catch (Exception ex) {
                    log.error("Error creating the workflow", ex);
                    aborted = true;
                    break;
                }
            }

            // determine the available actions
            int[] availableActions = this.osWorkflowInstance
                    .getAvailableActions(this.workflowId, this.map);

            // check if there are more actions available
            if (availableActions.length == 0) {
                // no, no more actions available - workflow finished
                log.debug("No more actions. Workflow is finished...");
                this.finished = true;
            } else {
                // get first available action to execute
                int nextAction = availableActions[0];

                log.debug("call action " + nextAction);
                try {
                    // call the action
                    this.osWorkflowInstance.doAction(this.workflowId,
                            nextAction, this.map);
                } catch (InvalidInputException iiex) {
                    log.error(iiex);
                    aborted = true;
                } catch (WorkflowException wfex) {
                    log.error(wfex);
                    aborted = true;
                }
            }
        }

        log.debug("Stopping workflow...");
        log.debug("Name:       " + this.osWorkflowName);
        log.debug("Action:     " + this.action);
        log.debug("Caller:     " + this.caller);
        log.debug("Map:        " + this.map);
        log.debug("WorkflowId: " + this.workflowId);
        log.debug("End state:  " + (finished ? "Finished" : "Aborted"));

        // call the endpoint method for cleanup actions or message exchange
        this.endpoint.postWorkflow();
    }
}
