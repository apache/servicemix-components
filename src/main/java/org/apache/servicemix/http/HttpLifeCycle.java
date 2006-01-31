/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;

public class HttpLifeCycle extends BaseLifeCycle {

    protected ServerManager server;
    protected HttpClient client;
    protected MultiThreadedHttpConnectionManager connectionManager;
    protected HttpConfiguration configuration;
    
    public HttpLifeCycle(BaseComponent component) {
        super(component);
        configuration = new HttpConfiguration();
    }

    public ServerManager getServer() {
        return server;
    }

    public void setServer(ServerManager server) {
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
     */
    public HttpConfiguration getConfiguration() {
        return configuration;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    protected void doInit() throws Exception {
        super.doInit();
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.load();
        if (server == null) {
            server = new ServerManager();
            server.setConfiguration(configuration);
            server.init();
        }
        if (client == null) {
            connectionManager = new MultiThreadedHttpConnectionManager();
            client = new HttpClient(connectionManager);
        }
    }

    protected void doShutDown() throws Exception {
        super.doShutDown();
        if (server != null) {
            server.shutDown();
        }
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    protected void doStart() throws Exception {
        super.doStart();
        server.start();
    }

    protected void doStop() throws Exception {
        super.doStop();
        server.stop();
    }

}
