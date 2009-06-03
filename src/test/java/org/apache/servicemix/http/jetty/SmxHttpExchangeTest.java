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
package org.apache.servicemix.http.jetty;

import java.io.InputStream;
import java.io.Reader;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.io.ByteArrayBuffer;

public class SmxHttpExchangeTest extends TestCase {

    private static transient Log log = LogFactory.getLog(SmxHttpExchangeTest.class);
    private static final String STRRESPONSECONTENT = "valid response content";
    private SmxHttpExchange httpExchange;

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
        assertTrue("", returnedContent.equalsIgnoreCase(STRRESPONSECONTENT));
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
        ByteArrayBuffer contentBuffer = new ByteArrayBuffer(STRRESPONSECONTENT);

        // set the exchange's responseContent
        httpExchange.onResponseContent(contentBuffer);
    }
}
