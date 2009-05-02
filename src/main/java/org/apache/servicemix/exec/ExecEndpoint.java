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
package org.apache.servicemix.exec;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.exec.marshaler.DefaultExecMarshaler;
import org.apache.servicemix.exec.marshaler.ExecMarshalerSupport;
import org.apache.servicemix.exec.utils.ExecUtils;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Represents an exec endpoint.
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="endpoint"
 */
public class ExecEndpoint extends ProviderEndpoint {

    private String command; // the command can be static (define in the
                            // descriptor) or provided in the incoming message
    private ExecMarshalerSupport marshaler = new DefaultExecMarshaler(); // the marshaler that parse the in message

    public String getCommand() {
        return command;
    }

    /**
     * <p>
     * This attribute specifies the default command to use if no is provided
     * in the incoming message.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <code>null</code>. 
     * 
     * @param command
     */
    public void setCommand(String command) {
        this.command = command;
    }
    
    public ExecMarshalerSupport getMarshaler() {
        return marshaler;
    }
    
    /**     
     * <p>
     * With this method you can specify a marshaler class which provides the
     * logic for converting a message into a execution command. This class
     * has to implement the interface class <code>ExecMarshalerSupport</code>. 
     * If you don't specify a marshaler, the <code>DefaultExecMarshaler</code> 
     * will be used.
     * </p>
     * 
     * @param marshaler a <code>ExecMarshalerSupport</code> class representing the
     *            marshaler.
     */
    public void setMarshaler(ExecMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, this means that another component
        // has requested our service
        // As this exchange is active, this is either an in or a fault (out are
        // sent by this component)
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            // exchange is finished
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // exchange has been aborted with an exception
            return;
        } else {
            // exchange is active
            this.handleProviderExchange(exchange);
        }
    }

    /**
     * <p>
     * Handles on the message exchange (provider role).
     * </p>
     * 
     * @param exchange
     *            the <code>MessageExchange</code>.
     */
    protected void handleProviderExchange(MessageExchange exchange) throws Exception {
        // fault message
        if (exchange.getFault() != null) {
            done(exchange);
        } else if (exchange.getMessage("in") != null) {
            // in message presents
            if (logger.isDebugEnabled()) {
                logger.debug("Received exchange: " + exchange);
            }
            // gets the in message
            NormalizedMessage in = exchange.getMessage("in");
            // parses the in message and get the execution command
            String exec = marshaler.constructExecCommand(in);
            if (exec == null || exec.trim().length() < 1) {
                exec = command;
            }
            if (exec == null || exec.trim().length() < 1) {
                throw new MessagingException("No command to execute.");
            }
            // executes the command
            String output = ExecUtils.execute(exec);
            if (exchange instanceof InOut) {
                // pushes the execution output in out message
                NormalizedMessage out = exchange.createMessage();
                out.setContent(new StringSource("<output>" + output + "</output>"));
                exchange.setMessage(out, "out");
                send(exchange);
            } else {
                done(exchange);
            }
        } else {
            if (command != null) {
                // executes the user-defined command
                String output = ExecUtils.execute(command);
                if (exchange instanceof InOut) {
                    NormalizedMessage out = exchange.createMessage();
                    out.setContent(new StringSource(marshaler.formatExecutionOutput(output)));
                    exchange.setMessage(out, "out");
                    send(exchange);
                } else {
                    done(exchange);
                }
            } else {
                throw new IllegalStateException("Provider exchange is ACTIVE, but no fault or command is provided.");
            }
        }
    }

}
