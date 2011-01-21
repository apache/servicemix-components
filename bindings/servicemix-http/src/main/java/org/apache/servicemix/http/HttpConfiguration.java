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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * Class to hold the configuration for the Jetty instance used by an HTTP
 * endpoint.
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="configuration"
 *                         description="configuration for the Jetty instance used by an HTTP endpoint"
 */
public class HttpConfiguration implements HttpConfigurationMBean {

    public static final String DEFAULT_JETTY_CONNECTOR_CLASS_NAME = SelectChannelConnector.class.getName();
    public static final String MAPPING_DEFAULT = "/jbi";
    public static final String CONFIG_FILE = "component.properties";

    private String rootDir;
    private String componentName = "servicemix-http";
    private Properties properties = new Properties();
    private boolean streamingEnabled;
    private String jettyConnectorClassName = DEFAULT_JETTY_CONNECTOR_CLASS_NAME;
    private transient Object keystoreManager;
    private transient Object authenticationService;

    /**
     * The JNDI name of the AuthenticationService object
     */
    private String authenticationServiceName = "java:comp/env/smx/AuthenticationService";

    /**
     * The JNDI name of the KeystoreManager object
     */
    private String keystoreManagerName = "java:comp/env/smx/KeystoreManager";

    /**
     * The maximum number of threads for the Jetty thread pool. It's set to 255
     * by default to match the default value in Jetty.
     */
    private int jettyThreadPoolSize = 255;

    /**
     * The maximum number of threads for the jetty client thread pool. It's set
     * to 16 to match the default value in Jetty.
     */
    private int jettyClientThreadPoolSize = 16;

    /**
     * Configuration to switch from shared jetty client for all
     * HttpProviderEndpoints to jetty client per HttpProviderEndpoint. It's
     * default value is false.
     */
    private boolean jettyClientPerProvider;

    /**
     * Maximum number of concurrent requests to the same host.
     */
    private int maxConnectionsPerHost = 65536;

    /**
     * Maximum number of concurrent requests.
     */
    private int maxTotalConnections = 65536;

    /**
     * If true, use register jetty mbeans
     */
    private boolean jettyManagement;

    /**
     * If the component is deployed in a web container and uses a servlet
     * instead of starting its own web server.
     */
    private boolean managed;

    /**
     * When managed is true, this is the servlet mapping used to access the
     * component.
     */
    private transient String mapping = MAPPING_DEFAULT;

    /**
     * Jetty connector max idle time (default value in jetty is 30000msec)
     **/
    private int connectorMaxIdleTime = 30000;
    
    /*
     * Jetty connector soLingerTime in ms(default value in jetty is -1 which means disable it)
     */
    private int soLingerTime = -1;

    /**
     * HttpConsumerProcessor continuation suspend time (default value in
     * servicemix is 60000msec)
     */
    private int consumerProcessorSuspendTime = 60000;
  	
    /***
     * HttpProvider endpoint expiration time.
     */
    private int providerExpirationTime = 300000;

    /**
     * Number of times a given HTTP request will be tried until successful. If
     * streaming is enabled, the value will always be 0.
     */
    private int retryCount = 3;

    /**
     * Proxy hostname. Component wide configuration, used either for http or
     * https connections. Can be overriden on a endpoint basis.
     */
    private String proxyHost;

    /**
     * Proxy listening port. Component wide configuration, used either for http
     * or https connections. Can be overriden on a endpoint basis.
     */
    private int proxyPort;

    /**
     * This field is used to decide if the http provider processor can copy the
     * http headers from the http response into the exchange as property. Be
     * careful if the headers will be used for a new http reuquest, it leads to
     * an error.
     */
    private boolean wantHeadersFromHttpIntoExchange;

    /**
   * This field is used to decide if the http provider processor http client should use preemptive authentication
   * which avoids in case of true the double sending of requests.
   */
    private boolean preemptiveAuthentication;

    private boolean useHostPortForAuthScope;

    /**
     * @return Returns the rootDir.
     * @org.apache.xbean.Property hidden="true"
     */
    public String getRootDir() {
        return rootDir;
    }

    /**
     * @param rootDir The rootDir to set.
     */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    /**
     * @return Returns the componentName.
     * @org.apache.xbean.Property hidden="true"
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * @param componentName The componentName to set.
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * @return the mapping
     */
    public String getMapping() {
        return mapping;
    }

