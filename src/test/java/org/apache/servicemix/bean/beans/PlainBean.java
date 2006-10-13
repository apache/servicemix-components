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
package org.apache.servicemix.bean.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.bean.Property;
import org.apache.servicemix.bean.XPath;
import org.apache.servicemix.bean.Content;

import javax.jbi.messaging.MessageExchange;

/**
 * A simple POJO which uses no annotations
 *
 * @version $Revision: $
 */
public class PlainBean {

    private static final Log log = LogFactory.getLog(PlainBean.class);

    private MessageExchange foo;
    private MessageExchange bar;
    private String propertyParameter;
    private String xpathParameter;
    private Object body;

    public void foo(MessageExchange messageExchange) {
        this.foo = messageExchange;
        log.info("foo() called with exchange: " + messageExchange);
    }

    public void bar(MessageExchange messageExchange) {
        this.bar = messageExchange;
        log.info("bar() called with exchange: " + messageExchange);
    }

    public void methodWithPropertyParameter(@Property(name = "person") String name) {
        this.propertyParameter = name;
        log.info("methodWithPropertyParameter() called with parameter: " + name);
    }


    public void methodWithPropertyParameterAndXPath(@Property(name = "person") String name, @XPath(xpath="/hello/@address") String address) {
        this.propertyParameter = name;
        this.xpathParameter = address;
        log.info("methodWithPropertyParameterAndXPath() called with parameter: " + address);
    }

    public void methodWithPropertyParameterAndContent(@Property(name = "person") String name, @Content Object body) {
        this.propertyParameter = name;
        this.body = body;
        log.info("methodWithPropertyParameterAndContent() called with body: " + body);
    }


    public MessageExchange getFoo() {
        return foo;
    }

    public void setFoo(MessageExchange foo) {
        this.foo = foo;
    }

    public MessageExchange getBar() {
        return bar;
    }

    public void setBar(MessageExchange bar) {
        this.bar = bar;
    }

    public String getPropertyParameter() {
        return propertyParameter;
    }

    public void setPropertyParameter(String propertyParameter) {
        this.propertyParameter = propertyParameter;
    }

    public String getXpathParameter() {
        return xpathParameter;
    }

    public void setXpathParameter(String xpathParameter) {
        this.xpathParameter = xpathParameter;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
