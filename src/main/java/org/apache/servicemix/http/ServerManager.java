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

import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.thread.BoundedThreadPool;

public class ServerManager {

    private Server server;
    private HttpConfiguration configuration;
    
    protected void init() throws Exception {
        if (configuration == null) {
            configuration = new HttpConfiguration();
        }
        if (server == null) {
            server = new Server();
            BoundedThreadPool btp = new BoundedThreadPool();
            btp.setMaxThreads(this.configuration.getJettyThreadPoolSize());
            server.setThreadPool(btp);
        }
    }

    protected void shutDown() throws Exception {
        server.stop();
    }

    protected void start() throws Exception {
        server.start();
    }

    protected void stop() throws Exception {
        server.stop();
    }
    
    // TODO: handle https
    public synchronized ContextHandler createContext(String strUrl, HttpProcessor processor) throws Exception {
        URL url = new URL(strUrl);
        Connector listener = getListener(url);
        if (listener == null) {
            listener = createListener(url);
        }
        String path = url.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        ContextHandler context = new ContextHandler();
        context.setContextPath(path);
        ServletHolder holder = new ServletHolder();
        holder.setName("jbiServlet");
        holder.setClassName(HttpBridgeServlet.class.getName());
        ServletHandler handler = new ServletHandler();
        handler.setServlets(new ServletHolder[] { holder });
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName("jbiServlet");
        mapping.setPathSpec("/*");
        handler.setServletMappings(new ServletMapping[] { mapping });
        context.setHandler(handler);
        context.setAttribute("processor", processor);
        // add context
        Handler[] handlers = server.getHandlers();
        handlers = (Handler[]) add(handlers, context, Handler.class);
        server.setHandlers(handlers);
        return context;
    }
    
    private Object[] add(Object[] array, Object obj, Class type) {
        List l = new ArrayList();
        if (array != null) {
            l.addAll(Arrays.asList(array));
        }
        l.add(obj);
        return l.toArray((Object[]) Array.newInstance(type, l.size()));
    }
    
    private Object[] remove(Object[] array, Object obj, Class type) {
        List l = new ArrayList();
        if (array != null) {
            l.addAll(Arrays.asList(array));
        }
        l.remove(obj);
        return l.toArray((Object[]) Array.newInstance(type, l.size()));
    }
    
    public synchronized void remove(ContextHandler context) {
        Handler[] handlers = server.getHandlers();
        handlers = (Handler[]) remove(handlers, context, Handler.class);
        server.setHandlers(handlers);
    }

    protected Connector getListener(URL url) {
        Connector[] listeners = server.getConnectors();
        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].getPort() == url.getPort()) {
                    if (!listeners[i].getHost().equals(url.getHost())) {
                        throw new IllegalStateException("The same port is already used for another host");
                    }
                    // TODO: check protocol
                    return listeners[i];
                }
            }
        }
        return null;
    }
    
    protected Connector createListener(URL url) throws Exception {
        if (!url.getProtocol().equals("http")) {
            // TODO: handle https ?
            throw new UnsupportedOperationException("Protocol " + url.getProtocol() + " is not supported");
        }
        String connectorClassName = configuration.getJettyConnectorClassName();
        Connector listener;
        try {
            listener = (Connector) Class.forName(connectorClassName).newInstance();
        } catch (Exception e) {
            // TODO use logger
            e.printStackTrace();
            listener = new SelectChannelConnector();
        }
        listener.setHost(url.getHost());
        listener.setPort(url.getPort());
        Connector[] connectors = server.getConnectors();
        connectors = (Connector[]) add(connectors, listener, Connector.class);
        server.setConnectors(connectors);
        listener.start();
        return listener;
    }

    public HttpConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(HttpConfiguration configuration) {
        this.configuration = configuration;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

}
