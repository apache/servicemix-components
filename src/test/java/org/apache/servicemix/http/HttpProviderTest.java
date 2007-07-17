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
package org.apache.servicemix.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.http.HttpConnector;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;

public class HttpProviderTest extends TestCase {

    private static transient Log log = LogFactory.getLog(HttpProviderTest.class);

    protected JBIContainer container;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    protected long testInOnly(String msg, boolean streaming) throws Exception {
        // HTTP Component
        HttpComponent component = new HttpComponent();
        component.getConfiguration().setStreamingEnabled(streaming);
        container.activateComponent(component, "HttpProviderTest");

        // Add a receiver component
        Receiver receiver = new ReceiverComponent();
        ActivationSpec asReceiver = new ActivationSpec("receiver", receiver);
        asReceiver.setService(new QName("test", "receiver"));
        container.activateComponent(asReceiver);

        // Add the http receiver
        HttpConnector connector = new HttpConnector("localhost", 8192);
        connector.setDefaultInOut(false);
        ActivationSpec asConnector = new ActivationSpec("connector", connector);
        asConnector.setDestinationService(new QName("test", "receiver"));
        container.activateComponent(asConnector);

        // Start container
        container.start();

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/http.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");

        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        RobustInOnly in = client.createRobustInOnlyExchange();
        in.setInterfaceName(new QName("http://http.servicemix.org/Test", "ProviderInterface"));
        in.getInMessage().setContent(new StreamSource(new ByteArrayInputStream(msg.getBytes())));

        long t0 = System.currentTimeMillis();
        client.sendSync(in);
        long t1 = System.currentTimeMillis();
        assertTrue(in.getStatus() == ExchangeStatus.DONE);

        // Check we received the message
        receiver.getMessageList().assertMessagesReceived(1);

        component.getServiceUnitManager().stop("provider");
        component.getServiceUnitManager().shutDown("provider");
        component.getServiceUnitManager().undeploy("provider", path.getAbsolutePath());

        return t1 - t0;
    }

    /**
     * The http.wsdl specifies the location URI as localhost:8192. Set a NormalizedMessage property to override this
     * value. Therefore don't start the HttpConnector on 8192, rather on another port to prove this functionality works.
     * 
     * @param msg
     * @param streaming
     * @return
     * @throws Exception
     */
    protected long testInOnlyOverrideDestination(String msg, boolean streaming) throws Exception {
        // HTTP Component
        HttpComponent component = new HttpComponent();
        component.getConfiguration().setStreamingEnabled(streaming);
        container.activateComponent(component, "HttpProviderTest");

        // Add a receiver component
        Receiver receiver = new ReceiverComponent();
        ActivationSpec asReceiver = new ActivationSpec("receiver", receiver);
        asReceiver.setService(new QName("test", "receiver"));
        container.activateComponent(asReceiver);

        // Add the http receiver
        HttpConnector connector = new HttpConnector("localhost", 9192);
        connector.setDefaultInOut(false);
        ActivationSpec asConnector = new ActivationSpec("connector", connector);
        asConnector.setDestinationService(new QName("test", "receiver"));
        container.activateComponent(asConnector);

        // Start container
        container.start();

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/http.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");

        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        RobustInOnly in = client.createRobustInOnlyExchange();
        in.setInterfaceName(new QName("http://http.servicemix.org/Test", "ProviderInterface"));
        in.getInMessage().setContent(new StreamSource(new ByteArrayInputStream(msg.getBytes())));
        in.getInMessage().setProperty(JbiConstants.HTTP_DESTINATION_URI, "http://localhost:9192/CheckAvailability");

        long t0 = System.currentTimeMillis();
        client.sendSync(in);
        long t1 = System.currentTimeMillis();
        assertTrue(in.getStatus() == ExchangeStatus.DONE);

        // Check we received the message
        receiver.getMessageList().assertMessagesReceived(1);

        component.getServiceUnitManager().stop("provider");
        component.getServiceUnitManager().shutDown("provider");
        component.getServiceUnitManager().undeploy("provider", path.getAbsolutePath());

        return t1 - t0;
    }

