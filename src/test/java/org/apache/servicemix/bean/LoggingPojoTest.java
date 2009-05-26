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
package org.apache.servicemix.bean;

import java.util.LinkedList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.servicemix.bean.pojos.LoggingPojo;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;

public class LoggingPojoTest extends TestCase {

    protected JBIContainer container;
    protected BeanComponent component;
    protected List<String> messages;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setEmbedded(true);
        container.init();

        component = new BeanComponent();
        messages = new LinkedList<String>();
        BeanEndpoint loggingEndpoint = new BeanEndpoint();
        LoggingPojo pojo = new LoggingPojo();
        pojo.setMaxMsgDisplaySize(35);
        pojo.setLog(new SimpleLog("my-logger") {
            @Override
            protected void log(int type, Object message, Throwable t) {
                messages.add(message.toString());
            }
        });
        loggingEndpoint.setBean(pojo);
        loggingEndpoint.setService(new QName("logging"));
        loggingEndpoint.setInterfaceName(new QName("logservice"));
        loggingEndpoint.setEndpoint("endpoint");
        component.addEndpoint(loggingEndpoint);
        container.activateComponent(component, "servicemix-bean");

        container.start();
    }

    protected void tearDown() throws Exception {
        container.shutDown();
    }

    public void testInOnly() throws Exception {
        ServiceMixClient client = new ServiceMixClientFacade(component.getComponentContext());
        InOnly exchange = client.createInOnlyExchange();
        fillExchange(exchange);
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        assertLog();
    }
    
    public void testInOnlyWithWrongContent() throws Exception {
        ServiceMixClient client = new ServiceMixClientFacade(component.getComponentContext());
        InOnly exchange = client.createInOnlyExchange();
        fillExchange(exchange);
        exchange.getInMessage().setContent(new StringSource("This is not XML!"));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        assertLog();
        assertTrue(messages.get(0).contains("Unable to display:"));
    }

    public void testInOut() throws Exception {
        ServiceMixClient client = new ServiceMixClientFacade(component.getComponentContext());
        InOut exchange = client.createInOutExchange();
        fillExchange(exchange);
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        client.done(exchange);
        
        assertLog();
    }
    
    private void fillExchange(MessageExchange exchange) throws MessagingException {
        exchange.setService(new QName("logging"));
        exchange.setInterfaceName(new QName("logservice"));
        exchange.setOperation(new QName("log"));
        exchange.setProperty("xml", new StringSource("<an>XML value</an>"));
        exchange.getMessage("in").setContent(new StringSource("<hello>world</hello>"));
        exchange.getMessage("in").setProperty("key", "value");
        exchange.getMessage("in").setProperty("xml", new StringSource("<an>XML value</an>"));
        exchange.getMessage("in").addAttachment("attachment", new DataHandler(new FileDataSource("src/test/resources/attachment.png")));
    }
    
    private void assertLog() {
        assertEquals(1, messages.size());
        String message = messages.get(0);
        assertTrue(message.contains("service: logging"));
        assertTrue(message.contains("endpoint: endpoint"));
        assertTrue(message.contains("interface: logservice"));
        assertTrue(message.contains("operation: log"));
        assertTrue(message.contains("key = value"));
        assertTrue(message.contains("xml = <an>XML value</an>"));
        assertTrue(message.contains("attachments:"));
    }
}
