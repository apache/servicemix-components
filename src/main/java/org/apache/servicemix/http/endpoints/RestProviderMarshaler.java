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
package org.apache.servicemix.http.endpoints;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Node;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.servicemix.expression.JAXPStringXPathExpression;
import org.apache.servicemix.http.jetty.SmxHttpExchange;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;

/**
 * Created by IntelliJ IDEA.
 * User: tpurcell
 * Date: Mar 12, 2008
 * Time: 3:37:47 PM
 *
 * Provides basic support for calling RESTful services using the servicemix-http JBI component
 */
public class RestProviderMarshaler extends DefaultHttpProviderMarshaler {
    private SourceTransformer transformer = new SourceTransformer();
    private JAXPStringXPathExpression contentExpression;

    /**
     * Returns the XPath expression that decribes the content to send.
     *
     * @return JAXPStringXPathExpression  XPath expression that decribes the content to send
     */
    public JAXPStringXPathExpression getContentExpression() {
        return contentExpression;
    }

    /**
     * Accepts the XPath expression that decribes the content to send.
     *
     * @param contentExpression JAXPStringXPathExpression that decribes the content to send
     */
    public void setContentExpression(JAXPStringXPathExpression contentExpression) {
        this.contentExpression = contentExpression;
    }

    public void createRequest(MessageExchange exchange, NormalizedMessage inMsg, SmxHttpExchange httpExchange) throws Exception {
        httpExchange.setURL(getLocationUri(exchange, inMsg));

        // Temporary fix for bug in jetty-client 6.1.5
        // http://fisheye.codehaus.org/browse/jetty-contrib/jetty/trunk/contrib/client/src/main/
        //                  java/org/mortbay/jetty/client/HttpConnection.java?r1=374&r2=378
        httpExchange.addRequestHeader(HttpHeaders.HOST_BUFFER, new ByteArrayBuffer(new URI(getLocationUri(exchange, inMsg)).getHost()));

        httpExchange.setMethod(getMethod(exchange, inMsg));
        httpExchange.setRequestHeader(HttpHeaders.CONTENT_TYPE, getContentType(exchange, inMsg));

        if (getHeaders() != null) {
            for (Map.Entry<String, String> e : getHeaders().entrySet()) {
                httpExchange.setRequestHeader(e.getKey(), e.getValue());
            }
        }

        if (contentExpression != null) {
            String contentToSend = applyContentExpression(exchange, inMsg);
            if (contentToSend != null) {
                httpExchange.setRequestContent(new ByteArrayBuffer(contentToSend.getBytes()));
            }
        } else if (inMsg.getContent() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.toResult(inMsg.getContent(), new StreamResult(baos));
            httpExchange.setRequestContent(new ByteArrayBuffer(baos.toByteArray()));
        }
    }

    /**
     * Calls evaluate on the XPath expression requesting a XPathConstants.NODE as the return type. This will make it
     * possible for a SourceTransformer to construct a full XML representation of the selected nodes.
     *
     * @param exchange  MessageExchange to use on MessageVariableResolver
     * @param inMsg     NormalizedMessage to use on MessageVariableResolver
     * @return String   Contains the XML items described by the provided XPath expression.
     */
    protected String applyContentExpression(MessageExchange exchange, NormalizedMessage inMsg) throws Exception {
        String content = null;

        if (contentExpression != null) {
            Node node = (Node) contentExpression.evaluate(exchange, inMsg, XPathConstants.NODE);
            content = transformer.toString(new DOMSource(node));
        }

        if (content == null) {
            throw new IllegalStateException("XPath expression failed. Unable to find Content for exchange");
        }
        return content;
    }

