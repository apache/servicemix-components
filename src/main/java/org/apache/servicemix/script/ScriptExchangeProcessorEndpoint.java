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
package org.apache.servicemix.script;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * @org.apache.xbean.XBean element="exchangeProcessor"
 */
public class ScriptExchangeProcessorEndpoint extends ProviderEndpoint {

    private ExchangeProcessor implementation;

    private List helpers = new ArrayList();

    public List getHelpers() {
        return helpers;
    }

    public void setHelpers(List helpers) {
        this.helpers = helpers;
        for (Iterator iterator = helpers.iterator(); iterator.hasNext();) {
            Object nextHelper = iterator.next();
            if (nextHelper instanceof ScriptHelper) {
                ((ScriptHelper) nextHelper).setScriptExchangeProcessorEndpoint(this);
            }
        }
    }

    public ExchangeProcessor getImplementation() {
        return implementation;
    }

    public void setImplementation(ExchangeProcessor implementation) {
        this.implementation = implementation;
    }

    public void start() throws Exception {
        super.start();
        logger = this.serviceUnit.getComponent().getLogger();
        if (implementation != null) {
            implementation.start();
        }
    }

    public void stop() throws Exception {
        if (implementation != null) {
            try {
                implementation.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.stop();
    }

    public void process(MessageExchange exchange) throws Exception {
        if (implementation != null) {
            implementation.process(exchange);
        }
    }

    protected void fail(MessageExchange messageExchange, Exception e) throws MessagingException {
        super.fail(messageExchange, e);
    }

    protected void send(MessageExchange messageExchange) throws MessagingException {
        super.send(messageExchange);
    }

    protected void sendSync(MessageExchange messageExchange) throws MessagingException {
        super.sendSync(messageExchange);
    }

    protected void done(MessageExchange messageExchange) throws MessagingException {
        super.done(messageExchange);
    }
}
