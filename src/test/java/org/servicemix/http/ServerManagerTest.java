/** 
 * 
 * Copyright 2005 Protique Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.http.HttpContext;
import org.servicemix.jbi.util.FileUtil;

public class ServerManagerTest extends TestCase {

    protected ServerManager server;
    
    protected void setUp() throws Exception {
        server = new ServerManager();
        server.init();
    }
    
    protected void tearDown() throws Exception {
        server.shutDown();
    }
    
    public void test() throws Exception {
        server.start();
        
        // Test first context 
        checkFail("http://localhost:8192/Service1", null);
        HttpContext ctx1 = server.createContext("http://localhost:8192/Service1", new TestHttpProcessor());
        checkFail("http://localhost:8192/Service1", null);
        ctx1.start();
        request("http://localhost:8192/Service1", null);
        
        // Test second context on the same host/port
        checkFail("http://localhost:8192/Service2", null);
        HttpContext ctx2 = server.createContext("http://localhost:8192/Service2", new TestHttpProcessor());
        checkFail("http://localhost:8192/Service2", null);
        ctx2.start();
        request("http://localhost:8192/Service2", null);
        ctx2.stop();
        checkFail("http://localhost:8192/Service2", null);

        // Test third context on another port
        checkFail("http://localhost:8193", null);
        HttpContext ctx3 = server.createContext("http://localhost:8193", new TestHttpProcessor());
        checkFail("http://localhost:8193", null);
        ctx3.start();
        request("http://localhost:8193/echo", null);
    }
    
    protected void checkFail(String url, String content) {
        try {
            request(url, content);
            fail("Request should have failed: " + url);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    protected String request(String url, String content) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setDoInput(true);
        if (content != null) {
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(content.getBytes());
            os.close();
        }
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(is, baos);
        return baos.toString();
    }
    
    public static class TestHttpProcessor implements HttpProcessor {
        public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
            System.out.println(request);
        }
        
    }
}
