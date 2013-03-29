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

import javax.activation.DataHandler;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.servicemix.util.jaf.ByteArrayDataSource;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class ScriptingComponentTest extends SpringTestSupport {
    private static final String TEST_PROPERTY = "JSR-223-TEST-PROPERTY-NAME";
    private static final String PROP_VALUE    = "JSR-223-TEST-PROPERTY-VALUE";

    // TODO need to run JRuby tests first here since upgrading to 1.7.2 for some reason. Was getting
    // NameError: ArrayJavaProxy is already defined
    // (root) at <script>:20
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
        assertNotNull("The out message was null...", me.getOutMessage());
        assertNotNull("The out message content was null...", me.getOutMessage().getContent());
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
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            assertNotNull("The out message content was null...", msg.getContent());
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
        assertNotNull("The out message was null...", me.getOutMessage());
        assertNotNull("The out message content was null...", me.getOutMessage().getContent());
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
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            assertNotNull("The out message content was null...", msg.getContent());
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
        assertNotNull("The out message was null...", me.getOutMessage());
        assertNotNull("The out message content was null...", me.getOutMessage().getContent());
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
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            assertNotNull("The out message content was null...", msg.getContent());
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
    
    public void testJavaScriptInOutAll() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "js-all"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        me.getInMessage().setProperty(TEST_PROPERTY, PROP_VALUE);
        me.getInMessage().addAttachment(TEST_PROPERTY, new DataHandler(new ByteArrayDataSource(PROP_VALUE.getBytes(), "raw")));
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
        assertNotNull("The out message was null...", me.getOutMessage());
        assertNotNull("The out message content was null...", me.getOutMessage().getContent());
        assertNotNull("The attachment was not copied to out message...", me.getOutMessage().getAttachment(TEST_PROPERTY));
        assertNotNull("The property was not copied to out message...", me.getOutMessage().getProperty(TEST_PROPERTY));
        assertEquals("The property was not copied to out message correctly...", PROP_VALUE, me.getOutMessage().getProperty(TEST_PROPERTY));
        
        client.done(me);
    }
    
    public void testJavaScriptInOnlyAll() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Receiver receiver = (Receiver) getBean("receiver");
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "js-all"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        me.getInMessage().setProperty(TEST_PROPERTY, PROP_VALUE);
        me.getInMessage().addAttachment(TEST_PROPERTY, new DataHandler(new ByteArrayDataSource(PROP_VALUE.getBytes(), "raw")));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.DONE) {
            receiver.getMessageList().assertMessagesReceived(1);
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            assertNotNull("The out message content was null...", msg.getContent());
            assertNotNull("The attachment was not copied to out message...", msg.getAttachment(TEST_PROPERTY));
            assertNotNull("The property was not copied to out message...", msg.getProperty(TEST_PROPERTY));
            assertEquals("The property was not copied to out message correctly...", PROP_VALUE, msg.getProperty(TEST_PROPERTY));
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
    
    public void testJavaScriptInOutOnlyHeaders() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "js-headersOnly"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        me.getInMessage().setProperty(TEST_PROPERTY, PROP_VALUE);
        me.getInMessage().addAttachment(TEST_PROPERTY, new DataHandler(new ByteArrayDataSource(PROP_VALUE.getBytes(), "raw")));
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
        assertNotNull("The out message was null...", me.getOutMessage());
        assertNotNull("The out message content was null...", me.getOutMessage().getContent());
        assertNull("The attachment was copied to out message...", me.getOutMessage().getAttachment(TEST_PROPERTY));
        assertNotNull("The property was not copied to out message...", me.getOutMessage().getProperty(TEST_PROPERTY));
        assertEquals("The property was not copied to out message correctly...", PROP_VALUE, me.getOutMessage().getProperty(TEST_PROPERTY));
        
        client.done(me);
    }
    
    public void testJavaScriptInOnlyOnlyHeaders() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Receiver receiver = (Receiver) getBean("receiver");
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "js-headersOnly"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        me.getInMessage().setProperty(TEST_PROPERTY, PROP_VALUE);
        me.getInMessage().addAttachment(TEST_PROPERTY, new DataHandler(new ByteArrayDataSource(PROP_VALUE.getBytes(), "raw")));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.DONE) {
            receiver.getMessageList().assertMessagesReceived(1);
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            assertNotNull("The out message content was null...", msg.getContent());
            assertNull("The attachment was copied to out message...", msg.getAttachment(TEST_PROPERTY));
            assertNotNull("The property was not copied to out message...", msg.getProperty(TEST_PROPERTY));
            assertEquals("The property was not copied to out message correctly...", PROP_VALUE, msg.getProperty(TEST_PROPERTY));
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
    
    public void testJavaScriptInOutOnlyAttachments() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "js-attachmentsOnly"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        me.getInMessage().setProperty(TEST_PROPERTY, PROP_VALUE);
        me.getInMessage().addAttachment(TEST_PROPERTY, new DataHandler(new ByteArrayDataSource(PROP_VALUE.getBytes(), "raw")));
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
        assertNotNull("The out message was null...", me.getOutMessage());
        assertNotNull("The out message content was null...", me.getOutMessage().getContent());
        assertNotNull("The attachment was not copied to out message...", me.getOutMessage().getAttachment(TEST_PROPERTY));
        assertNull("The property was copied to out message...", me.getOutMessage().getProperty(TEST_PROPERTY));
        
        client.done(me);
    }
    
    public void testJavaScriptInOnlyOnlyAttachments() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Receiver receiver = (Receiver) getBean("receiver");
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "js-attachmentsOnly"));
        me.getInMessage().setContent(new StringSource("<hello>jsr-223</hello>"));
        me.getInMessage().setProperty(TEST_PROPERTY, PROP_VALUE);
        me.getInMessage().addAttachment(TEST_PROPERTY, new DataHandler(new ByteArrayDataSource(PROP_VALUE.getBytes(), "raw")));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.DONE) {
            receiver.getMessageList().assertMessagesReceived(1);
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            assertNotNull("The out message content was null...", msg.getContent());
            assertNotNull("The attachment was not copied to out message...", msg.getAttachment(TEST_PROPERTY));
            assertNull("The property was copied to out message...", msg.getProperty(TEST_PROPERTY));
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
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring.xml" }, false);
        context.setValidating(false);
        context.refresh();
        return context;
    }

}
