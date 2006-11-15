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
package org.apache.servicemix.http.security;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.w3c.dom.Element;

public class HttpSecurityTest extends SpringTestSupport {
    
    static {
        String path = System.getProperty("java.security.auth.login.config");
        if (path == null) {
            URL resource = HttpSecurityTest.class.getResource("login.properties");
            if (resource != null) {
                path = new File(resource.getFile()).getAbsolutePath();
                System.setProperty("java.security.auth.login.config", path);
            }
        }
        System.err.println("Path to login config: " + path);
    }
    
    protected void setUp() throws Exception {
        Thread.sleep(500);
        super.setUp();
        Thread.sleep(500);
    }
    
    public void testOk() throws Exception {
        testAuthenticate("user1", "user1");
    }
    
    public void testUnauthorized() throws Exception {
        try {
            testAuthenticate("user2", "user2");
            fail("User2 is not authorized");
        } catch (Exception e) {
            e.printStackTrace();
            // ok
        }
    }
    
    public void testBadCred() throws Exception {
        try {
            testAuthenticate("user2", "userx");
            fail("User2 has bad credentials");
        } catch (Exception e) {
            e.printStackTrace();
            // ok
        }
    }
    
    protected void testAuthenticate(final String username, final String password) throws Exception {
        HttpClient client = new HttpClient();
        client.getState().setCredentials(
                        new AuthScope(AuthScope.ANY),
                        new UsernamePasswordCredentials(username, password)
                    );
        
        PostMethod method = new PostMethod("http://localhost:8192/Service/");
        try {
            method.setDoAuthentication(true);
            method.setRequestEntity(new StringRequestEntity("<hello>world</hello>"));
            int state = client.executeMethod(method);
            FileUtil.copyInputStream(method.getResponseBodyAsStream(), System.err);
            if (state != HttpServletResponse.SC_OK && state != HttpServletResponse.SC_ACCEPTED) {
                throw new IllegalStateException("Http status: " + state);
            }
        } finally {
            method.releaseConnection();
        }
    }
    
    public void testWSSec() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyInputStream(getClass().getResourceAsStream("request.xml"), out);
        String request = out.toString();
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("http://localhost:8192/WSSec/");
        try {
            method.setDoAuthentication(true);
            method.setRequestEntity(new StringRequestEntity(request));
            int state = client.executeMethod(method);
            FileUtil.copyInputStream(method.getResponseBodyAsStream(), System.err);
            if (state != HttpServletResponse.SC_OK && state != HttpServletResponse.SC_ACCEPTED) {
                throw new IllegalStateException("Http status: " + state);
            }
        } finally {
            method.releaseConnection();
        }
    }

    public void testWSSecBadCred() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyInputStream(getClass().getResourceAsStream("request-bc.xml"), out);
        String request = out.toString();
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("http://localhost:8192/WSSec/");
        try {
            method.setDoAuthentication(true);
            method.setRequestEntity(new StringRequestEntity(request));
            int state = client.executeMethod(method);
            String str = method.getResponseBodyAsString();
            System.err.println(str);
            assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, state);
            Element e = new SourceTransformer().toDOMElement(new StringSource(str));
            assertEquals("Envelope", e.getLocalName());
            e = (Element) e.getFirstChild();
            assertEquals("Body", e.getLocalName());
            e = (Element) e.getFirstChild();
            assertEquals("Fault", e.getLocalName());
        } finally {
            method.releaseConnection();
        }
    }

    public void testWSSecUnkownUser() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyInputStream(getClass().getResourceAsStream("request-uu.xml"), out);
        String request = out.toString();
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("http://localhost:8192/WSSec/");
        try {
            method.setDoAuthentication(true);
            method.setRequestEntity(new StringRequestEntity(request));
            int state = client.executeMethod(method);
            String str = method.getResponseBodyAsString();
            System.err.println(str);
            assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, state);
            Element e = new SourceTransformer().toDOMElement(new StringSource(str));
            assertEquals("Envelope", e.getLocalName());
            e = (Element) e.getFirstChild();
            assertEquals("Body", e.getLocalName());
            e = (Element) e.getFirstChild();
            assertEquals("Fault", e.getLocalName());
        } finally {
            method.releaseConnection();
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/http/security/secure.xml");
    }

}