    /**
     * Accept the response from the RESTful service and pass it on to the NMR
     * @param exchange      MessageExchange to use on MessageVariableResolver
     * @param httpExchange  SmxHttpExchange which holds the response from the RESTful service
     * @throws Exception
     */
    public void handleResponse(MessageExchange exchange, SmxHttpExchange httpExchange) throws Exception {
        int response = httpExchange.getResponseStatus();

        if (exchange instanceof InOnly) {
            processInOnly(exchange, response);
        } else if (isSuccessful(response)) {
            NormalizedMessage msg = exchange.createMessage();
            msg.setContent(packageHttpResponse(httpExchange, response));
            exchange.setMessage(msg, "out");
        } else {
            Fault fault = exchange.createFault();
            fault.setContent(packageHttpResponse(httpExchange, response));
            exchange.setFault(fault);
        }

    }

    /**
     * Handles InOnly MEP. If the call was not successful there's noone to tell so throw an exception
     *
     * @param exchange      MessageExchange to use on MessageVariableResolver
     * @param response      HTTP Status Code returned by the RESTful service
     */
    private void processInOnly(MessageExchange exchange, int response) throws Exception {
        if (isSuccessful(response)) {
            exchange.setStatus(ExchangeStatus.DONE);
        } else {
            throw new Exception("Invalid status response: " + response);
        }
    }

    /**
     * Returns true for the following HTTP staus codes as successful:
     * OK                       200 OK
     * CREATED                  201 Created
     * ACCEPTED                 202 Accepted
     * NO_CONTENT               204 No Content
     *
     * False for all others.
     *
     * @param   httpStatusCode Code to evaluate.
     * @return  True for the above codes. False for all others.
     */
    public boolean isSuccessful(int httpStatusCode) {
        if (httpStatusCode == HttpStatus.SC_OK
                || httpStatusCode == HttpStatus.SC_CREATED
                || httpStatusCode == HttpStatus.SC_ACCEPTED
                || httpStatusCode == HttpStatus.SC_NO_CONTENT) {
            return true;
        }
        return false;
    }

    /**
     * Provides basic packaging for the response from the RESTful service. The XML returned has the following structure:
     *
     * <response>
     *    <http-status>
     *       <code>nnn</code>
     *    </http-status>
     *    <http-headers>
     *       <"header field name 1">header field value 1"</"header field name 1">
     *       <"header field name 2">header field value 2"</"header field name 2">
     *       ...
     *       <"header field name n">header field value n"</"header field name n">
     *    </http-headers>
     *    <content>
     *      "Actual HTTP content(if any)"
     *    </content>
     * </response>
     *
     * @param httpExchange  SmxHttpExchange which holds the response from the RESTful service
     * @param response      HTTP Status Code returned by the RESTful service
     * @return StreamSource
     */
    public StreamSource packageHttpResponse(SmxHttpExchange httpExchange, int response) throws UnsupportedEncodingException {
        StringBuffer responseBuffer = new StringBuffer();
        responseBuffer.append("<response>");

        responseBuffer.append("<http-status>");
        responseBuffer.append("<code>" + response + "</code>");
        responseBuffer.append("</http-status>");

        responseBuffer.append("<http-headers>");
        Iterator responseFields = httpExchange.getResponseFields().getFields();
        while (responseFields.hasNext()) {
            HttpFields.Field httpField = (HttpFields.Field) responseFields.next();
            responseBuffer.append("<");
            responseBuffer.append(httpField.getName());
            responseBuffer.append(">");
            responseBuffer.append(httpField.getValue());
            responseBuffer.append("</");
            responseBuffer.append(httpField.getName());
            responseBuffer.append(">");
        }
        responseBuffer.append("</http-headers>");

        if (httpExchange.getResponseData() != null) {
            responseBuffer.append("<content>");
            responseBuffer.append(httpExchange.getResponseContent());
            responseBuffer.append("</content>");
        } else {
            responseBuffer.append("<content/>");
        }

        responseBuffer.append("</response>");

        return new StreamSource(new InputStreamReader(new ByteArrayInputStream(responseBuffer.toString().getBytes()), "utf-8"));
    }
}
