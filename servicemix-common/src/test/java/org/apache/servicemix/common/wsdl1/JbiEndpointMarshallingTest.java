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
package org.apache.servicemix.common.wsdl1;

import java.net.URI;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import org.apache.servicemix.common.scheduler.Scheduler;

public class JbiEndpointMarshallingTest extends TestCase {

    private static transient Log logger =  LogFactory.getLog(JbiEndpointMarshallingTest.class);
    
    public static final String NS_URI_JBI = "http://servicemix.org/wsdl/jbi/";

    public static final String ELEM_ENDPOINT = "endpoint";
    
    public static final QName Q_ELEM_JBI_ENDPOINT = new QName(NS_URI_JBI, ELEM_ENDPOINT);
           
    private QName SvcName = new QName("http://apache.org/hello_world_soap_http_provider",
    		"SOAPService", "");
    
    private QName defaultOp = new QName("http://schemas.xmlsoap.org/wsdl/", "sayHi", "");
    
    
    // Test to DeSerialize JBI endpoints 
    public void testDeSerialize() throws Exception {
    	ExtensionRegistry extReg = new ExtensionRegistry();    	
    	JbiExtension.register(extReg);
    	    	    	
    	WSDLFactory factory = WSDLFactory.newInstance();
    	WSDLReader reader = factory.newWSDLReader();    	   		
    	reader.setFeature("javax.wsdl.verbose", true);
    	reader.setFeature("javax.wsdl.importDocuments", true);    	
    	reader.setExtensionRegistry(extReg);
    	
    	java.net.URL tmp = null;
        try {
            tmp = JbiEndpointMarshallingTest.class.getClassLoader().getResource(
                "org/apache/servicemix/common/wsdl1/helloworld.wsdl"                 
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
    	
    	Definition wsdlDefinition = reader.readWSDL(tmp.toString());
    	Service svc = wsdlDefinition.getService(SvcName);
    	Port port = svc.getPort("SoapPort");
    	List extElements  = port.getExtensibilityElements();    
    	
    	//Tests for role=provider, MEP=Default
    	JbiEndpoint jbiEndpoint = (JbiEndpoint)extElements.get(0);    	
    	assertEquals(javax.jbi.messaging.MessageExchange.Role.PROVIDER, jbiEndpoint.getRole().PROVIDER);
    	assertEquals(defaultOp, jbiEndpoint.getDefaultOperation());
    	assertNotNull(jbiEndpoint.toString());
    	
    	//Tests for role=consumer,MEP=in-only
    	jbiEndpoint = (JbiEndpoint)extElements.get(1);
    	assertEquals(javax.jbi.messaging.MessageExchange.Role.CONSUMER, jbiEndpoint.getRole().CONSUMER);
    	assertEquals(new URI("http://www.w3.org/2004/08/wsdl/in-only"), jbiEndpoint.getDefaultMep());    	
    	
    	//Tests for role=provider, MEP=robust-in-only
    	jbiEndpoint = (JbiEndpoint)extElements.get(2);
    	assertEquals(javax.jbi.messaging.MessageExchange.Role.PROVIDER, jbiEndpoint.getRole().PROVIDER); 
    	assertEquals(new URI("http://www.w3.org/2004/08/wsdl/robust-in-only"), jbiEndpoint.getDefaultMep());
    	
    	//Tests for role=provider, MEP=in-out
    	jbiEndpoint = (JbiEndpoint)extElements.get(3);
    	assertEquals(javax.jbi.messaging.MessageExchange.Role.PROVIDER, jbiEndpoint.getRole().PROVIDER);
    	assertEquals(new URI("http://www.w3.org/2004/08/wsdl/in-out"), jbiEndpoint.getDefaultMep());
    	
    }    
        
    public void testNoRoleDeserialize() throws Exception {
    	ExtensionRegistry extReg = new ExtensionRegistry();    	
    	JbiExtension.register(extReg);
    	    	    	
    	WSDLFactory factory = WSDLFactory.newInstance();
    	WSDLReader reader = factory.newWSDLReader();    	   		
    	reader.setFeature("javax.wsdl.verbose", true);
    	reader.setFeature("javax.wsdl.importDocuments", true);    	
    	reader.setExtensionRegistry(extReg);
    	
    	java.net.URL tmp = null;
        try {
            tmp = JbiEndpointMarshallingTest.class.getClassLoader().getResource(
                "org/apache/servicemix/common/wsdl1/no-role.wsdl"                 
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
    	
        try {
    	    reader.readWSDL(tmp.toString());
    	    fail();
        } catch (WSDLException ex) {
    		// Should catch this exception - Role must be specified");
        }
        
        tmp = null;
        try {
            tmp = JbiEndpointMarshallingTest.class.getClassLoader().getResource(
                "org/apache/servicemix/common/wsdl1/different-role.wsdl"                 
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        
        try {
    	    reader.readWSDL(tmp.toString());
    	    fail();
        } catch (WSDLException ex) {
    		// Should catch this exception - Unrecognised role: producer");
        }            	  
    }
    
    // Test to Serialize JBI endpoints
    public void testSerialize() throws Exception {    	
    	ExtensionRegistry extReg = new ExtensionRegistry();
    	JbiExtension.register(extReg);
    	
    	WSDLFactory factory = WSDLFactory.newInstance();
    	WSDLReader reader = factory.newWSDLReader();    	   		
    	reader.setFeature("javax.wsdl.verbose", true);
    	reader.setFeature("javax.wsdl.importDocuments", true);    	
    	reader.setExtensionRegistry(extReg);
    	
    	java.net.URL tmp = null;
        try {
            tmp = JbiEndpointMarshallingTest.class.getClassLoader().getResource(
                "org/apache/servicemix/common/wsdl1/helloworld.wsdl"                 
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
    	
    	Definition wsdlDefinition = reader.readWSDL(tmp.toString());
    	Service svc = wsdlDefinition.getService(SvcName);
    	Port port = svc.getPort("SoapPort");

    	JbiEndpoint jbiExt = (JbiEndpoint)extReg.createExtension(javax.wsdl.Port.class, 
                                                                 Q_ELEM_JBI_ENDPOINT);
    	
    	jbiExt.setRequired(true);
    	jbiExt.setRole(javax.jbi.messaging.MessageExchange.Role.PROVIDER);
    	jbiExt.setDefaultMep(new URI("http://www.w3.org/2004/08/wsdl/in-out"));
    	assertTrue(jbiExt.getRequired());

        port.addExtensibilityElement(jbiExt);

    	
    	WSDLWriter writer = factory.newWSDLWriter();
    	wsdlDefinition.setExtensionRegistry(extReg);
    	writer.writeWSDL(wsdlDefinition, System.out);
    	System.out.println("Test finished");    	    	 
    	
    }        

    
}