    /**
     * @param mapping the mapping to set
     */
    public void setMapping(String mapping) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }
        if (mapping.endsWith("/")) {
            mapping = mapping.substring(0, mapping.length() - 1);
        }
        this.mapping = mapping;
    }

    /**
     * @return the managed
     */
    public boolean isManaged() {
        return managed;
    }

    /**
     * @param managed the managed to set
     */
    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    /**
     * @return the jettyManagement
     */
    public boolean isJettyManagement() {
        return jettyManagement;
    }

    /**
     * @param jettyManagement the jettyManagement to set
     */
    public void setJettyManagement(boolean jettyManagement) {
        this.jettyManagement = jettyManagement;
    }

    /**
     * Gets the authentication service being used.
     * 
     * @return the authenticationService object
     */
    public Object getAuthenticationService() {
        return authenticationService;
    }

    /**
     * Directly sets the authenitcation service object to be used for
     * authentication. This object takes precedence over the JNDI name specified
     * by <code>setAuthenticationServiceName</code>.
     * 
     * @param authenticationService the authenticationService object
     * @org.apache.xbean.Property description=
     *                            "the authentication service object. This property takes precedence over
     *                            <code>authenticationServiceName</code>."
     */
    public void setAuthenticationService(Object authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Gets the JNDI name of the authentication service object.
     * 
     * @return a string representing the JNDI name for the authentication
     *         service object.
     */
    public String getAuthenticationServiceName() {
        return authenticationServiceName;
    }

    /**
     * Sets the JNDI name of the authentication service object.
     * 
     * @param authenticationServiceName a string representing the JNDI name for
     *            the authentication service object.
     * @org.apache.xbean.Property description="the JNDI name of the authentication service object. The default is java:comp/env/smx/AuthenticationService."
     */
    public void setAuthenticationServiceName(String authenticationServiceName) {
        this.authenticationServiceName = authenticationServiceName;
        save();
    }

    /**
     * Gets the object used as the keystore manager.
     * 
     * @return the keystoreManager object
     */
    public Object getKeystoreManager() {
        return keystoreManager;
    }

    /**
     * Directly sets the keystore manager object to be used for authentication.
     * This object takes precedence over the JNDI name specified by
     * <code>setKeystoreManagerName</code>.
     * 
     * @param keystoreManager the keystoreManager object
     * @org.apache.xbean.Property 
     *                            description="the keystore object. This property takes precedence over
     *                            <code>keystoreManagerName</code>."
     */
    public void setKeystoreManager(Object keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    /**
     * Gets the JNDI name of the keystore manager object.
     * 
     * @return a string representing the JNDI name for the keystore manager
     *         object.
     */
    public String getKeystoreManagerName() {
        return keystoreManagerName;
    }

    /**
     * Sets the JNDI name of the keystore manager object.
     * 
     * @param keystoreManagerName a string representing the JNDI name for the
     *            keystore manager object.
     * @org.apache.xbean.Property description="the JNDI name of the keystore manager object. The default is java:comp/env/smx/KeystoreManager."
     */
    public void setKeystoreManagerName(String keystoreManagerName) {
        this.keystoreManagerName = keystoreManagerName;
        save();
    }

    /**
     * Determines if client-side requests use HTTP streaming.
     * 
     * @return true if client-side requests use streaming
     */
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    /**
     * Sets whether or not client-side requests use HTTP streaming.
     * 
     * @param streamingEnabled Set to true to enable client-side HTTP streaming.
     * @org.apache.xbean.Property 
     *                            description="Specifies if client-side requests use HTTP streaming."
     */
    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
        save();
    }

    /**
     * Returns the name of the class implementing the Jetty connector used by an
     * HTTP endpoint.
     * 
     * @return a string representing the classname of the Jetty conector being
     *         used
     */
    public String getJettyConnectorClassName() {
        return jettyConnectorClassName;
    }

    /**
     * Sets the classname of the Jetty connector used by an HTTP endpoint.
     * 
     * @param jettyConnectorClassName a String representing the classname of the
     *            Jetty connector to use.
     * @org.apache.xbean.Property 
     *                            description="the classname of the Jetty connector used by the endpoint"
     */
    public void setJettyConnectorClassName(String jettyConnectorClassName) {
        this.jettyConnectorClassName = jettyConnectorClassName;
        save();
    }

    /**
     * Gets the number of maximum number of threads in the server-side
     * threadpool.
     * 
     * @return an integer representing the maximum number of threads in the
     *         server-side threadpool
     */
    public int getJettyThreadPoolSize() {
        return jettyThreadPoolSize;
    }

    /**
     * Sets the maximum number of threads in the server-side thread pool. The
     * default is 255 to match Jetty's default setting.
     * 
     * @param jettyThreadPoolSize an integer representing the maximum number of
     *            threads in the server-side thread pool
     * @org.apache.xbean.Property description="the maximum number of threads in the server-side threadpool. The default setting is 255."
     */
    public void setJettyThreadPoolSize(int jettyThreadPoolSize) {
        this.jettyThreadPoolSize = jettyThreadPoolSize;
        save();
    }

    /**
     * Get the maximum number of threads in the client-side thread pool.
     * 
     * @return an int representing the maximum number of threads in the
     *         client-side thread pool.
     */
    public int getJettyClientThreadPoolSize() {
        return jettyClientThreadPoolSize;
    }

    /**
     * Sets the maximum number of threads in the client-side thread pool.
     * 
     * @param jettyClientThreadPoolSize an int specifiying the maximum number of
     *            threads available in the client-side thread pool
     * @org.apache.xbean.Property description="the maximum number of threads in the client-side threadpool. The default setting is 16."
     */
    public void setJettyClientThreadPoolSize(int jettyClientThreadPoolSize) {
        this.jettyClientThreadPoolSize = jettyClientThreadPoolSize;
        save();
    }

    /**
     * Determines if each HTTP provider endpoint uses its own Jetty client. The
     * default is for all HTTP provider endpoints to use a shared Jetty client.
     * 
     * @return true if HTTP providers use individual Jetty clients
     */
    public boolean isJettyClientPerProvider() {
        return jettyClientPerProvider;
    }

    /**
     * Specifies whether each HTTP provider endpoint uses its own Jetty client.
     * The default behavior is that all HTTP provider endpoints use a shrared
     * Jetty client.
     * 
     * @param jettyClientProvider <code>true</code> if HTTP providers are to use
     *            individual Jetty clients
     * @org.apache.xbean.Property description="Specifies if HTTP provider endpoints share a Jetty client or use per-endpoint Jetty clients. The default setting is
     *                            <code>false</code> meaning that all provider
     *                            endpoints use a shared Jetty client."
     */
    public void setJettyClientPerProvider(boolean jettyClientPerProvider) {
        this.jettyClientPerProvider = jettyClientPerProvider;
        save();
    }

    /**
     * Gets the maximum number of concurent requests allowed from a particular
     * host.
     * 
     * @return an int representing the maximum number of connections
     */
    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    /**
     * Sets the maximum number of connections allowed from a particular host.
     * The default is 65536.
     * 
     * @param maxConnectionsPerHost an int specifying the max number of
     *            connecitons
     * @org.apache.xbean.Property description="the maximum number of concurent connections allowed from a host. The default is 65536."
     */
    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        save();
    }

    /**
     * Gets the maximum number of concurent connections allowed to an endpoint.
     * 
     * @return an int representing the total number of allowed concurrent
     *         connections
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    /**
     * Sets the maximum number of total concurrent connections allowed to an
     * endpoint.
     * 
     * @param maxTotalConnections an int specifying the total number of
     *            concurrent connections allowed to an endpoint
     * @org.apache.xbean.Property description="the maximum number of total concurent connections allowed to an endpoint. The default is 65536."
     */
    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
        save();
    }

    /**
     * Gets the amount of time, in milliseconds, that a connection will sit idle
     * before timing out.
     * 
     * @return an int representing the number of milliseconds before an idle
     *         connection will timeout
     */
    public int getConnectorMaxIdleTime() {
        return connectorMaxIdleTime;
    }

    /**
     * Sets the amount of time, in milliseconds, that a connection will sit idle
     * before timing out. The default is 30000.
     * 
     * @param connectorMaxIdleTime an int specifying the number of milliseconds
     *            that a connection will sit idle before timing out
     * @org.apache.xbean.Property description="the number of miliseconds a connection will be idle before timing out. The default is 30000."
     */
    public void setConnectorMaxIdleTime(int connectorMaxIdleTime) {
        this.connectorMaxIdleTime = connectorMaxIdleTime;
        save();
    }

    
    /**
     * Gets the amount of connector soLingerTime, in milliseconds.
     * 
     * @return an int representing the connector soLingerTime in milliseconds
     */
    public int getSoLingerTime() {
        return this.soLingerTime;
    }
    
    
    /**
     * Sets the amount of connector soLinger time, in milliseconds.
     * The default is -1 which means disable it.
     * 
     * @param soLingerTime an int specifying the connector soLingerTime in milliseconds
     * @org.apache.xbean.Property description="the connector soLingerTime in milliseconds. The default is -1."
     */
    public void setSoLingerTime(int soLingerTime) {
        this.soLingerTime = soLingerTime;
        save();
    }

    /**
     * Gets the number of milliseconds passed to the <code>susspend</code>
     * method of the Jetty <code>Continuation</code> object used to process
     * requests.
     * 
     * @return an int representing the number of milliseconds the Jetty
     *         <code>Continuation</code> object will susspend the processing of
     *         the current request
     */
    public int getConsumerProcessorSuspendTime() {
        return consumerProcessorSuspendTime;
    }

    /**
     * Sets the number of milliseconds passed to the <code>susspend</code>
     * method of the Jetty <code>Continuation</code> object used to process
     * requests. The <code>Continuation</code> object is used to optimize
     * connection resources when the endpoint is under heavy load. The default
     * is 60000.
     * 
     * @param consumerProcessorSuspendTime an int representing the number of
     *            milliseconds the Jetty <code>Continuation</code> object will
     *            susspend the processing of the current request
     * @org.apache.xbean.Property description="the number of miliseconds Jetty will susspend the processing of a request. The default is 60000."
     */
    public void setConsumerProcessorSuspendTime(int consumerProcessorSuspendTime) {
        this.consumerProcessorSuspendTime = consumerProcessorSuspendTime;
        save();
    }


    /***
     * Gets the number of milliseconds that the provider will wait for a response before expiring.
     * @return an int representing the ammout of time the provider will wait for a response before expiring.
     */
    public int getProviderExpirationTime() {
        return providerExpirationTime;
    }

    /**
     * Sets the number of milliseconds the provider will wait for a response (read timeout).
     * The default default value for Jetty is 300000.
     *
     * @param providerExpirationTime an int representing the number of milliseconds the Jetty will wait for a response.
     * @org.apache.xbean.Property description="the number of miliseconds Jetty will susspend the processing of a request. The default is 60000."
     */
    public void setProviderExpirationTime(int providerExpirationTime) {
        this.providerExpirationTime = providerExpirationTime;
        save();
    }



    /**
     * Gets the number of times a request will be tried before an error is
     * created.
     * 
     * @return an int representing the number of times a request will be
     *         attempted before an error is raised
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Sets the number of times a request will be tried before an error is
     * created. The default is 3. If streaming is enabled, the value will always
     * be 0.
     * 
     * @param retryCount an int representing the number of times a request will
     *            be attempted before an error is raised
     * @org.apache.xbean.Property description="the number of times a request will be attempted without succees before an error is created. The default is 3. If streaming is enabled, the value will always be 0."
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        save();
    }

    /**
     * @return Returns the proxyHost.
     */
    public String getProxyHost() {
        return this.proxyHost;
    }

    /**
     * @param proxyHost The proxyHost to set.
     * @org.apache.xbean.Property description="the default proxy host name used to send requests. This can be overridden by each endpoint."
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        save();
    }

    /**
     * @return Returns the proxyPort.
     */
    public int getProxyPort() {
        return this.proxyPort;
    }

    /**
     * @param proxyPort The proxyPort to set.
     * @org.apache.xbean.Property description="the default proxy port used to send requests. This can be overridden by each endpoint."
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        save();
    }

    /**
     * Determines if the HTTP provider processor copies the HTTP headers from
     * the HTTP response into the JBI exchange.
     * 
     * @return <code>true</code> if the HTTP headers will be copied into the
     *         exchange
     */
    public boolean isWantHeadersFromHttpIntoExchange() {
        return wantHeadersFromHttpIntoExchange;
    }

    /**
     * Specifies if the HTTP provider processor copies the HTTP headers from the
     * HTTP response into the JBI exchange. If the headers will be used for a
     * new HTTP reuquest, setting this to <code>true</code> leads to an error.
     * 
     * @param wantHeadersFromHttpIntoExchange <code>true</code> if the HTTP
     *            headers will be copied into the exchange
     * @org.apache.xbean.Property description="Specifies if the HTTP provider will copy the HTTP request headers into the JBI exchange. The default is
     *                            <code>false</code>."
     */
    public void setWantHeadersFromHttpIntoExchange(boolean wantHeadersFromHttpIntoExchange) {
        this.wantHeadersFromHttpIntoExchange = wantHeadersFromHttpIntoExchange;
    }

    /**
     *
     * @return true if preemptive auth is used in the http client
     */
    public boolean isPreemptiveAuthentication() {
        return preemptiveAuthentication;
    }

    /**
     * Specifies of the httpclient uses preemptive authentication which can save performance. The default is false.
     * If enabled it always send credentials also if it is not needed. 
     * @param preemptiveAuthentication the value which strategy should be used
     *
     * @org.apache.xbean.Property description="Specifies of the httpclient uses preemptive authentication which can save performance. The default is false"
     */
    public void setPreemptiveAuthentication(boolean preemptiveAuthentication) {
        this.preemptiveAuthentication = preemptiveAuthentication;
        save();
    }

    /**
     *
     * @return true if AuthScope of httpclient depeneds on host and port
     */
    public boolean isUseHostPortForAuthScope() {
        return useHostPortForAuthScope;
    }

    /**
     *
     * @param useHostPortForAuthScope If true the AuthScope of the httpclient is bind to a special host and port from the url. The default is false
     *
     * @org.apache.xbean.Property description="If true the AuthScope of the httpclient is bind to a special host and port from the url. The default is false"
     */
    public void setUseHostPortForAuthScope(boolean useHostPortForAuthScope) {
        this.useHostPortForAuthScope = useHostPortForAuthScope;
        save();
    }

    public void save() {
        setProperty(componentName + ".jettyThreadPoolSize", Integer.toString(jettyThreadPoolSize));
        setProperty(componentName + ".jettyClientThreadPoolSize", Integer.toString(jettyClientThreadPoolSize));
        setProperty(componentName + ".jettyClientPerProvider", Boolean.toString(jettyClientPerProvider));
        setProperty(componentName + ".jettyConnectorClassName", jettyConnectorClassName);
        setProperty(componentName + ".streamingEnabled", Boolean.toString(streamingEnabled));
        setProperty(componentName + ".maxConnectionsPerHost", Integer.toString(maxConnectionsPerHost));
        setProperty(componentName + ".maxTotalConnections", Integer.toString(maxTotalConnections));
        setProperty(componentName + ".keystoreManagerName", keystoreManagerName);
        setProperty(componentName + ".authenticationServiceName", authenticationServiceName);
        setProperty(componentName + ".jettyManagement", Boolean.toString(jettyManagement));
        setProperty(componentName + ".connectorMaxIdleTime", Integer.toString(connectorMaxIdleTime));
        setProperty(componentName + ".soLingerTime", Integer.toString(soLingerTime));
        setProperty(componentName + ".consumerProcessorSuspendTime", Integer
            .toString(consumerProcessorSuspendTime));
        setProperty(componentName + ".providerExpirationTime", Integer.toString(providerExpirationTime));
        setProperty(componentName + ".retryCount", Integer.toString(retryCount));
        setProperty(componentName + ".proxyHost", proxyHost);
        setProperty(componentName + ".proxyPort", Integer.toString(proxyPort));
        setProperty(componentName + ".wantHeadersFromHttpIntoExchange", Boolean
            .toString(wantHeadersFromHttpIntoExchange));
        setProperty(componentName + ".preemptiveAuthentication", Boolean.toString(preemptiveAuthentication));
        setProperty(componentName + ".useHostPortForAuthScope", Boolean.toString(useHostPortForAuthScope));
        if (rootDir != null) {
            File f = new File(rootDir, CONFIG_FILE);
            try {
                this.properties.store(new FileOutputStream(f), null);
            } catch (Exception e) {
                throw new RuntimeException("Could not store component configuration", e);
            }
        }
    }

    protected void setProperty(String name, String value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.setProperty(name, value);
        }
    }

    public boolean load() {
        File f = null;
        InputStream in = null;
        if (rootDir != null) {
            // try to find the property file in the workspace folder
            f = new File(rootDir, CONFIG_FILE);
            if (!f.exists()) {
                f = null;
            }
        }
        if (f == null) {
            // find property file in classpath if it is not available in
            // workspace
            in = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (in == null) {
                return false;
            }
        }

        try {
            if (f != null) {
                properties.load(new FileInputStream(f));
            } else {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load component configuration", e);
        }
        if (properties.getProperty(componentName + ".jettyThreadPoolSize") != null) {
            jettyThreadPoolSize = Integer.parseInt(properties.getProperty(componentName
                                                                          + ".jettyThreadPoolSize"));
        }
        if (properties.getProperty(componentName + ".jettyClientThreadPoolSize") != null) {
            jettyClientThreadPoolSize = Integer.parseInt(properties
                .getProperty(componentName + ".jettyClientThreadPoolSize"));
        }
        if (properties.getProperty(componentName + ".jettyClientPerProvider") != null) {
            jettyClientPerProvider = Boolean.valueOf(properties.getProperty(componentName + ".jettyClientPerProvider"))
              .booleanValue();
        }
        if (properties.getProperty(componentName + ".jettyConnectorClassName") != null) {
            jettyConnectorClassName = properties.getProperty(componentName + ".jettyConnectorClassName");
        }
        if (properties.getProperty(componentName + ".streamingEnabled") != null) {
            streamingEnabled = Boolean.valueOf(properties.getProperty(componentName + ".streamingEnabled"))
                .booleanValue();
        }
        if (properties.getProperty(componentName + ".maxConnectionsPerHost") != null) {
            maxConnectionsPerHost = Integer.parseInt(properties.getProperty(componentName
                                                                            + ".maxConnectionsPerHost"));
        }
        if (properties.getProperty(componentName + ".maxTotalConnections") != null) {
            maxTotalConnections = Integer.parseInt(properties.getProperty(componentName
                                                                          + ".maxTotalConnections"));
        }
        if (properties.getProperty(componentName + ".keystoreManagerName") != null) {
            keystoreManagerName = properties.getProperty(componentName + ".keystoreManagerName");
        }
        if (properties.getProperty(componentName + ".authenticationServiceName") != null) {
            authenticationServiceName = properties.getProperty(componentName + ".authenticationServiceName");
        }
        if (properties.getProperty(componentName + ".jettyManagement") != null) {
            jettyManagement = Boolean.valueOf(properties.getProperty(componentName + ".jettyManagement"))
                .booleanValue();
        }
        if (properties.getProperty(componentName + ".connectorMaxIdleTime") != null) {
            connectorMaxIdleTime = Integer.parseInt(properties.getProperty(componentName
                                                                           + ".connectorMaxIdleTime"));
        }
        if (properties.getProperty(componentName + ".soLingerTime") != null) {
            soLingerTime = Integer.parseInt(properties.getProperty(componentName
                                                                           + ".soLingerTime"));
        }
        if (properties.getProperty(componentName + ".consumerProcessorSuspendTime") != null) {
            consumerProcessorSuspendTime = Integer.parseInt(properties
                .getProperty(componentName + ".consumerProcessorSuspendTime"));
        }
        if (properties.getProperty(componentName + ".providerExpirationTime") != null) {
            providerExpirationTime = Integer.parseInt(properties
                .getProperty(componentName + ".providerExpirationTime"));
        }
        if (properties.getProperty(componentName + ".retryCount") != null) {
            retryCount = Integer.parseInt(properties.getProperty(componentName + ".retryCount"));
        }
        if (properties.getProperty(componentName + ".proxyHost") != null) {
            proxyHost = properties.getProperty(componentName + ".proxyHost");
        }
        if (properties.getProperty(componentName + ".proxyPort") != null) {
            proxyPort = Integer.parseInt(properties.getProperty(componentName + ".proxyPort"));
        }
        if (properties.getProperty(componentName + ".wantHeadersFromHttpIntoExchange") != null) {
            wantHeadersFromHttpIntoExchange = Boolean
                .valueOf(properties.getProperty(componentName + ".wantHeadersFromHttpIntoExchange"))
                .booleanValue();
        }
        if (properties.getProperty(componentName + ".preemptiveAuthentication") != null) {
            preemptiveAuthentication = Boolean.valueOf(properties.getProperty(componentName + ".preemptiveAuthentication")).booleanValue();
        }

        if (properties.getProperty(componentName + ".useHostPortForAuthScope") != null) {
            useHostPortForAuthScope = Boolean.valueOf(properties.getProperty(componentName + ".useHostPortForAuthScope")).booleanValue();
        }
        return true;
    }

}
