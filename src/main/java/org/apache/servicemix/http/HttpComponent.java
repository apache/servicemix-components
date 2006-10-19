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
import javax.xml.namespace.QName;

import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.URISupport;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ResolvedEndpoint;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.xbean.BaseXBeanDeployer;
import org.apache.servicemix.http.jetty.JCLLogger;
import org.apache.servicemix.http.jetty.JettyContextManager;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.auth.impl.JAASAuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;
import org.w3c.dom.DocumentFragment;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="component"
 *                  description="An http component"
 */
public class HttpComponent extends DefaultComponent {

    public final static String EPR_URI = "urn:servicemix:http";
    public final static QName EPR_SERVICE = new QName(EPR_URI, "HttpComponent");
    public final static String EPR_NAME = "epr";
    
    static {
        JCLLogger.init();
    }
    
    protected ContextManager server;
    protected HttpClient client;
    protected MultiThreadedHttpConnectionManager connectionManager;
    protected HttpConfiguration configuration = new HttpConfiguration();
    protected HttpEndpoint[] endpoints;
    
    protected String protocol;
    protected String host;
    protected int port = 80;
    protected String path;
    
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
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
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the endpoints
     */
    public HttpEndpoint[] getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoints the endpoints to set
     */
    public void setEndpoints(HttpEndpoint[] endpoints) {
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
    
    public void setClient(HttpClient client) {
        this.client = client;
    }

    /**
     * @return Returns the configuration.
     * @org.apache.xbean.Flat
     */
    public HttpConfiguration getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(HttpConfiguration configuration) {
        this.configuration = configuration;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    protected void doInit() throws Exception {
        super.doInit();
        // Load configuration
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.setComponentName(context.getComponentName());
        configuration.load();
        // Lookup keystoreManager and authenticationService
        if (configuration.getKeystoreManager() == null) {
            try {
                String name = configuration.getKeystoreManagerName();
                Object km =  context.getNamingContext().lookup(name);
                configuration.setKeystoreManager((KeystoreManager) km); 
            } catch (Throwable e) {
                // ignore
            }
        }
        if (configuration.getAuthenticationService() == null) {
            try {
                String name = configuration.getAuthenticationServiceName();
                Object as =  context.getNamingContext().lookup(name);
                configuration.setAuthenticationService((AuthenticationService) as); 
            } catch (Throwable e) {
                configuration.setAuthenticationService(new JAASAuthenticationService());
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
        }
        // Create serverManager
        if (configuration.isManaged()) {
            server = new ManagedContextManager();
        } else {
            JettyContextManager server = new JettyContextManager();
            server.setMBeanServer(context.getMBeanServer());
            this.server = server;
        }
        server.setConfiguration(configuration);
        server.init();
    }

    protected void doShutDown() throws Exception {
        super.doShutDown();
        if (server != null) {
            ContextManager s = server;
            server = null;
            s.shutDown();
        }
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    protected void doStart() throws Exception {
        server.start();
        super.doStart();
    }

    protected void doStop() throws Exception {
        super.doStop();
        server.stop();
    }

    protected QName getEPRServiceName() {
        return EPR_SERVICE;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        HttpEndpoint httpEp = new HttpEndpoint();
        httpEp.setServiceUnit(new ServiceUnit(component));
        httpEp.setService(ep.getServiceName());
        httpEp.setEndpoint(ep.getEndpointName());
        httpEp.setRole(MessageExchange.Role.PROVIDER);
        URI uri = new URI(ep.getEndpointName());
        Map map = URISupport.parseQuery(uri.getQuery());
        if( IntrospectionSupport.setProperties(httpEp, map, "http.") ) {
            uri = URISupport.createRemainingURI(uri, map);
        }
        if (httpEp.getLocationURI() == null) {
            httpEp.setLocationURI(uri.toString());
        }
        httpEp.activateDynamic();
        return httpEp;
    }

    /**
     * @return the keystoreManager
     */
    public KeystoreManager getKeystoreManager() {
        return configuration.getKeystoreManager();
    }

    /**
     * @param keystoreManager the keystoreManager to set
     */
    public void setKeystoreManager(KeystoreManager keystoreManager) {
        this.configuration.setKeystoreManager(keystoreManager);
    }

    /**
     * @return the authenticationService
     */
    public AuthenticationService getAuthenticationService() {
        return configuration.getAuthenticationService();
    }

    /**
     * @param authenticationService the authenticationService to set
     */
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.configuration.setAuthenticationService(authenticationService);
    }

    /**
     * When servicemix-http is embedded inside a web application and configured
     * to reuse the existing servlet container, this method will create and
     * return the HTTPProcessor which will handle all servlet calls
     * @param mappings 
     */
    public HttpProcessor getMainProcessor() {
        return server.getMainProcessor();
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createServiceUnitManager()
     */
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] { new BaseXBeanDeployer(this, getEndpointClasses()), 
                                                new HttpWsdl1Deployer(this) };
        return new BaseServiceUnitManager(this, deployers);
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#resolveEndpointReference(org.w3c.dom.DocumentFragment)
     */
    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        ServiceEndpoint ep = ResolvedEndpoint.resolveEndpoint(epr, EPR_URI, EPR_NAME, EPR_SERVICE, "http:");
        if (ep == null) {
            ep = ResolvedEndpoint.resolveEndpoint(epr, EPR_URI, EPR_NAME, EPR_SERVICE, "https:");
        }
        return ep;
    }

    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }

    protected Class[] getEndpointClasses() {
        return new Class[] { HttpEndpoint.class };
    }
    
}
