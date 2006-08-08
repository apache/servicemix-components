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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.StringUtil;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class ManagedContextManager implements ContextManager {

    private static final Log logger = LogFactory.getLog(ManagedContextManager.class);
    
    private HttpConfiguration configuration;
    private Map managedContexts;
    
    public void init() throws Exception {
        if (configuration == null) {
            configuration = new HttpConfiguration();
        }
        managedContexts = new ConcurrentHashMap();
    }

    public void shutDown() throws Exception {
        stop();
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }
    
    public synchronized Object createContext(String strUrl, 
                                             HttpProcessor processor) throws Exception {
        URI uri = new URI(strUrl);
        String path = uri.getPath();
        if (!path.startsWith("/")) {
            path = path + "/";
        }
        if (!path.endsWith("/")) {
            path = path + "/"; 
        }
        managedContexts.put(path, processor);
        return path;
    }
    
    public synchronized void remove(Object context) throws Exception {
        managedContexts.remove(context);
    }

    public HttpConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(HttpConfiguration configuration) {
        this.configuration = configuration;
    }

    public HttpProcessor getMainProcessor() {
        if (!configuration.isManaged()) {
            throw new IllegalStateException("ServerManager is not managed");
        }
        return new MainProcessor();
    }
    
    protected class MainProcessor implements HttpProcessor {
        private String mapping;
        public MainProcessor() {
            this.mapping = configuration.getMapping();
        }
        public String getAuthMethod() {
            return null;
        }
        public SslParameters getSsl() {
            return null;
        }
        public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String uri = request.getRequestURI();
            if ("/".equals(uri) && "GET".equals(request.getMethod())) {
                displayServices(request, response);
                return;
            }
            Set urls = managedContexts.keySet();
            for (Iterator iter = urls.iterator(); iter.hasNext();) {
                String url = (String) iter.next();
                if (uri.startsWith(request.getContextPath() + mapping + url)) {
                    HttpProcessor proc = (HttpProcessor) managedContexts.get(url);
                    proc.process(request, response);
                    return;
                }
            }
            displayServices(request, response);
        }
        public void displayServices(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

            Set servers = ManagedContextManager.this.managedContexts.keySet();
            for (Iterator iter = servers.iterator(); iter.hasNext();) {
                String context = (String) iter.next();
                if (!context.endsWith("/")) {
                    context += "/";
                }
                String protocol = request.isSecure() ? "https" : "http";
                context = protocol + "://" + request.getLocalName() + ":" + request.getLocalPort() + request.getContextPath() + mapping + context; 
                writer.write("<li><a href=\"");
                writer.write(context);
                writer.write("?wsdl\">");
                writer.write(context);
                writer.write("</a></li>\n");
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
        }
    }

}
