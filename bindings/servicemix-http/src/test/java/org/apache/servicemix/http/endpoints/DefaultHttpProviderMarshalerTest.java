package org.apache.servicemix.http.endpoints;

import java.util.HashMap;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.expression.PropertyExpression;
import org.apache.servicemix.http.jetty.SmxHttpExchange;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.mock.MockExchangeFactory;
import org.mortbay.jetty.HttpMethods;

import junit.framework.TestCase;

public class DefaultHttpProviderMarshalerTest extends TestCase {

        String port1 = System.getProperty("http.port1");
        
	private DefaultHttpProviderMarshaler defHttpProviderMarshaler;
	
	protected void setUp() throws Exception {
		super.setUp();
		defHttpProviderMarshaler = new DefaultHttpProviderMarshaler();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		defHttpProviderMarshaler = null;
	}
	
	// Test getLocationUri when URI on message exchange is null.
	public void testGetLocationUriNull() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        NormalizedMessage msg = me.createMessage();
        msg.setContent(new StringSource("<input>input message</input>"));
        
        defHttpProviderMarshaler.setLocationURI(null);
        
        try {
        	defHttpProviderMarshaler.getLocationUri(me, msg);
        	fail("getLocationUri() should throw an exception for null URI");
        } catch (IllegalStateException ise) {
        	// test succeeds
        }
	}
	
	// Test getLocationUri when LocationURIExpression is set.
	public void testGetLocationUriExpression() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setProperty("uri", "someOperation");
        NormalizedMessage msg = me.createMessage();
        msg.setContent(new StringSource("<input>input message</input>"));
        
        PropertyExpression uriExp = new PropertyExpression("uri");
        
        defHttpProviderMarshaler.setLocationURIExpression(uriExp);
        
        String uriReturned = defHttpProviderMarshaler.getLocationUri(me, msg);
        
        assertTrue("getLocationUri() should return the string value set on the exchange", 
        		uriReturned.equals("someOperation"));
	}

	// Test getMethod when in msg has no content.  Method should be HTTP GET. 
	public void testGetMethodInMsgContentNull() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        NormalizedMessage msg = me.createMessage();
        
        String httpMethod = defHttpProviderMarshaler.getMethod(me, msg);
        
        assertTrue("getMethod() with null in msg contents should return GET", 
        		httpMethod.equals(HttpMethods.GET));
	}
	
	// Test getMethod when a methodExpression is set on the message exchange.
	public void testGetMethodWithMethodExpression() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setProperty("method", "POST");
        NormalizedMessage msg = me.createMessage();
        msg.setContent(new StringSource("<input>input message</input>"));
        
        PropertyExpression methodExp = new PropertyExpression("method");
        
        defHttpProviderMarshaler.setMethodExpression(methodExp);
        
        String httpMethod = defHttpProviderMarshaler.getMethod(me, msg);
        
        assertTrue("getMethod() with method expression should return the value set on the exchange", 
        		httpMethod.equals(HttpMethods.POST));
	}
	
	// Test getContentType when content type is null.
	public void testGetContentTypeNull() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        NormalizedMessage msg = me.createMessage();

        // Must explicitly set contentType to null.  It is set by default.
        defHttpProviderMarshaler.setContentType(null);
        
        try {
        	defHttpProviderMarshaler.getContentType(me, msg);
        	fail("getContentType() should throw an exception when contentType is null");
        } catch (IllegalStateException ise) {
        	// test passes
        }
	}
	
	// Test getContentType when contentTypeExpression is set on the exchange.
	public void testGetContentTypeExpression() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setProperty("contentType", "text/plain");
        NormalizedMessage msg = me.createMessage();
        msg.setContent(new StringSource("<input>input message</input>"));
        
        PropertyExpression contentTypeExp = new PropertyExpression("contentType");
        
        defHttpProviderMarshaler.setContentTypeExpression(contentTypeExp);
        
        String contentType = defHttpProviderMarshaler.getContentType(me, msg);
        
        assertTrue("getContentType() should return the value set on the exchange", 
        		contentType.equals("text/plain"));
        
	}
	
	// Test createRequest when headers are set.
	public void testCreateRequestWithHeaders() throws Exception {
		MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setProperty("uri", "http://localhost:"+port1+"/Service1/someOperation");
        NormalizedMessage msg = me.createMessage();
        msg.setContent(new StringSource("<input>input message</input>"));
        SmxHttpExchange httpExchange = new SmxHttpExchange();
        
        PropertyExpression uriExp = new PropertyExpression("uri");
        
        // Create a header to add to the exchange.
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", "no-cache");
        
        defHttpProviderMarshaler.setHeaders(headers);
        defHttpProviderMarshaler.setLocationURIExpression(uriExp);
        
        defHttpProviderMarshaler.createRequest(me, msg, httpExchange);
        
        // Create request sets the request content on the httpExchange.
        assertNotNull("createRequest() should set the http exchange's request content", 
        		httpExchange.getRequestContent());
	}
}
