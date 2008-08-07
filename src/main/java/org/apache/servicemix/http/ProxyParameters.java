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

/**
 * This class contains all parameters needed to send http requests through a proxy
 * 
 * @author Fabrice Dewasmes
 * @org.apache.xbean.XBean description="parameters needed to send HTTP requests through a proxy"
 */
public class ProxyParameters {

    protected String proxyHost;
    protected int proxyPort;
    protected BasicAuthCredentials proxyCredentials;

    /**
     * @return Returns the proxyCredentials.
     */
    public BasicAuthCredentials getProxyCredentials() {
        return this.proxyCredentials;
    }

    /**
	* Sets the authentication data used for the proxy using basic authentication.
	*
	* @param	proxyCredentials	the <code>BasicAuthCredentials</code> that 
	*					will be used for authentication
	 * @org.apache.xbean.Property description="authentication data for using basic HTTP authentication."
	 */
    public void setProxyCredentials(BasicAuthCredentials proxyCredentials) {
        this.proxyCredentials = proxyCredentials;
    }

    /**
     * Proxy Host through which every http call are emitted
     * 
     * @return Returns the proxyHost.
     */
    public String getProxyHost() {
        return this.proxyHost;
    }

    /**
     * @param proxyHost
     *            The proxy host name to set.
	 * @org.apache.xbean.Property description="the proxy's host name"
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * Proxy Port for the proxy host specified
     * 
     * @return Returns the proxyPort.
     */
    public int getProxyPort() {
        return this.proxyPort;
    }

    /**
     * @param proxyPort
     *            The ProxyPort to set.
	 * @org.apache.xbean.Property description="the proxy's port number"
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
}