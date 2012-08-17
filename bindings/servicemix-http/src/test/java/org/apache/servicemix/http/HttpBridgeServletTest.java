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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public class HttpBridgeServletTest extends TestCase {

    private HttpBridgeServlet httpBridgeServlet;

    protected void setUp() throws Exception {
        super.setUp();
        httpBridgeServlet = new HttpBridgeServlet();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        httpBridgeServlet = null;
    }

    // Test init() when HttpProcessor is null.
    public void testInitProcessorNull() throws Exception {
        httpBridgeServlet.setProcessor(null);
        TestServletConfig config = new TestServletConfig();
        
        try {
            httpBridgeServlet.init(config);
            fail("init() should fail when HttpProcessor is null");
        } catch (ServletException se) {
            String errorMsg = se.getMessage();
            assertTrue("ServletException does not contain the expected error message", errorMsg.contains("No binding property available"));
        }
    }

    // Test service() method - check for exceptions, fail if any are thrown.
    public void testService() throws Exception {
        TestHttpProcessor processor = new TestHttpProcessor();
        httpBridgeServlet.setProcessor(processor);
        TestServletConfig config = new TestServletConfig();

        httpBridgeServlet.init(config);

        Request request = new Request();
        Response response = null;

        try {
            httpBridgeServlet.service(request, response);
        } catch (Exception e) {
            fail("service() should not throw an exception");
        }
    }

    // Dummy ServletConfig implementation for testing.  
    public static class TestServletConfig implements ServletConfig {

        public String getInitParameter(String name) {
            return null;
        }

        public Enumeration<String> getInitParameterNames() {
            return null;
        }

        public ServletContext getServletContext() {
            return new TestServletContext();
        }

        public String getServletName() {
            return null;
        }
    }

    // Dummy ServletContext implementation for testing.
    public static class TestServletContext implements ServletContext {

        public Object getAttribute(String name) {
            return null;
        }

        public Enumeration<String> getAttributeNames() {
            return null;
        }

        public ServletContext getContext(String uripath) {
            return this;
        }

        public String getContextPath() {
            return null;
        }

        public String getInitParameter(String name) {
            return null;
        }

        public Enumeration<String> getInitParameterNames() {
            return null;
        }

        public boolean setInitParameter(String s, String s1) {
            return false;
        }

        public int getMajorVersion() {
            return 0;
        }

        public String getMimeType(String file) {
            return null;
        }

        public int getMinorVersion() {
            return 0;
        }

        public RequestDispatcher getNamedDispatcher(String name) {
            return null;
        }

        public String getRealPath(String path) {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        public URL getResource(String path) throws MalformedURLException {
            return null;
        }

        public InputStream getResourceAsStream(String path) {
            return null;
        }

        public Set<String> getResourcePaths(String path) {
            return null;
        }

        public String getServerInfo() {
            return null;
        }

        public String getServletContextName() {
            return null;
        }

        public ServletRegistration.Dynamic addServlet(String s, String s1) throws IllegalArgumentException, IllegalStateException {
            return null;
        }

        public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) throws IllegalArgumentException, IllegalStateException {
            return null;
        }

        public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) throws IllegalArgumentException, IllegalStateException {
            return null;
        }

        public <T extends Servlet> T createServlet(Class<T> tClass) throws ServletException {
            return null;
        }

        public ServletRegistration getServletRegistration(String s) {
            return null;
        }

        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            return null;
        }

        public FilterRegistration.Dynamic addFilter(String s, String s1) throws IllegalArgumentException, IllegalStateException {
            return null;
        }

        public FilterRegistration.Dynamic addFilter(String s, Filter filter) throws IllegalArgumentException, IllegalStateException {
            return null;
        }

        public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) throws IllegalArgumentException, IllegalStateException {
            return null;
        }

        public <T extends Filter> T createFilter(Class<T> tClass) throws ServletException {
            return null;
        }

        public FilterRegistration getFilterRegistration(String s) {
            return null;
        }

        public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
            return null;
        }

        public void addListener(Class<? extends EventListener> aClass) {

        }

        public void addListener(String s) {

        }

        public <T extends EventListener> void addListener(T t) {

        }

        public <T extends EventListener> T createListener(Class<T> tClass) throws ServletException {
            return null;
        }

        public void declareRoles(String... strings) {

        }

        public SessionCookieConfig getSessionCookieConfig() {
            return null;
        }

        public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

        }

        public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
            return null;
        }

        public int getEffectiveMajorVersion() throws UnsupportedOperationException {
            return 0;
        }

        public int getEffectiveMinorVersion() throws UnsupportedOperationException {
            return 0;
        }

        public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
            return null;
        }

        public ClassLoader getClassLoader() {
            return null;
        }

        public JspConfigDescriptor getJspConfigDescriptor() {
            return null;
        }

        public void log(String message, Throwable throwable) {
        }

        public void log(String msg) {
        }

        public void removeAttribute(String name) {
        }

        public void setAttribute(String name, Object object) {
        }

        public Servlet getServlet(String name) throws ServletException {
            return null;
        }

        public Enumeration<String> getServletNames() {
            return null;
        }

        public Enumeration<Servlet> getServlets() {
            return null;
        }

        public void log(Exception exception, String msg) {
        }
    }

    // Dummy HttpProcessor implementation for testing.
    public static class TestHttpProcessor implements HttpProcessor {

        public SslParameters getSsl() {
            return null;
        }

        public String getAuthMethod() {
            return null;
        }

        public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
            
        }

    }
}
