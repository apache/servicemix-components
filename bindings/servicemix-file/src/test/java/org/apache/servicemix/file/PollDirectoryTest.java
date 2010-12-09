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
package org.apache.servicemix.file;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.jbi.event.ExchangeEvent;
import org.apache.servicemix.jbi.event.ExchangeListener;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.servicemix.util.FileUtil;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class PollDirectoryTest extends SpringTestSupport {

    private static final int NUMBER = 10;
    private static final SourceTransformer TRANSFORMER = new SourceTransformer();
    
    protected void setUp() throws Exception {
	    FileUtil.deleteFile(new File("target/archive"));
	    FileUtil.deleteFile(new File("target/pollerFiles"));
	    FileUtil.deleteFile(new File("target/pollerFiles2"));
	    FileUtil.deleteFile(new File("target/pollerFiles3"));
	    super.setUp();
    }

    public void testSendToWriterSoItCanBePolled() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        // lets send a request to be written to a file
        // which should then be polled
        for (int i = 0; i < NUMBER; i++) {
            InOnly me = client.createInOnlyExchange();
            me.setService(new QName("urn:test", "service"));
            NormalizedMessage message = me.getInMessage();
            message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test" + i + ".xml");
            message.setContent(new StringSource("<hello>world</hello>"));
            client.sendSync(me);
        }

        Receiver receiver = (Receiver) getBean("receiver");
        receiver.getMessageList().waitForMessagesToArrive(NUMBER, 10000);
        receiver.getMessageList().assertMessagesReceived(NUMBER);
    }
    
    public void testArchive() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        // lets send a request to be written to a file
        // which should then be polled
        for (int i = 0; i < NUMBER; i++) {
            InOnly me = client.createInOnlyExchange();
            me.setService(new QName("urn:test", "service2"));
            NormalizedMessage message = me.getInMessage();
            message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test.xml");
            message.setContent(new StringSource("<hello>world</hello>"));
            client.sendSync(me);
            try {
            	Thread.sleep(2000); // wait for file move / delete
            } catch (InterruptedException e) {
            	// ignore it
            }
        }

        Receiver receiver = (Receiver) getBean("receiver2");
        receiver.getMessageList().assertMessagesReceived(NUMBER);
        
        File dir = new File("./target/archive");
        assertEquals("The archive is not created...", true, dir.exists() && dir.isDirectory());
        int numFiles = dir.listFiles().length;
        assertEquals("There should be " + NUMBER + " files in archive but only " + numFiles + " files are found..." , NUMBER, numFiles);
        dir = new File("target/pollerFiles2");
        assertEquals("The archive is not created...", true, dir.exists() && dir.isDirectory());
        numFiles = dir.listFiles().length;
        assertEquals("There shouldn't be any files in the poll folder...but I found " + numFiles + " files there..." , 0, numFiles);
    }

    /*
     * Test to ensure that setting a comparator on the endpoints re-orders the exchanges being sent
     */
    public void testComparator() throws Exception {
        // adding a listener to register the order in which exchanges are being sent
        final List<String> sent = new ArrayList<String>();
        jbi.addListener(new ExchangeListener() {

            public void exchangeSent(ExchangeEvent exchangeEvent) {
                MessageExchange exchange = exchangeEvent.getExchange();
                if (exchange.getStatus() == ExchangeStatus.ACTIVE && "receiver3".equals(exchange.getService().getLocalPart())) {
                    sent.add((String) exchange.getMessage("in").getProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY));
                }
            }

            public void exchangeAccepted(ExchangeEvent exchangeEvent) {
                // we're not interested in this one
            }
        });

        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        
        // lets send a request to be written to a file
        // which should then be polled
        for (int i = 0; i < 2; i++) {
            InOnly me = client.createInOnlyExchange();
            me.setService(new QName("urn:test", "service3"));
            NormalizedMessage message = me.getInMessage();
            if (i == 0) {
                message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "btest.xml");
                message.setContent(new StringSource("<second>file</second>"));
            } else {
                message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "atest.xml");
                message.setContent(new StringSource("<first>file</first>"));
            }
            client.sendSync(me);
        }
        
        Receiver receiver = (Receiver) getBean("receiver3");
        receiver.getMessageList().waitForMessagesToArrive(2, 10000);
        receiver.getMessageList().assertMessagesReceived(2);
        assertEquals("atest.xml should have been sent before btest.xml",
                     "atest.xml", sent.get(0));
        assertEquals("atest.xml should have been sent before btest.xml",
                     "btest.xml", sent.get(1));
    }

    // Testing the "append=false" 
    public void testSendToWriterNotAppend() throws Exception {
        FileUtil.deleteFile(new File("target/pollerFilesNotAppend"));
     
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        // lets send a request to be written to a file
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "serviceNotAppend"));
        NormalizedMessage message = me.getInMessage();
        message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test.xml");
        message.setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(me);

        // send a second request so that it should overwrite the first 
        // in the test.xml file
        InOnly me2 = client.createInOnlyExchange();
        me2.setService(new QName("urn:test", "serviceNotAppend"));
        NormalizedMessage message2 = me2.getInMessage();
        message2.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test.xml");
        message2.setContent(new StringSource("<Goodbye>world</Goodbye>"));
        client.sendSync(me2);            

        File resultFile = new File("target/pollerFilesNotAppend/test.xml");
        String content = getFileContent(resultFile);
        assertEquals(content.indexOf("hello"), -1);
    }
        
    // Testing the "append=true" 
    public void testSendToWriterAppend() throws Exception {
        FileUtil.deleteFile(new File("target/pollerFilesAppend"));
     
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        // lets send a request to be written to a file        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "serviceAppend"));
        NormalizedMessage message = me.getInMessage();
        message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test.xml");
        message.setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(me);

        // send a second request so that it append to the first 
        // request in the test.xml file
        InOnly me2 = client.createInOnlyExchange();
        me2.setService(new QName("urn:test", "serviceAppend"));
        NormalizedMessage message2 = me2.getInMessage();
        message2.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test.xml");
        message2.setContent(new StringSource("<Goodbye>world</Goodbye>"));
        client.sendSync(me2);            

        File resultFile = new File("target/pollerFilesAppend/test.xml");       
        String content = getFileContent(resultFile);
        assertTrue(content.indexOf("hello") != 0);
    }    
   
    protected void assertExchangeWorked(MessageExchange me) throws Exception {
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + TRANSFORMER.toString(me.getFault().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring-polling.xml" }, false);
        context.setValidating(false);
        context.refresh();
        return context;
    }

    private String getFileContent(File file) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
        String str = "";

        try {
            fis = new FileInputStream(file);
            // Here BufferedInputStream is added for fast reading.
            bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);

            // dis.available() returns 0 if the file does not have more lines.
            while (dis.available() != 0) {
                // this statement reads the line from the file and print it to
                // the console.
                str = str.concat(dis.readLine());
            }  
            // dispose all the resources after using them.
            fis.close();
            bis.close();
            dis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }            

}
