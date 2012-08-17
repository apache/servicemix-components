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

import junit.framework.TestCase;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.http.BindingServlet;
import org.apache.servicemix.components.http.HttpInOutBinding;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;

public class HttpProviderTest extends TestCase {

    private final Logger logger = LoggerFactory.getLogger(HttpProviderTest.class);

    protected JBIContainer container;
    Integer port1 = Integer.parseInt(System.getProperty("http.port1", "61101"));

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
        JettyHttpConnector connector = new JettyHttpConnector("localhost", port1);
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
        component.getServiceUnitManager().init("provider", path.getAbsolutePath());
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
     * The http.wsdl specifies the location URI as localhost:"+port1+". Set a NormalizedMessage property to override this
     * value. Therefore don't start the HttpConnector on "+port1+", rather on another port to prove this functionality works.
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
        JettyHttpConnector connector = new JettyHttpConnector("localhost", 9192);
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
        component.getServiceUnitManager().init("provider", path.getAbsolutePath());
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
        JettyHttpConnector connector = new JettyHttpConnector("localhost", port1);
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
        component.getServiceUnitManager().init("provider", path.getAbsolutePath());
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
        logger.info("Msg Sent [{}]", inputMesage);
        logger.info("Msg Recieved [{}]", reply);

        assertEquals(inputMesage.length(), reply.length());
        assertEquals(inputMesage, reply);

        component.getServiceUnitManager().stop("provider");
        component.getServiceUnitManager().shutDown("provider");
        component.getServiceUnitManager().undeploy("provider", path.getAbsolutePath());

        logger.info("Executed in {} ms", (t1 - t0));

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
            logger.info("Streaming: {}", dt);
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




public class JettyHttpConnector extends HttpInOutBinding {
    private Connector listener = new SocketConnector();

	/**
	 * The maximum number of threads for the Jetty SocketListener. It's set
	 * to 256 by default to match the default value in Jetty.
	 */
	private int maxThreads = 256;

    private Server server;
    private String host;
    private int port;

    /**
     * Constructor
     *
     * @param host
     * @param port
     */
    public JettyHttpConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }


    /**
     * Constructor
     *
     * @param listener
     */
    public JettyHttpConnector(Connector listener) {
        this.listener = listener;
    }

    /**
     * Called when the Component is initialized
     *
     * @param cc
     * @throws javax.jbi.JBIException
     */
    public void init(ComponentContext cc) throws JBIException {
        super.init(cc);
        //should set all ports etc here - from the naming context I guess ?
        if (listener == null) {
            listener = new SocketConnector();
        }
        listener.setHost(host);
        listener.setPort(port);
        server = new Server();
        QueuedThreadPool btp = new QueuedThreadPool();
        btp.setMaxThreads(getMaxThreads());
        server.setThreadPool(btp);
    }

    /**
     * start the Component
     *
     * @throws JBIException
     */
    public void start() throws JBIException {
        server.setConnectors(new Connector[] { listener });
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder holder = new ServletHolder();
        holder.setName("jbiServlet");
        holder.setClassName(BindingServlet.class.getName());
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setServlets(new ServletHolder[]{holder});
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName("jbiServlet");
        mapping.setPathSpec("/*");
        servletHandler.setServletMappings(new ServletMapping[]{mapping});
        context.setServletHandler(servletHandler);
        server.setHandler(context);
        context.setAttribute("binding", this);
        try {
            server.start();
        }
        catch (Exception e) {
            throw new JBIException("Start failed: " + e, e);
        }
    }

    /**
     * stop
     */
    public void stop() throws JBIException {
        try {
            if (server != null) {
                server.stop();
            }
        }
        catch (Exception e) {
            throw new JBIException("Stop failed: " + e, e);
        }
    }

    /**
     * shutdown
     */
    public void shutDown() throws JBIException {
        super.shutDown();
        server = null;
    }


    // Properties
    //-------------------------------------------------------------------------
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}
}
}
