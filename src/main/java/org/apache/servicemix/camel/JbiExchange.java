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
package org.apache.servicemix.camel;

import javax.jbi.JBIException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.impl.DefaultExchange;

/**
 * An {@link org.apache.camel.Exchange} working with JBI which exposes the underlying JBI
 * features such as the JBI {@link #getMessageExchange()},
 * {@link #getInMessage()} and {@link #getOutMessage()}
 *
 * @version $Revision: 563665 $
 */
public class JbiExchange extends DefaultExchange {
    private final JbiBinding binding;

    private MessageExchange messageExchange;

    public JbiExchange(CamelContext context, JbiBinding binding) {
        super(context);
        this.binding = binding;
        populateProperties();
    }



    public JbiExchange(CamelContext context, JbiBinding binding, MessageExchange messageExchange) {
        super(context);
        this.binding = binding;
        this.messageExchange = messageExchange;

        setPattern(ExchangePattern.fromWsdlUri(messageExchange.getPattern().toString()));
        populateProperties();
    }

    @Override
    public JbiMessage getIn() {
        return (JbiMessage) super.getIn();
    }

    @Override
    public JbiMessage getOut() {
        return (JbiMessage) super.getOut();
    }

    @Override
    public JbiMessage getOut(boolean lazyCreate) {
        return (JbiMessage) super.getOut(lazyCreate);
    }

    @Override
    public JbiMessage getFault() {
        return (JbiMessage) super.getFault();
    }

    @Override
    public JbiMessage getFault(boolean lazyCreate) {
        return (JbiMessage) super.getFault(lazyCreate);
    }

    @Override
    public org.apache.camel.Exchange newInstance() {        
        return new JbiExchange(this.getContext(), this.getBinding(), this.getMessageExchange());
    }

    /**
     * @return the Camel <-> JBI binding
     */
    public JbiBinding getBinding() {
        return binding;
    }

    // Expose JBI features
    // -------------------------------------------------------------------------

    /**
     * Returns the underlying JBI message exchange for an inbound exchange or
     * null for outbound messages
     *
     * @return the inbound message exchange
     */
    public MessageExchange getMessageExchange() {
        return messageExchange;
    }

    /**
     * Returns the underlying In {@link NormalizedMessage}
     *
     * @return the In message
     */
    public NormalizedMessage getInMessage() {
        return getIn().getNormalizedMessage();
    }

    /**
     * Returns the underlying Out {@link NormalizedMessage}
     *
     * @return the Out message
     */
    public NormalizedMessage getOutMessage() {
        return getOut().getNormalizedMessage();
    }

    /**
     * Returns the underlying Fault {@link NormalizedMessage}
     *
     * @return the Fault message
     */
    public NormalizedMessage getFaultMessage() {
        return getFault().getNormalizedMessage();
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    protected JbiMessage createInMessage() {
        return createMessage("in");
    }

    @Override
    protected JbiMessage createOutMessage() {
        return createMessage("out");
    }

    @Override
    protected JbiMessage createFaultMessage() {
        return createMessage("fault");
    }

    private JbiMessage createMessage(String name) {
        if (messageExchange != null) {
            try {
                NormalizedMessage msg = messageExchange.getMessage(name);
                if (msg == null) {
                    msg = messageExchange.createMessage();
                    messageExchange.setMessage(msg, name);
                }
                return new JbiMessage(msg);
            } catch (JBIException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new JbiMessage();
        }
    }

    private void populateProperties() {
        if (messageExchange != null && messageExchange.getOperation() != null) {
            setProperty("jbi.operation", messageExchange.getOperation().toString());
        }
    }

}
