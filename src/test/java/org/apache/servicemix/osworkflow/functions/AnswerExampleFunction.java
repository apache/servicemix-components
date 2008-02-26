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
package org.apache.servicemix.osworkflow.functions;

import java.util.Map;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.osworkflow.OSWorkflow;
import org.apache.servicemix.osworkflow.OSWorkflowEndpoint;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;

/**
 * @author lhe
 */
public class AnswerExampleFunction implements FunctionProvider {

    private static Log logger = LogFactory.getLog(AnswerExampleFunction.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.opensymphony.workflow.FunctionProvider#execute(java.util.Map,
     *      java.util.Map, com.opensymphony.module.propertyset.PropertySet)
     */
    public void execute(Map transientVars, Map args, PropertySet propertySet)
            throws WorkflowException {
        OSWorkflowEndpoint ep = null;
        boolean isAsynchron = false;
        MessageExchange exchange = null;

        if (transientVars.containsKey(OSWorkflow.KEY_ENDPOINT)) {
            ep = (OSWorkflowEndpoint) transientVars
                    .get(OSWorkflow.KEY_ENDPOINT);
        } else {
            throw new WorkflowException(
                    "AnswerExampleFunction: Missing transient variable for endpoint object. ("
                            + OSWorkflow.KEY_ENDPOINT + ")");
        }

        if (transientVars.containsKey(OSWorkflow.KEY_ASYNC_PROCESSING)) {
            isAsynchron = (Boolean) transientVars
                    .get(OSWorkflow.KEY_ASYNC_PROCESSING);
        } else {
            throw new WorkflowException(
                    "AnswerExampleFunction: Missing transient variable for async object. ("
                            + OSWorkflow.KEY_ASYNC_PROCESSING + ")");
        }

        if (transientVars.containsKey(OSWorkflow.KEY_EXCHANGE)) {
            exchange = (MessageExchange) transientVars
                    .get(OSWorkflow.KEY_EXCHANGE);
        } else {
            throw new WorkflowException(
                    "AnswerExampleFunction: Missing transient variable for exchange object. ("
                            + OSWorkflow.KEY_EXCHANGE + ")");
        }

        if (!isAsynchron) {
            try {
                NormalizedMessage msg = exchange.createMessage();
                msg.setContent(new StringSource(
                        "<example>Test passed!</example>"));
                exchange.setMessage(msg, "out");
                ep.send(exchange, false);
            } catch (Exception ex) {
                throw new WorkflowException(
                        "AnswerExampleFunction: Unable to answer request.", ex);
            }
        }
    }
}
