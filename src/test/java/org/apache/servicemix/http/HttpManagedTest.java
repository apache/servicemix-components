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

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.components.http.InvalidStatusResponseException;
import org.apache.xbean.spring.context.XmlWebApplicationContext;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.springframework.web.context.ContextLoaderListener;

public class HttpManagedTest extends TestCase {
    private static Log logger =  LogFactory.getLog(HttpManagedTest.class);

    private Server server;
    
    protected void shutDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
    
    public void test() throws Exception {
        ContextHandler context = new ContextHandler();
        context.setContextPath("/test");
        context.setEventListeners(new EventListener[] { new ContextLoaderListener() });
        Map initParams = new HashMap();
        initParams.put("contextConfigLocation", "classpath:org/apache/servicemix/http/spring-web.xml");
        initParams.put("contextClass", XmlWebApplicationContext.class.getName());
        context.setInitParams(initParams);
        ServletHolder holder = new ServletHolder();
        holder.setName("jbiServlet");
        holder.setClassName(HttpManagedServlet.class.getName());
        ServletHandler handler = new ServletHandler();
        handler.setServlets(new ServletHolder[] { holder });
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName("jbiServlet");
        mapping.setPathSpec("/*");
        handler.setServletMappings(new ServletMapping[] { mapping });
        context.setHandler(handler);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] { contexts });
        contexts.addHandler(context);

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost("localhost");
        connector.setPort(8190);
        
        server = new Server();
        server.setConnectors(new Connector[] { connector });
        server.setHandler(handlers);
        server.start();
        
        logger.info("Started");
        
        PostMethod post = new PostMethod("http://localhost:8190/test/jbi/Service/");
        post.setRequestEntity(new StringRequestEntity("<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'><soap:Body><hello>world</hello></soap:Body></soap:Envelope>"));
        new HttpClient().executeMethod(post);
        if (post.getStatusCode() != 200) {
            throw new InvalidStatusResponseException(post.getStatusCode());
        }
        logger.info(post.getResponseBodyAsString());
        
    }
}
