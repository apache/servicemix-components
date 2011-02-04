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
package org.apache.servicemix.soap.ws.addressing;

import java.io.InputStream;

import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.InOut;
import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.JBIException;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Document;

import junit.framework.TestCase;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.InterceptorProvider;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.wsdl.WSDLUtils;
import org.apache.servicemix.soap.wsdl.BindingFactory;
import org.apache.servicemix.soap.wsdl.validator.WSIBPValidator;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.tck.mock.MockExchangeFactory;
import org.apache.servicemix.tck.mock.MockComponentContext;

public class WsAddressingTest extends TestCase {

    private final Logger logger = LoggerFactory.getLogger(WsAddressingTest.class);

    public void testWsAddressing() throws Exception {
        Binding<?> binding = getBinding("simple.wsdl");
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(InterceptorProvider.Phase.ServerIn));
        phaseIn.add(new WsAddressingPolicy().getInterceptors(InterceptorProvider.Phase.ServerIn));

        ComponentContext context = new DummyComponentContext();

        Message msg = new MessageImpl();
        msg.put(ComponentContext.class, context);
        msg.put(Binding.class, binding);
        msg.setContent(InputStream.class, getClass().getResourceAsStream("addressing-request.xml"));
        msg.put(MessageExchangeFactory.class, new MockExchangeFactory());
        phaseIn.doIntercept(msg);

        MessageExchange exchange = msg.getContent(MessageExchange.class);
        assertNotNull(exchange);
        assertTrue(exchange instanceof InOut);
        NormalizedMessage in = exchange.getMessage("in");
        assertNotNull(in);
        Source content = in.getContent();
        assertNotNull(content);
        ServiceEndpoint se = exchange.getEndpoint();
        assertNotNull(se);
        assertEquals(new QName("uri:another:HelloWorld", "HelloWorldService"), se.getServiceName());
        assertEquals("HelloWorldPort", se.getEndpointName());
    }

    protected Binding<?> getBinding(String wsdlResource) throws Exception {
        String url = getClass().getResource(wsdlResource).toString();
        WSDLReader reader = WSDLUtils.createWSDL11Reader();
        Definition def = reader.readWSDL(url);
        WSIBPValidator validator = new WSIBPValidator(def);
        if (!validator.isValid()) {
            for (String err : validator.getErrors()) {
                logger.info(err);
            }
        }
        Service svc = (Service) def.getServices().values().iterator().next();
        Port port = (Port) svc.getPorts().values().iterator().next();
        Binding<?> binding = BindingFactory.createBinding(port);
        return binding;
    }

    protected static class DummyComponentContext extends MockComponentContext {
        @Override
        public ServiceEndpoint getEndpoint(final QName service, final String name) {
            return new ServiceEndpoint() {
                public DocumentFragment getAsReference(QName operationName) {
                    throw new UnsupportedOperationException();
                }
                public String getEndpointName() {
                    return name;
                }
                public QName[] getInterfaces() {
                    return new QName[0];
                }
                public QName getServiceName() {
                    return service;
                }
            };
        }

        @Override
        public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
            if (new QName("uri:another:HelloWorld", "HelloWorldService").equals(endpoint.getServiceName()) && "HelloWorldPort".equals(endpoint.getEndpointName())) {
                return DomUtil.parse(getClass().getResourceAsStream("HelloWorld-DOC.wsdl"));
            } else {
                return null;
            }
        }
    }

}
