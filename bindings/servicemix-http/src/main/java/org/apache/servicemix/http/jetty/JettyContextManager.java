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
package org.apache.servicemix.http.jetty;

import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.servicemix.common.security.KeystoreManager;
import org.apache.servicemix.http.*;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.jbi.JBIException;
import javax.management.MBeanServer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JettyContextManager implements ContextManager {

    private final Logger logger = LoggerFactory.getLogger(JettyContextManager.class);

    private Map<String, Server> servers;
    private HttpConfiguration configuration;
    private QueuedThreadPool threadPool;
    private Map<String, SslParameters> sslParams;
    private MBeanServer mBeanServer;
    private MBeanContainer mbeanContainer;

    /**
     * @return the mbeanServer
     */
    public MBeanServer getMBeanServer() {
        return mBeanServer;
    }

    /**
     * @param mBeanServer the mbeanServer to set
     */
    public void setMBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
    }

    public void init() throws Exception {
        if (configuration == null) {
            configuration = new HttpConfiguration();
        }
        if (mBeanServer != null && !configuration.isManaged() && configuration.isJettyManagement()) {
            mbeanContainer = new MBeanContainer(mBeanServer);
        }
        servers = new HashMap<String, Server>();
        sslParams = new HashMap<String, SslParameters>();
        QueuedThreadPool qtp = new QueuedThreadPool();
        qtp.setMaxThreads(this.configuration.getJettyThreadPoolSize());
        threadPool = qtp;
    }

    public void shutDown() throws Exception {
        stop();
    }

    public void start() throws Exception {
        threadPool.start();
        for (Iterator<Server> it = servers.values().iterator(); it.hasNext(); ) {
            Server server = it.next();
            server.start();
        }
    }

    public void stop() throws Exception {
        for (Iterator<Server> it = servers.values().iterator(); it.hasNext(); ) {
            Server server = it.next();
            server.stop();
        }
        for (Iterator<Server> it = servers.values().iterator(); it.hasNext(); ) {
            Server server = it.next();
            server.join();
            Connector[] connectors = server.getConnectors();
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof AbstractConnector) {
                    ((AbstractConnector) connectors[i]).join();
                }
            }
        }
        threadPool.stop();
    }

    public synchronized Object createContext(String strUrl, HttpProcessor processor) throws Exception {
        URL url = new URL(strUrl);
        Server server = getServer(url);
        if (server == null) {
            server = createServer(url, processor.getSsl());
        } else {
            // Check ssl params
            SslParameters ssl = sslParams.get(getKey(url));
            if (ssl != null && !ssl.equals(processor.getSsl())) {
                throw new Exception("An https server is already created on port " + url.getPort()
                        + " but SSL parameters do not match");
            }
        }
        String path = url.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String pathSlash = path + "/";
        // Check that context does not exist yet
        HandlerCollection handlerCollection = (HandlerCollection) server.getHandler();
        ContextHandlerCollection contexts = (ContextHandlerCollection) handlerCollection.getHandlers()[0];
        Handler[] handlers = contexts.getHandlers();
        if (handlers != null) {
            for (int i = 0; i < handlers.length; i++) {
                if (handlers[i] instanceof ContextHandler) {
                    ContextHandler h = (ContextHandler) handlers[i];
                    String handlerPath = h.getContextPath() + "/";
                    if (handlerPath.startsWith(pathSlash) || pathSlash.startsWith(handlerPath)) {
                        throw new Exception("The requested context for path '" + path
                                + "' overlaps with an existing context for path: '" + h.getContextPath() + "'");
                    }
                }
            }
        }
        // Create context
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(path);
        ServletHolder holder = new ServletHolder();
        holder.setName("jbiServlet");
        holder.setClassName(HttpBridgeServlet.class.getName());
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setServlets(new ServletHolder[]{holder});
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName("jbiServlet");
        mapping.setPathSpec("/*");
        servletHandler.setServletMappings(new ServletMapping[]{mapping});
        if (processor.getAuthMethod() != null) {
            ConstraintSecurityHandler secHandler = new ConstraintSecurityHandler();
            ConstraintMapping constraintMapping = new ConstraintMapping();
            Constraint constraint = new Constraint();
            constraint.setAuthenticate(true);
            constraint.setRoles(new String[]{"*"});
            constraintMapping.setConstraint(constraint);
            constraintMapping.setPathSpec("/");
            secHandler.setConstraintMappings(new ConstraintMapping[]{constraintMapping});
            secHandler.setHandler(servletHandler);
            secHandler.setAuthMethod(processor.getAuthMethod());
            JaasUserRealm realm = new JaasUserRealm();
            if (configuration.getAuthenticationService() != null) {
                realm.setAuthenticationService(AuthenticationService.Proxy.create(configuration.getAuthenticationService()));
            }
            secHandler.setLoginService(realm);
            context.setSecurityHandler(secHandler);
        }
        context.setServletHandler(servletHandler);
        context.setAttribute("processor", processor);
        // add context
        contexts.addHandler(context);
        servletHandler.initialize();
        context.start();
        return context;
    }

    public synchronized void remove(Object context) throws Exception {
        ((ContextHandler) context).stop();
        for (Iterator<Server> it = servers.values().iterator(); it.hasNext(); ) {
            Server server = it.next();
            HandlerCollection handlerCollection = (HandlerCollection) server.getHandler();
            ContextHandlerCollection contexts = (ContextHandlerCollection) handlerCollection.getHandlers()[0];
            Handler[] handlers = contexts.getHandlers();
            if (handlers != null && handlers.length > 0) {
                contexts.setHandlers((Handler[]) LazyList.removeFromArray(handlers, context));
            }
        }
    }

    protected Server getServer(URL url) {
        return servers.get(getKey(url));
    }

    protected String getKey(URL url) {
        String host = url.getHost();
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isAnyLocalAddress()) {
                host = InetAddress.getLocalHost().getHostName();
            }
        } catch (UnknownHostException e) {
            //unable to lookup host name, using IP address instead
        }
        return url.getProtocol() + "://" + host + ":" + url.getPort();
    }

    protected Server createServer(URL url, SslParameters ssl) throws Exception {
        boolean isSsl = false;
        if (url.getProtocol().equals("https")) {
            // TODO: put ssl default information on HttpConfiguration
            if (ssl == null) {
                throw new IllegalArgumentException("https protocol required but no ssl parameters found");
            }
            isSsl = true;
        } else if (!url.getProtocol().equals("http")) {
            throw new UnsupportedOperationException("Protocol " + url.getProtocol() + " is not supported");
        }
        // Create a new server
        Connector connector;
        if (isSsl && ssl.isManaged()) {
            connector = setupManagerSslConnector(url, ssl);
        } else if (isSsl) {
            connector = setupSslConnector(url, ssl);
        } else {
            String connectorClassName = configuration.getJettyConnectorClassName();
            try {
                connector = (Connector) Class.forName(connectorClassName).newInstance();
            } catch (Exception e) {
                logger.warn("Could not create a jetty connector of class '{}'. Defaulting to {}", connectorClassName, HttpConfiguration.DEFAULT_JETTY_CONNECTOR_CLASS_NAME);
                logger.debug("Reason: {}", e.getMessage(), e);
                connector = (Connector) Class.forName(HttpConfiguration.DEFAULT_JETTY_CONNECTOR_CLASS_NAME)
                        .newInstance();
            }
        }
        connector.setHost(url.getHost());
        connector.setPort(url.getPort());
        connector.setMaxIdleTime(this.configuration.getConnectorMaxIdleTime());
        ((AbstractConnector) connector).setSoLingerTime(this.configuration.getSoLingerTime());
        Server server = new Server();
        server.setThreadPool(new ThreadPoolWrapper());
        server.setConnectors(new Connector[]{connector});
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{contexts, new DisplayServiceHandler()});
        server.setHandler(handlers);
        servers.put(getKey(url), server);
        sslParams.put(getKey(url), isSsl ? ssl : null);
        if (mbeanContainer != null) {
            server.getContainer().addEventListener(mbeanContainer);
        }
        int serverGracefulTimeout = this.configuration.getServerGracefulTimeout();
        if (serverGracefulTimeout > 0){
            server.setGracefulShutdown(serverGracefulTimeout);
            server.setStopAtShutdown(true);
        }
        server.start();
        return server;
    }

    private Connector setupSslConnector(URL url, SslParameters ssl) throws JBIException {
        Connector connector;
        String keyStore = ssl.getKeyStore();
        if (keyStore == null) {
            keyStore = System.getProperty("javax.net.ssl.keyStore", "");
            if (keyStore == null) {
                throw new IllegalArgumentException(
                        "keyStore or system property javax.net.ssl.keyStore must be set");
            }
        }
        if (keyStore.startsWith("classpath:")) {
            try {
                String res = keyStore.substring(10);
                URL resurl = new ClassPathResource(res).getURL();
                keyStore = resurl.toString();
            } catch (IOException e) {
                throw new JBIException("Unable to find keystore " + keyStore, e);
            }
        }
        String keyStorePassword = ssl.getKeyStorePassword();
        if (keyStorePassword == null) {
            keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
            if (keyStorePassword == null) {
                throw new IllegalArgumentException(
                        "keyStorePassword or system property javax.net.ssl.keyStorePassword must be set");
            }
        }
        SslSocketConnector sslConnector = new SslSocketConnector();
        sslConnector.setSslKeyManagerFactoryAlgorithm(ssl.getKeyManagerFactoryAlgorithm());
        sslConnector.setSslTrustManagerFactoryAlgorithm(ssl.getTrustManagerFactoryAlgorithm());
        sslConnector.setProtocol(ssl.getProtocol());
        sslConnector.setConfidentialPort(url.getPort());
        sslConnector.setPassword(ssl.getKeyStorePassword());
        sslConnector.setKeyPassword(ssl.getKeyPassword() != null ? ssl.getKeyPassword() : keyStorePassword);
        sslConnector.setKeystore(keyStore);
        sslConnector.setKeystoreType(ssl.getKeyStoreType());
        sslConnector.setNeedClientAuth(ssl.isNeedClientAuth());
        sslConnector.setWantClientAuth(ssl.isWantClientAuth());
        // important to set this values for selfsigned keys
        // otherwise the standard truststore of the jre is used
        sslConnector.setTruststore(ssl.getTrustStore());
        if (ssl.getTrustStorePassword() != null) {
            // check is necessary because if a null password is set
            // jetty would ask for a password on the comandline
            sslConnector.setTrustPassword(ssl.getTrustStorePassword());
        }
        sslConnector.setTruststoreType(ssl.getTrustStoreType());
        connector = sslConnector;
        return connector;
    }

    private Connector setupManagerSslConnector(URL url, SslParameters ssl) {
        Connector connector;
        String keyStore = ssl.getKeyStore();
        if (keyStore == null) {
            throw new IllegalArgumentException("keyStore must be set");
        }
        ServiceMixSslSocketConnector sslConnector = new ServiceMixSslSocketConnector();
        sslConnector.setSslKeyManagerFactoryAlgorithm(ssl.getKeyManagerFactoryAlgorithm());
        sslConnector.setSslTrustManagerFactoryAlgorithm(ssl.getTrustManagerFactoryAlgorithm());
        sslConnector.setProtocol(ssl.getProtocol());
        sslConnector.setConfidentialPort(url.getPort());
        sslConnector.setKeystore(keyStore);
        sslConnector.setKeyAlias(ssl.getKeyAlias());
        sslConnector.setNeedClientAuth(ssl.isNeedClientAuth());
        sslConnector.setWantClientAuth(ssl.isWantClientAuth());
        sslConnector.setKeystoreManager(KeystoreManager.Proxy.create(getConfiguration().getKeystoreManager()));
        // important to set this values for selfsigned keys
        // otherwise the standard truststore of the jre is used
        sslConnector.setTruststore(ssl.getTrustStore());
        if (ssl.getTrustStorePassword() != null) {
            // check is necessary because if a null password is set
            // jetty would ask for a password on the comandline
            sslConnector.setTrustPassword(ssl.getTrustStorePassword());
        }
        sslConnector.setTruststoreType(ssl.getTrustStoreType());
        connector = sslConnector;
        return connector;
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

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (response.isCommitted() || AbstractHttpConnection.getCurrentConnection().getRequest().isHandled()) {
                return;
            }

            String method = request.getMethod();

            if (!method.equals(HttpMethods.GET) || !request.getRequestURI().equals("/")) {
                response.sendError(404);
                return;
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

            for (String serverUri : servers.keySet()) {
                Server server = JettyContextManager.this.servers.get(serverUri);
                Handler[] handlers = server.getChildHandlersByClass(ContextHandler.class);
                for (int i = 0; handlers != null && i < handlers.length; i++) {
                    if (!(handlers[i] instanceof ContextHandler)) {
                        continue;
                    }
                    ContextHandler context = (ContextHandler) handlers[i];
                    StringBuffer sb = new StringBuffer();
                    sb.append(serverUri);
                    if (!context.getContextPath().startsWith("/")) {
                        sb.append("/");
                    }
                    sb.append(context.getContextPath());
                    if (!context.getContextPath().endsWith("/")) {
                        sb.append("/");
                    }
                    if (context.isStarted()) {
                        writer.write("<li><a href=\"");
                        writer.write(sb.toString());
                        writer.write("?wsdl\">");
                        writer.write(sb.toString());
                        writer.write("</a></li>\n");
                    } else {
                        writer.write("<li>");
                        writer.write(sb.toString());
                        writer.write(" [Stopped]</li>\n");
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                writer.write("\n<!-- Padding for IE                  -->");
            }

            writer.write("\n</BODY>\n</HTML>\n");
            writer.flush();
            response.setContentLength(writer.size());
            OutputStream out = response.getOutputStream();
            writer.writeTo(out);
            out.close();
        }

    }

    protected class ThreadPoolWrapper extends AbstractLifeCycle implements ThreadPool {

        public boolean dispatch(Runnable job) {
            logger.debug("Dispatching job: {}", job);
            return threadPool.dispatch(job);
        }

        public int getIdleThreads() {
            return threadPool.getIdleThreads();
        }

        public int getThreads() {
            return threadPool.getThreads();
        }

        public void join() throws InterruptedException {
        }

        public boolean isLowOnThreads() {
            return threadPool.isLowOnThreads();
        }
    }

    public HttpProcessor getMainProcessor() {
        throw new IllegalStateException("ServerManager is not managed");
    }

}
