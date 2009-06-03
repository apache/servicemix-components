package org.apache.servicemix.http.jetty;

import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.io.ByteArrayBuffer;

import junit.framework.TestCase;

public class SmxHttpExchangeTest extends TestCase {

	private static transient Log log = LogFactory.getLog(SmxHttpExchangeTest.class);
	private SmxHttpExchange httpExchange;
	private static final String strResponseContent = "valid response content";
	
	protected void setUp() throws Exception {
		super.setUp();
		httpExchange = new SmxHttpExchange();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		httpExchange = null;
	}

	// Test getResponseStatus when it throws an IllegalStateException
	public void testGetResponseStatusException() throws Exception {
		// set the response status
		httpExchange.onResponseStatus(null, 0, null);
		
		try {
			httpExchange.getResponseStatus();
			fail("getResponseStatus() should fail with IllegalStateException");
		} catch (IllegalStateException ise) {
			log.info("testGetResponseStatusException() got the expected exception");
		}
	}
	
	// Test getResponseFields when it throws an IllegalStateException
	public void testGetResponseFieldsException() throws Exception {
		
		// set the response status
		httpExchange.onResponseStatus(null, 0, null);
		
		try {
			httpExchange.getResponseFields();
			fail("getResponseFields() should fail with IllegalStateException");
		} catch (IllegalStateException ise) {
			log.info("testGetResponseFieldsException() got the expected exception");
		}
	}
	
	// Test getResponseContent when responseContent is null.
	public void testGetResponseContentNull() throws Exception {
		
		assertNull("getResponseContent() should return null", httpExchange.getResponseContent());
	}
	
	// Test getResponseContent when responseContent is returned as a string.
	public void testGetResponseContentString() throws Exception {
		
		// set the response content
		setResponseContent();
		
		String returnedContent = httpExchange.getResponseContent();
		
		assertTrue("", returnedContent.equalsIgnoreCase(strResponseContent));
	}
	
	// Test getResponseReader when responseContent is null.
	public void testGetResponseReaderNull() throws Exception {		
		assertNull("getResponseReader() should return null", httpExchange.getResponseReader());
	}
	
	// Test getResponseReader when responseContent is returned as a reader.
	public void testGetResponseReader() throws Exception {

		// set the response content
		setResponseContent();
		
		Reader inReader = httpExchange.getResponseReader();
		
		assertNotNull("getResponseReader() should return a Reader", inReader);
	}
	
	// Test getResponseStream when responseContent is null.
	public void testGetResponseStreamNull() throws Exception {

		assertNull("getResponseStream() should return null", httpExchange.getResponseStream());		
	}
	
	// Test getResponseStream when responseContent is returned as an InputStream.
	public void testGetResponseStream() throws Exception {
		
		// set the response content
		setResponseContent();
		
		InputStream inStream = httpExchange.getResponseStream();
		
		assertNotNull("getResponseStream() should return an InputStream", inStream);
	}
	
	// Test getResponseData when responseContent is null.
	public void testGetResponseDataNull() throws Exception {

		assertNull("getResponseData() should return null", httpExchange.getResponseData());
	}
	
	// Test getResponseData when responseContent is returned as a byte array.
	public void testGetResponseData() throws Exception {
		
		// set the response content
		setResponseContent();
		
		byte[] byteArray = httpExchange.getResponseData();
		
		assertTrue("getResponseData() should return more than 0 bytes", byteArray.length > 0);
	}
	
	// Method used for test setup to set the exchange's responseContent 
	//for getResponse<type> tests.
	private void setResponseContent() throws Exception {
		ByteArrayBuffer contentBuffer = new ByteArrayBuffer(strResponseContent);
		
		// set the exchange's responseContent
		httpExchange.onResponseContent(contentBuffer);
	}
}
