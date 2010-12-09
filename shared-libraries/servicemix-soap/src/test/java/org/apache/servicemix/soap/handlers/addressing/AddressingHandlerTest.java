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
package org.apache.servicemix.soap.handlers.addressing;

import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.jbi.util.WSAddressingConstants;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import junit.framework.TestCase;

public class AddressingHandlerTest extends TestCase {


	private AddressingHandler handler;
	private static final String WSA_NS = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

	public AddressingHandlerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		this.handler = new AddressingHandler();
	}
	
	public void testCreateHeader() throws Exception {
		QName messageIdQN = new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_MESSAGE_ID, WSAddressingConstants.WSA_PREFIX);
		String messageId = "uuid:1234567890";
		DocumentFragment wsaMessageId = this.handler.createHeader(messageIdQN, messageId);

		assertNotNull("DocumentFragment is null", wsaMessageId);
		assertEquals("messageId", messageId, wsaMessageId.getTextContent());
		
	}
	
	public void testWSAEmptyPrefix() throws Exception {
		// setup
		QName messageIdQN = new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_MESSAGE_ID, "");
		QName relatesToQN = new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_RELATES_TO, "");

		
		// create messages and add them to the context
		Context msgContext = new Context();
		SoapMessage inMessage = new SoapMessage();
		SoapMessage outMessage = new SoapMessage();
		msgContext.setInMessage(inMessage);
		msgContext.setOutMessage(outMessage);
		
		// add wsa MessageID header to in message
		String messageId = "uuid:1234567890";
		DocumentFragment wsaMessageId = this.handler.createHeader(messageIdQN, messageId);
		inMessage.addHeader(messageIdQN, wsaMessageId);
		
		// run handler
		this.handler.onReply(msgContext);
		
		// verify relates-to
		DocumentFragment wsaRelatesTo = (DocumentFragment) outMessage.getHeaders().get(relatesToQN);
		assertNotNull("No RelatesTo header", wsaRelatesTo);
		assertEquals("Value", messageId, wsaRelatesTo.getTextContent());
	}
	
	public void testWSAPrefix() throws Exception {
		// setup
		QName messageIdQN = new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_MESSAGE_ID, WSAddressingConstants.WSA_PREFIX);
		QName relatesToQN = new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_RELATES_TO, WSAddressingConstants.WSA_PREFIX);

		
		// create messages and add them to the context
		Context msgContext = new Context();
		SoapMessage inMessage = new SoapMessage();
		SoapMessage outMessage = new SoapMessage();
		msgContext.setInMessage(inMessage);
		msgContext.setOutMessage(outMessage);
		
		// add wsa MessageID header to in message
		String messageId = "uuid:1234567890";
		DocumentFragment wsaMessageId = this.handler.createHeader(messageIdQN, messageId);
		inMessage.addHeader(messageIdQN, wsaMessageId);
		
		// run handler
		this.handler.onReply(msgContext);
		
		// verify relates-to
		DocumentFragment wsaRelatesTo = (DocumentFragment) outMessage.getHeaders().get(relatesToQN);
		assertNotNull("No RelatesTo header", wsaRelatesTo);
		assertEquals("Value", messageId, wsaRelatesTo.getTextContent());
	}
	
	// test onReceive() when wsa:Action and wsa:To are set on the incoming message.
	// The appropriate parts of the Context that is passed in should be set.
	public void testOnReceiveActionAndTo() throws Exception {
		Context msgContext = new Context();
		SoapMessage soapMessage = new SoapMessage();
		soapMessage.setBodyName(new QName("http://test.org", "echo"));
		soapMessage.addHeader(new QName(WSA_NS, "To"), 
				createDocumentFragment("To", "http://localhost:8192/Services/EchoService"));
		soapMessage.addHeader(new QName(WSA_NS, "Action"), 
				createDocumentFragment("Action", "http://test.org/Echo/EchoService"));
		soapMessage.setSource(new StringSource(
				"<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\" +" +
				"<env:Body><ns1:EchoRequest xmlns:ns1=\"http://test.org\"><echo>this string</echo>" +
				"</ns1:EchoRequest></env:Body></env:Envelope>"));
		
		msgContext.setInMessage(soapMessage);
		
		this.handler.onReceive(msgContext);
		
		assertNotNull("Endpoint on Context should be set", msgContext.getProperty(Context.ENDPOINT));
		assertNotNull("Service on Context should be set", msgContext.getProperty(Context.SERVICE));
		assertNotNull("Operation on Context should be set", msgContext.getProperty(Context.OPERATION));
		assertNotNull("Interface on Context should be set", msgContext.getProperty(Context.INTERFACE));
	}
	
	private DocumentFragment createDocumentFragment(String headerName, String headerValue) throws Exception {
		DocumentFragment df = null;
		Document doc = DOMUtil.newDocument();
		df = doc.createDocumentFragment();
		//Element e = doc.createElementNS(WSA_NS, headerName);
		Element e = doc.createElement(headerName);
		Text t = doc.createTextNode(headerValue);
		e.appendChild(t);
		df.appendChild(e);
		return df;
	}

}