    protected String testInOut(String msg, boolean streaming) throws Exception {
        // HTTP Component
        HttpComponent component = new HttpComponent();
        component.getConfiguration().setStreamingEnabled(streaming);
        container.activateComponent(component, "HTTPComponent");

        // Add a echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asReceiver = new ActivationSpec("echo", echo);
        asReceiver.setService(new QName("test", "echo"));
        container.activateComponent(asReceiver);

        // Add the http receiver
        HttpConnector connector = new HttpConnector("localhost", 8192);
        connector.setDefaultInOut(true);
        ActivationSpec asConnector = new ActivationSpec("connector", connector);
        asConnector.setDestinationService(new QName("test", "echo"));
        container.activateComponent(asConnector);

        // Start container
        container.start();

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/http.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");

        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://http.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StreamSource(new ByteArrayInputStream(msg.getBytes())));

        long t0 = System.currentTimeMillis();
        client.sendSync(inout);
        long t1 = System.currentTimeMillis();
        assertTrue(inout.getStatus() == ExchangeStatus.ACTIVE);

        // Check we received the message
        assertNotNull(inout.getOutMessage());
        assertNotNull(inout.getOutMessage().getContent());
        SourceTransformer sourceTransformer = new SourceTransformer();
        String reply = sourceTransformer.toString(inout.getOutMessage().getContent());
        String inputMesage = sourceTransformer.toString(new StreamSource(new ByteArrayInputStream(msg.getBytes())));
        log.info("Msg Sent [" + inputMesage + "]");
        log.info("Msg Recieved [" + reply + "]");

        assertEquals(inputMesage.length(), reply.length());
        assertEquals(inputMesage, reply);

        component.getServiceUnitManager().stop("provider");
        component.getServiceUnitManager().shutDown("provider");
        component.getServiceUnitManager().undeploy("provider", path.getAbsolutePath());

        log.info("Executed in " + (t1 - t0) + "ms");

        return reply;
    }

    public void testInOnly() throws Exception {
        testInOnly("<hello>world</hello>", false);
    }

    /**
     * JIRA SM-695. Tests the ability of the ProviderProcessor to override the locationURI using the property
     * JbiConstants.HTTP_DESTINATION_URI
     * 
     * @throws Exception
     */
    public void testInOnlyOverrideDestination() throws Exception {
        testInOnlyOverrideDestination("<hello>world</hello>", false);
    }

    public void testInOut() throws Exception {
        testInOut("<hello>world</hello>", true);
    }

    public void testPerfInOnlyWithBigMessage() throws Exception {
        int nbRuns = 10;
        int sizeInKb = 64;

        StringBuffer sb = new StringBuffer();
        sb.append("<hello>\n");
        for (int i = 0; i < sizeInKb; i++) {
            sb.append("\t<hello>");
            for (int j = 0; j < 1024 - 15; j++) {
                sb.append((char) ('A' + (int) (Math.random() * ('Z' - 'A' + 1))));
            }
            sb.append("</hello>\n");
        }
        sb.append("</hello>\n");
        String str = sb.toString();

        /*
         * for(int i = 0; i < nbRuns; i++) { System.gc(); long dt = testInOnly(str, false); System.err.println("No
         * Streaming: " + dt); tearDown(); setUp(); }
         */

        for (int i = 0; i < nbRuns; i++) {
            System.gc();
            long dt = testInOnly(str, true);
            log.info("Streaming: " + dt);
            tearDown();
            setUp();
        }
    }

    public void testInOutWithBigMessage() throws Exception {
        int sizeInKb = 640 * 1024;

        StringBuffer sb = new StringBuffer();
        sb.append("<hello>\n");

        for (int j = 0; j < sizeInKb - 15; j++) {
            sb.append((char) ('A' + (int) (Math.random() * ('Z' - 'A' + 1))));
        }

        sb.append("</hello>\n");
        String str = sb.toString();

        testInOut(str, true);
    }
}
