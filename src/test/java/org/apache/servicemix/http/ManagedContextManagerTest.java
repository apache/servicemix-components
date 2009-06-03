package org.apache.servicemix.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;

import junit.framework.TestCase;

public class ManagedContextManagerTest extends TestCase {

	private static transient Log log = LogFactory.getLog(ManagedContextManagerTest.class);
	private ManagedContextManager server;
	private HttpConfiguration config;

	protected void setUp() throws Exception {
		super.setUp();
		config = new HttpConfiguration();
		server = new ManagedContextManager();
		server.setConfiguration(config);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		server.shutDown();
		server = null;
		config = null;
	}
	
	// Test createContext with a path that does not begin with a / and
	// does not end with a /.
	public void testCreateContext() throws Exception {
		server.init();
		server.start();
		
		TestHttpProcessor httpProcessor = new TestHttpProcessor();
		String strUrl = "path/to/some/resource";
		
		String returnedPath = (String)server.createContext(strUrl, httpProcessor);
		
		assertTrue("Context path should have / at end and at the beginning", 
				returnedPath.equals("/" + strUrl + "/"));
	}
	
	// Test getMainProcessor with an unmanaged config - should throw IllegalStateException
	public void testGetMainProcessorUnmanagedConfig() throws Exception {
		server.init();
		server.start();
		
		try {
			server.getMainProcessor();
			fail("getMainProcessor() should fail for unmanaged config.");
		} catch (IllegalStateException ise) {
			// test succeeds
			log.info("testGetMainProcessorUnmanagedConfig() threw the expected exception");
		}
	}
/*	
	public void testProcessorProcess() throws Exception {
		server.init();
		server.start();
		
		Request request = new Request();
		request.setUri(new HttpURI("/"));
		request.setMethod("GET");
		request.setProtocol("http");
		TestResponse response = new TestResponse();
		
		TestHttpProcessor httpProcessor = new TestHttpProcessor();
		String strUrl = "/path/to/some/resource/";
		
		server.createContext(strUrl, httpProcessor);
		
		server.getConfiguration().setManaged(true);
		
		server.getMainProcessor().process(request, response);
		
		assertTrue("", response.getStatus() == HttpServletResponse.SC_NOT_FOUND);
		
	}
*/	
    public static class TestHttpProcessor implements HttpProcessor {
        public SslParameters getSsl() {
            return null;
        }

        public String getAuthMethod() {
            return null;
        }

        public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
            log.info(request);
        }

    }
    
}
