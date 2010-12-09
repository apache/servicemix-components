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

import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.tck.ReceiverComponent;

/**
 * Tests on handling fault messages with the Camel Exception handler
 */
public abstract class JbiCamelErrorHandlingTestSupport extends JbiTestSupport {

    protected static final String MESSAGE = "<just><a>test</a></just>";

    protected ReceiverComponent receiverComponent = new ReceiverComponent();

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        ActivationSpec spec;

        spec = new ActivationSpec(new ReturnFaultComponent());
        spec.setService(new QName("urn:test", "faulty-service"));
        spec.setEndpoint("endpoint");
        activationSpecList.add(spec);

        spec = new ActivationSpec(new ReturnErrorComponent(new IllegalArgumentException("iae error")));
        spec.setService(new QName("urn:test", "iae-error-service"));
        spec.setEndpoint("endpoint");
        activationSpecList.add(spec);

        spec = new ActivationSpec(new ReturnErrorComponent(new IllegalStateException("ise error")));
        spec.setService(new QName("urn:test", "ise-error-service"));
        spec.setEndpoint("endpoint");
        activationSpecList.add(spec);

        spec = new ActivationSpec(new ReturnErrorComponent(new NullPointerException("npe error")));
        spec.setService(new QName("urn:test", "npe-error-service"));
        spec.setEndpoint("endpoint");
        activationSpecList.add(spec);

        spec = new ActivationSpec(receiverComponent);
        spec.setService(new QName("urn:test", "receiver-service"));
        spec.setEndpoint("endpoint");
        activationSpecList.add(spec);

        spec = new ActivationSpec(new MyEchoComponent());
        spec.setService(new QName("urn:test", "echo-service"));
        spec.setEndpoint("endpoint");
        activationSpecList.add(spec);
    }

    protected static class ReturnErrorComponent extends ComponentSupport implements MessageExchangeListener {
        private Exception exception;

        public ReturnErrorComponent(Exception exception) {
            this.exception = exception;
        }

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                fail(exchange, exception);
            }
        }
    }

    protected static class ReturnFaultComponent extends ComponentSupport implements MessageExchangeListener {
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                Fault fault = exchange.createFault();
                fault.setContent(new StringSource("<fault/>"));
                fail(exchange, fault);
            }
        }
    }
}
