package org.apache.servicemix.http;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;

import junit.framework.TestCase;

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
			assertTrue("ServletException does not contain the expected error message", 
					errorMsg.contains("No binding property available"));
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

		public Enumeration<Object> getAttributeNames() {
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

		public Enumeration<Servlet> getServletNames() {
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
