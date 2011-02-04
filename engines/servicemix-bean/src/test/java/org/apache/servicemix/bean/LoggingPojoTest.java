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

import org.apache.servicemix.bean.pojos.LoggingPojo;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.slf4j.Logger;
import org.slf4j.Marker;

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
        pojo.setLog(new MyLogger("my-logger"));
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

    private class MyLogger implements Logger {
        private String name;

        public MyLogger(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void debug(String message) {
            messages.add(message);
        }
        public void debug(Marker marker, String format, Object args1, Object args2) {
            messages.add(marker.toString());
        }
        public void debug(Marker marker, String format, Object args) {
            messages.add(marker.toString());
        }
        public void debug(Marker marker, String format, Object[] args) {
            messages.add(marker.toString());
        }
        public void debug(Marker marker, String format) {
            messages.add(marker.toString());
        }
        public void debug(Marker marker, String format, Throwable cause) {
            messages.add(marker.toString());
        }
        public void debug(String message, Object args1, Object args2) {
            messages.add(message);
        }
        public void debug(String message, Throwable cause) {
            messages.add(message);
        }
        public void debug(String message, Object args) {
            messages.add(message);
        }
        public void debug(String message, Object[] args) {
            messages.add(message);
        }
        public void trace(Marker marker, String format, Object arg1, Object arg2) {
            messages.add(marker.toString());
        }
        public void trace(Marker marker, String format) {
            messages.add(marker.toString());
        }
        public void trace(String message, Object args) {
            messages.add(message);
        }
        public void trace(String message, Throwable cause) {
            messages.add(message);
        }
        public void trace(String message, Object[] args) {
            messages.add(message);
        }
        public void trace(Marker marker, String format, Throwable cause) {
            messages.add(marker.toString());
        }
        public void trace(Marker marker, String format, Object[] args) {
            messages.add(marker.toString());
        }
        public void trace(Marker marker, String format, Object args) {
            messages.add(marker.toString());
        }
        public void trace(String message, Object args1, Object args2) {
            messages.add(message);
        }
        public void trace(String message) {
            messages.add(message);
        }
        public void info(String message, Throwable cause) {
            messages.add(message);
        }
        public void info(Marker marker, String format, Object[] argArray) {
            messages.add(marker.toString());
        }
        public void info(Marker marker, String format, Throwable cause) {
            messages.add(marker.toString());
        }
        public void info(Marker marker, String format, Object args) {
            messages.add(marker.toString());
        }
        public void info(String message, Object[] args) {
            messages.add(message);
        }
        public void info(String message, Object args) {
            messages.add(message);
        }
        public void info(String message, Object args1, Object args2) {
            messages.add(message);
        }
        public void info(Marker marker, String format, Object args1, Object args2) {
            messages.add(marker.toString());
        }
        public void info(Marker marker, String format) {
            messages.add(marker.toString());
        }
        public void info(String message) {
            messages.add(message);
        }
        public void warn(Marker marker, String format, Throwable cause) {
            messages.add(marker.toString());
        }
        public void warn(Marker marker, String format, Object[] argArray) {
            messages.add(marker.toString());
        }
        public void warn(String message, Object args1, Object args2) {
            messages.add(message);
        }
        public void warn(String message, Object args) {
            messages.add(message);
        }
        public void warn(Marker marker, String format, Object args) {
            messages.add(marker.toString());
        }
        public void warn(String message, Throwable cause) {
            messages.add(message);
        }
        public void warn(String message, Object[] args) {
            messages.add(message);
        }
        public void warn(Marker marker, String format, Object args1, Object args2) {
            messages.add(marker.toString());
        }
        public void warn(String message) {
            messages.add(message);
        }
        public void warn(Marker marker, String format) {
            messages.add(marker.toString());
        }
        public void error(String message, Throwable cause) {
            messages.add(message);
        }
        public void error(String message, Object[] args) {
            messages.add(message);
        }
        public void error(String message, Object args) {
            messages.add(message);
        }
        public void error(String message) {
            messages.add(message);
        }
        public void error(String message, Object args1, Object args2) {
            messages.add(message);
        }
        public void error(Marker marker, String format, Throwable cause) {
            messages.add(marker.toString());
        }
        public void error(Marker marker, String format, Object[] args) {
            messages.add(marker.toString());
        }
        public void error(Marker marker, String format, Object args) {
            messages.add(marker.toString());
        }
        public void error(Marker marker, String format) {
            messages.add(marker.toString());
        }
        public void error(Marker marker, String format, Object args1, Object args2) {
            messages.add(marker.toString());
        }
        public boolean isDebugEnabled(Marker marker) {
            return true;
        }
        public boolean isErrorEnabled(Marker marker) {
            return true;
        }
        public boolean isInfoEnabled(Marker marker) {
            return true;
        }
        public boolean isTraceEnabled(Marker marker) {
            return true;
        }
        public boolean isWarnEnabled(Marker marker) {
            return true;
        }
        public boolean isInfoEnabled() {
            return true;
        }
        public boolean isWarnEnabled() {
            return true;
        }
        public boolean isErrorEnabled() {
            return true;
        }
        public boolean isDebugEnabled() {
            return true;
        }
        public boolean isTraceEnabled() {
            return true;
        }
    }
}
