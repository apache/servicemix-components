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

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.DefaultServiceUnit;
import org.apache.servicemix.common.util.IntrospectionSupport;
import org.apache.servicemix.common.util.URISupport;
import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.servicemix.common.security.KeystoreManager;
import org.apache.servicemix.common.xbean.BaseXBeanDeployer;
import org.apache.servicemix.http.endpoints.HttpConsumerEndpoint;
import org.apache.servicemix.http.endpoints.HttpProviderEndpoint;
import org.apache.servicemix.http.jetty.JCLLogger;
import org.apache.servicemix.http.jetty.JettyContextManager;
import org.mortbay.thread.QueuedThreadPool;

/**
 * an HTTP JBI component
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="component"
 *                         description="an HTTP JBI component. The component is responsible for hosting HTTP endpoints."
 */
public class HttpComponent extends DefaultComponent {

    public static final String[] EPR_PROTOCOLS = {"http:", "https"};

    static {
        JCLLogger.init();
    }

    protected ContextManager server;
    protected HttpClient client;
    protected MultiThreadedHttpConnectionManager connectionManager;
    protected org.mortbay.jetty.client.HttpClient connectionPool;
    protected HttpConfiguration configuration = new HttpConfiguration();
    protected HttpEndpointType[] endpoints;

    protected String protocol;
    protected String host;
    protected int port = 80;
    protected String path;

    /**
     * Returns the host name.
     * 
     * @return a string contianing the host name
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the host name.
     * 
     * @param host a string specifying the host name
     * @org.apache.xbean.Property description="the host name"
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     * @org.apache.xbean.Property description="the port number. The default is 80."
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     * @org.apache.xbean.Property description="the protocol being used. Valid values are <code>http:</code> and <code>https:</code>"
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the endpoints
     */
    public HttpEndpointType[] getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoints the endpoints to set
     * @org.apache.xbean.Property description="the endpoints hosted by a component"
     */
    public void setEndpoints(HttpEndpointType[] endpoints) {
        this.endpoints = endpoints;
    }

    public ContextManager getServer() {
        return server;
    }

    public void setServer(ContextManager server) {
        this.server = server;
    }

    public HttpClient getClient() {
        return client;
    }

    /**
     * @param client the HTTP client instance used by the component
     * @org.apache.xbean.Property description="the Apache Commons HTTP client used by a component"
     */
    public void setClient(HttpClient client) {
        this.client = client;
    }

    public org.mortbay.jetty.client.HttpClient getConnectionPool() {
        return connectionPool;
    }

    public org.mortbay.jetty.client.HttpClient createNewJettyClient() throws Exception {
        org.mortbay.jetty.client.HttpClient tempClient = new org.mortbay.jetty.client.HttpClient();
        QueuedThreadPool btp = new QueuedThreadPool();
        btp.setMaxThreads(getConfiguration().getJettyClientThreadPoolSize());
        tempClient.setThreadPool(btp);
        tempClient.setConnectorType(org.mortbay.jetty.client.HttpClient.CONNECTOR_SELECT_CHANNEL);
        tempClient.setTimeout(getConfiguration().getProviderExpirationTime());
        tempClient.start();
        return tempClient;
    }

    /**
     * Sets the connection pool used by the component. The connection pool is a Jetty HTTP client instance with a thread pool set
     * using the HTTP component's <code>jettyClientThreadPoolSize</code> property.
     * 
     * @param connectionPool a Jetty <code>HttpClient</code>
     * @org.apache.xbean.Property description="a Jetty HTTP client instance maintaining a thread pool for client-side connections"
     */
    public void setConnectionPool(org.mortbay.jetty.client.HttpClient connectionPool) {
        this.connectionPool = connectionPool;
    }

    /**
     * @return Returns the configuration.
     * @org.apache.xbean.Flat
     */
    public HttpConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration an <code>HttpConfiguration</code> object containing the configuration information needed to establish
     *            HTTP connections
     * @org.apache.xbean.Property description="the HTTP configuration information used to establish HTTP connections"
     */
    public void setConfiguration(HttpConfiguration configuration) {
        this.configuration = configuration;
    }

