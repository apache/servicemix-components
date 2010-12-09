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

import java.util.HashMap;
import java.util.Map;

import org.apache.servicemix.soap.marshalers.SoapMessage;

/**
 * 
 * @author Guillaume Nodet
 * @version $Revision: 1.5 $
 * @since 3.0
 */
public class Context {

    public static final String SOAP_IN = "org.apache.servicemix.SoapIn";
    public static final String SOAP_OUT = "org.apache.servicemix.SoapOut";
    public static final String SOAP_FAULT = "org.apache.servicemix.SoapFault";
	public static final String INTERFACE = "org.apache.servicemix.Interface";
	public static final String OPERATION = "org.apache.servicemix.Operation";
	public static final String SERVICE = "org.apache.servicemix.Service";
	public static final String ENDPOINT = "org.apache.servicemix.Endpoint";
    
    public static final String AUTHENTICATION_SERVICE = "org.apache.servicemix.AuthenticationService";
    public static final String KEYSTORE_MANAGER = "org.apache.servicemix.KeystoreManager";
	
	private Map properties;
	
	public Context() {
		this.properties = new HashMap();
	}
    
    public SoapMessage getInMessage() {
        return (SoapMessage) getProperty(SOAP_IN);
    }
    
    public SoapMessage getOutMessage() {
        return (SoapMessage) getProperty(SOAP_OUT);
    }
    
    public SoapMessage getFaultMessage() {
        return (SoapMessage) getProperty(SOAP_FAULT);
    }
    
    public void setInMessage(SoapMessage message) {
        setProperty(SOAP_IN, message);
    }
    
    public void setOutMessage(SoapMessage message) {
        setProperty(SOAP_OUT, message);
    }
    
    public void setFaultMessage(SoapMessage message) {
        setProperty(SOAP_FAULT, message);
    }
    
	public Object getProperty(String name) {
		return properties.get(name);
	}
	
	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}
	
}
