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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.thread.BoundedThreadPool;
import org.mortbay.thread.ThreadPool;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.StringUtil;

public class ServerManager {

    private static final Log logger = LogFactory.getLog(ServerManager.class);
    
    private Map servers;
    private HttpConfiguration configuration;
    private ThreadPool threadPool;
    
    protected void init() throws Exception {
        if (configuration == null) {
            configuration = new HttpConfiguration();
        }
        servers = new HashMap();
        BoundedThreadPool btp = new BoundedThreadPool();
        btp.setMaxThreads(this.configuration.getJettyThreadPoolSize());
        threadPool = btp;
    }

    protected void shutDown() throws Exception {
        stop();
    }

    protected void start() throws Exception {
        threadPool.start();
        for (Iterator it = servers.values().iterator(); it.hasNext();) {
            Server server = (Server) it.next();
            server.start();
        }
    }

    protected void stop() throws Exception {
        for (Iterator it = servers.values().iterator(); it.hasNext();) {
            Server server = (Server) it.next();
            server.stop();
        }
        threadPool.stop();
    }
    
    public synchronized ContextHandler createContext(String strUrl, HttpProcessor processor) throws Exception {
        URL url = new URL(strUrl);
        Server server = getServer(url);
        if (server == null) {
            server = createServer(url);
        }
        String path = url.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // Check that context does not exist yet
        Handler[] handlers = server.getHandlers();
        if (handlers != null) {
            for (int i = 0; i < handlers.length; i++) {
                if (handlers[i] instanceof ContextHandler) {
                    ContextHandler h = (ContextHandler) handlers[i];
                    if (h.getContextPath().startsWith(path) ||
                        path.startsWith(h.getContextPath())) {
                        throw new Exception("The requested context for path '" + path + "' overlaps with an existing context for path: '" + h.getContextPath() + "'");
                    }
                }
            }
        }
        // Create context
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
        for (Iterator it = servers.values().iterator(); it.hasNext();) {
            Server server = (Server) it.next();
            Handler[] handlers = server.getHandlers();
            handlers = (Handler[]) remove(handlers, context, Handler.class);
            server.setHandlers(handlers);
        }
    }

    protected Server getServer(URL url) {
        String key = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
        Server server = (Server) servers.get(key);
        return server;
    }
    
    protected Server createServer(URL url) throws Exception {
        if (!url.getProtocol().equals("http")) {
            // TODO: handle https ?
            throw new UnsupportedOperationException("Protocol " + url.getProtocol() + " is not supported");
        }
        // Create a new server
        String connectorClassName = configuration.getJettyConnectorClassName();
        Connector connector;
        try {
            connector = (Connector) Class.forName(connectorClassName).newInstance();
        } catch (Exception e) {
            logger.warn("Could not create a jetty connector of class '" + connectorClassName + "'. Defaulting to " + HttpConfiguration.DEFAULT_JETTY_CONNECTOR_CLASS_NAME);
            if (logger.isDebugEnabled()) {
                logger.debug("Reason: " + e.getMessage(), e);
            }
            connector = (Connector) Class.forName(HttpConfiguration.DEFAULT_JETTY_CONNECTOR_CLASS_NAME).newInstance();
        }
        connector.setHost(url.getHost());
        connector.setPort(url.getPort());
        Server server = new Server();
        server.setThreadPool(threadPool);
        server.setConnectors(new Connector[] { connector });
        server.setNotFoundHandler(new DisplayServiceHandler());
        connector.start();
        server.start();
        String key = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
        servers.put(key, server);
        return server;
    }

    public HttpConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(HttpConfiguration configuration) {
        this.configuration = configuration;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }
    
    protected class DisplayServiceHandler extends AbstractHandler {

        public boolean handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
            String method = request.getMethod();
            
            if (!method.equals(HttpMethods.GET) || !request.getRequestURI().equals("/")) {
                response.sendError(404);
                return true;   
            }

            response.setStatus(404);
            response.setContentType(MimeTypes.TEXT_HTML);
            
            ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);

            String uri = request.getRequestURI();
            uri = StringUtil.replace(uri, "<", "&lt;");
            uri = StringUtil.replace(uri, ">", "&gt;");
            
            writer.write("<HTML>\n<HEAD>\n<TITLE>Error 404 - Not Found");
            writer.write("</TITLE>\n<BODY>\n<H2>Error 404 - Not Found.</H2>\n");
            writer.write("No service matched or handled this request.<BR>");
            writer.write("Known services are: <ul>");

            Set servers = ServerManager.this.servers.keySet();
            for (Iterator iter = servers.iterator(); iter.hasNext();) {
                String serverUri = (String) iter.next();
                Server server = (Server) ServerManager.this.servers.get(serverUri);
                Handler[] handlers = server.getAllHandlers();
                for (int i = 0; handlers != null && i < handlers.length; i++)
                {
                    if (!(handlers[i] instanceof ContextHandler)) {
                        continue;
                    }
                    ContextHandler context = (ContextHandler)handlers[i];
                        writer.write("<li><a href=\"");
                        writer.write(serverUri);
                        if (!context.getContextPath().startsWith("/")) {
                            writer.write("/");
                        }
                        writer.write(context.getContextPath());
                        if (!context.getContextPath().endsWith("/")) {
                            writer.write("/");
                        }
                        writer.write("?wsdl\">");
                        writer.write(serverUri);
                        writer.write(context.getContextPath());
                        writer.write("</a></li>\n");
                }
            }
            
            for (int i=0; i < 10; i++) {
                writer.write("\n<!-- Padding for IE                  -->");
            }
            
            writer.write("\n</BODY>\n</HTML>\n");
            writer.flush();
            response.setContentLength(writer.size());
            OutputStream out = response.getOutputStream();
            writer.writeTo(out);
            out.close();
            
            return true;
        }
        
    }

}
