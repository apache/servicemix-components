package org.apache.servicemix.http;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.servicemix.components.http.InvalidStatusResponseException;
import org.apache.servicemix.xbean.XmlWebApplicationContext;
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

    public void test() throws Exception {
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
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
        connector.setPort(8080);
        
        Server server = new Server();
        server.setConnectors(new Connector[] { connector });
        server.setHandler(handlers);
        server.start();
        
        System.err.println("Started");
        
        PostMethod post = new PostMethod("http://localhost:8080/jbi/Service/");
        post.setRequestEntity(new StringRequestEntity("<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'><soap:Body><hello>world</hello></soap:Body></soap:Envelope>"));
        new HttpClient().executeMethod(post);
        if (post.getStatusCode() != 200) {
            throw new InvalidStatusResponseException(post.getStatusCode());
        }
        System.err.println(post.getResponseBodyAsString());
        
    }
}
