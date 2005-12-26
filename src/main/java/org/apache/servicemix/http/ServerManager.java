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

import java.net.URL;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpListener;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

public class ServerManager {

    private Server server;
    
    protected void init() throws Exception {
        if (server == null) {
            server = new Server();
        }
    }

    protected void shutDown() throws Exception {
        server.stop(true);
    }

    protected void start() throws Exception {
        server.start();
    }

    protected void stop() throws Exception {
        server.stop();
    }
    
    // TODO: handle https
    public HttpContext createContext(String strUrl, HttpProcessor processor) throws Exception {
        URL url = new URL(strUrl);
        HttpListener listener = getListener(url);
        if (listener == null) {
            listener = createListener(url);
        }
        String path = url.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        HttpContext context = server.addContext(path);
        ServletHandler handler = new ServletHandler();
        handler.addServlet("jbiServlet", "/*", HttpBridgeServlet.class.getName());
        context.addHandler(handler);
        context.setAttribute("processor", processor);
        handler.start();
        return context;
    }
    
    public void remove(HttpContext context) {
        server.removeContext(context);
    }

    protected HttpListener getListener(URL url) {
        HttpListener[] listeners = server.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i].getPort() == url.getPort()) {
                if (!listeners[i].getHost().equals(url.getHost())) {
                    throw new IllegalStateException("The same port is already used for another host");
                }
                // TODO: check protocol
                return listeners[i];
            }
        }
        return null;
    }
    
    protected HttpListener createListener(URL url) throws Exception {
        if (!url.getProtocol().equals("http")) {
            // TODO: handle https ?
            throw new UnsupportedOperationException("Protocol " + url.getProtocol() + " is not supported");
        }
        SocketListener listener = new SocketListener();
        listener.setHost(url.getHost());
        listener.setPort(url.getPort());
        server.addListener(listener);
        listener.start();
        return listener;
    }

}