    /*
     * (non-Javadoc)
     * @see org.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    protected void doInit() throws Exception {
        // Load configuration
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.setComponentName(context.getComponentName());
        configuration.load();
        // Lookup keystoreManager and authenticationService
        if (configuration.getKeystoreManager() == null) {
            try {
                String name = configuration.getKeystoreManagerName();
                Object km = context.getNamingContext().lookup(name);
                configuration.setKeystoreManager(km);
            } catch (Throwable e) {
                // ignore
            }
        }
        if (configuration.getAuthenticationService() == null) {
            try {
                String name = configuration.getAuthenticationServiceName();
                Object as = context.getNamingContext().lookup(name);
                configuration.setAuthenticationService(as);
            } catch (Throwable e) {
                try {
                    Class cl = Class
                        .forName("org.apache.servicemix.jbi.security.auth.impl.JAASAuthenticationService");
                    configuration.setAuthenticationService(cl.newInstance());
                } catch (Throwable t) {
                    logger.warn("Unable to retrieve or create the authentication service");
                }
            }
        }
        // Create client
        if (client == null) {
            connectionManager = new MultiThreadedHttpConnectionManager();
            HttpConnectionManagerParams params = new HttpConnectionManagerParams();
            params.setDefaultMaxConnectionsPerHost(configuration.getMaxConnectionsPerHost());
            params.setMaxTotalConnections(configuration.getMaxTotalConnections());
            connectionManager.setParams(params);
            client = new HttpClient(connectionManager);
            client.getParams().setAuthenticationPreemptive(configuration.isPreemptiveAuthentication());

        }
        // Create connectionPool
        if (connectionPool == null) {
            connectionPool = createNewJettyClient();
        }
        // Create serverManager
        if (configuration.isManaged()) {
            server = new ManagedContextManager();
        } else {
            JettyContextManager jcm = new JettyContextManager();
            jcm.setMBeanServer(context.getMBeanServer());
            this.server = jcm;
        }
        server.setConfiguration(configuration);
        server.init();
        server.start();
        // Default initalization
        super.doInit();
    }

    protected void doShutDown() throws Exception {
        super.doShutDown();
        if (server != null) {
            ContextManager s = server;
            server = null;
            s.stop();
            s.shutDown();
        }
        if (connectionPool != null) {
            connectionPool.stop();
            connectionPool = null;
        }
        if (connectionManager != null) {
            connectionManager.shutdown();
            connectionManager = null;
            client = null;
        }
    }

    protected void doStart() throws Exception {
        super.doStart();
    }

    protected void doStop() throws Exception {
        super.doStop();
    }

    protected String[] getEPRProtocols() {
        return EPR_PROTOCOLS;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        HttpEndpoint httpEp = new HttpEndpoint(true);
        httpEp.setServiceUnit(new DefaultServiceUnit(component));
        httpEp.setService(ep.getServiceName());
        httpEp.setEndpoint(ep.getEndpointName());
        httpEp.setRole(MessageExchange.Role.PROVIDER);
        URI uri = new URI(ep.getEndpointName());
        Map map = URISupport.parseQuery(uri.getQuery());
        if (IntrospectionSupport.setProperties(httpEp, map, "http.")) {
            uri = URISupport.createRemainingURI(uri, map);
        }
        if (httpEp.getLocationURI() == null) {
            httpEp.setLocationURI(uri.toString());
        }
        return httpEp;
    }

    /**
     * @return the keystoreManager
     */
    public Object getKeystoreManager() {
        return configuration.getKeystoreManager();
    }

    /**
     * @param keystoreManager the keystoreManager to set
     * @org.apache.xbean.Property description="the keystore manager object used by a component"
     */
    public void setKeystoreManager(Object keystoreManager) {
        this.configuration.setKeystoreManager(keystoreManager);
    }

    /**
     * @return the authenticationService
     */
    public Object getAuthenticationService() {
        return configuration.getAuthenticationService();
    }

    /**
     * @param authenticationService the authenticationService to set
     * @org.apache.xbean.Property description="the authentication service object used by a component"
     */
    public void setAuthenticationService(Object authenticationService) {
        this.configuration.setAuthenticationService(authenticationService);
    }

    /**
     * When servicemix-http is embedded inside a web application and configured to reuse the existing servlet container, this method
     * will create and return the HTTPProcessor which will handle all servlet calls
     */
    public HttpProcessor getMainProcessor() {
        return server.getMainProcessor();
    }

    /*
     * (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createServiceUnitManager()
     */
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] {new BaseXBeanDeployer(this, getEndpointClasses()),
                                               new HttpWsdl1Deployer(this)};
        return new BaseServiceUnitManager(this, deployers);
    }

    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }

    protected Class[] getEndpointClasses() {
        return new Class[] {HttpEndpoint.class, HttpConsumerEndpoint.class, HttpProviderEndpoint.class};
    }

}
