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
package org.apache.servicemix.soap;

import javax.xml.namespace.QName;

import org.apache.servicemix.soap.marshalers.SoapMarshaler;

import junit.framework.TestCase;

public class SoapFaultTest extends TestCase {

	private SoapFault soapFault;
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	// test translateCodeTo11() when code is null
	public void testTranslateCodeTo11CodeNull() throws Exception {
		soapFault = new SoapFault(null, null);
		
		soapFault.translateCodeTo11();
		
		assertTrue("translateCodeTo11() should return \"Server\" for null code", 
				soapFault.getCode().equals(SoapMarshaler.SOAP_11_CODE_SERVER));
	}
	
	// test translateCodeTo11() when both code and subcode are set.
	public void testTranslateCodeTo11CodeAndSubcodeSet() throws Exception {
		QName subcode = new QName("http://test/service", "Memory");
		soapFault = new SoapFault(SoapMarshaler.SOAP_12_CODE_SENDER, 
				subcode, "Out of Memory");
		
		soapFault.translateCodeTo11();
		
		assertTrue("code should be set to subcode", soapFault.getCode().equals(subcode));
	}
	
	// test translateCodeTo11() with code set but subcode is null.
	public void testTranslateCodeTo11CodeSet() throws Exception {
		soapFault = new SoapFault(SoapMarshaler.SOAP_12_CODE_DATAENCODINGUNKNOWN, 
				"unknown encoding type");

		soapFault.translateCodeTo11();
		
		assertTrue("code should have a code of Client", soapFault.getCode().equals(SoapMarshaler.SOAP_11_CODE_CLIENT));
	}
	
	// test translateCodeTo12() when code is null
	public void testTranslateCodeTo12CodeNull() throws Exception {
		soapFault = new SoapFault(null, null);
		
		soapFault.translateCodeTo12();
		
		assertTrue("null code for SOAP 1.2 should be set to Receiver",
				soapFault.getCode().equals(SoapMarshaler.SOAP_12_CODE_RECEIVER));
	}
	
	// test translateCodeTo12() when code is Server.
	public void testTranslateCodeTo12CodeServer() throws Exception {
		soapFault = new SoapFault(SoapMarshaler.SOAP_11_CODE_SERVER, "test reason");
		
		soapFault.translateCodeTo12();
		
		assertTrue("code should be changed to Receiver", 
				soapFault.getCode().equals(SoapMarshaler.SOAP_12_CODE_RECEIVER));
	}
	
	// test translateCodeTo12() when code is Client.
	public void testTranslateCodeTo12CodeClient() throws Exception {
		soapFault = new SoapFault(SoapMarshaler.SOAP_11_CODE_CLIENT, "test reason");
		
		soapFault.translateCodeTo12();
		
		assertTrue("code should be changed to Sender", 
				soapFault.getCode().equals(SoapMarshaler.SOAP_12_CODE_SENDER));
	}
}
