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
package org.apache.servicemix.scripting;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class ScriptingComponentTest extends SpringTestSupport {
    private static transient Log log = LogFactory.getLog(ScriptingComponentTest.class);

    public void testGroovyInOut() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "groovy-jsr223"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        System.err.println(new SourceTransformer().toString(me.getOutMessage().getContent()));
        client.done(me);
    }
    
    public void testGroovyInOnly() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Receiver receiver = (Receiver) getBean("receiver");
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "groovy-jsr223"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.DONE) {
            receiver.getMessageList().assertMessagesReceived(1);
        } else {
            if (me.getStatus() == ExchangeStatus.ERROR) {
                if (me.getError() != null) {
                    throw me.getError();
                } else {
                    fail("Received ERROR status");
                }
            } else if (me.getFault() != null) {
                fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
            }    
        }                
    }

    public void testJRubyInOut() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "jruby-jsr223"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        System.err.println(new SourceTransformer().toString(me.getOutMessage().getContent()));
        client.done(me);
    }
    
    public void testJRubyInOnly() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Receiver receiver = (Receiver) getBean("receiver");
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "jruby-jsr223"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.DONE) {
            receiver.getMessageList().assertMessagesReceived(1);
        } else {
            if (me.getStatus() == ExchangeStatus.ERROR) {
                if (me.getError() != null) {
                    throw me.getError();
                } else {
                    fail("Received ERROR status");
                }
            } else if (me.getFault() != null) {
                fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
            }    
        }                
    }

    public void testJavaScriptInOut() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "js-jsr223"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        System.err.println(new SourceTransformer().toString(me.getOutMessage().getContent()));
        client.done(me);
    }
    
    public void testJavaScriptInOnly() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Receiver receiver = (Receiver) getBean("receiver");
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "js-jsr223"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.DONE) {
            receiver.getMessageList().assertMessagesReceived(1);
        } else {
            if (me.getStatus() == ExchangeStatus.ERROR) {
                if (me.getError() != null) {
                    throw me.getError();
                } else {
                    fail("Received ERROR status");
                }
            } else if (me.getFault() != null) {
                fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
            }    
        }                
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring.xml");
    }

}
