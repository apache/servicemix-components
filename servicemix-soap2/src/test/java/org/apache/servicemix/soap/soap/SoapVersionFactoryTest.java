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
package org.apache.servicemix.soap.soap;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.soap.bindings.soap.Soap11;
import org.apache.servicemix.soap.bindings.soap.Soap12;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.bindings.soap.SoapVersionFactory;

public class SoapVersionFactoryTest extends TestCase {
    
    public void testSoap11() {
        SoapVersion sv = SoapVersionFactory.getInstance().getSoapVersion(Soap11.SOAP_NAMESPACE);
        assertNotNull(sv);
        assertEquals(Soap11.SOAP_NAMESPACE, sv.getNamespace());
        assertEquals(Soap11.SOAP_DEFAULT_PREFIX, sv.getPrefix());
    }

    public void testSoap12() {
        SoapVersion sv = SoapVersionFactory.getInstance().getSoapVersion(Soap12.SOAP_NAMESPACE);
        assertNotNull(sv);
        assertEquals(Soap12.SOAP_NAMESPACE, sv.getNamespace());
        assertEquals(Soap12.SOAP_DEFAULT_PREFIX, sv.getPrefix());
    }
    
    public void testUnkown() {
        SoapVersion sv = SoapVersionFactory.getInstance().getSoapVersion("urn:soap");
        assertNull(sv);
    }
    
    public void testDerivedSoap11() {
        SoapVersion sv = Soap11.getInstance().getDerivedVersion("S");
        assertNotNull(sv);
        assertEquals(Soap11.SOAP_NAMESPACE, sv.getNamespace());
        assertEquals("S", sv.getPrefix());
    }

    public void testDerivedSoap12() {
        SoapVersion sv = Soap12.getInstance().getDerivedVersion("S");
        assertNotNull(sv);
        assertEquals(Soap12.SOAP_NAMESPACE, sv.getNamespace());
        assertEquals("S", sv.getPrefix());
    }
    
    public void testDerivedSoap11FromFactory() {
        SoapVersion sv = SoapVersionFactory.getInstance().getSoapVersion(
                        new QName(Soap11.SOAP_NAMESPACE, "Envelope", "S"));
        assertNotNull(sv);
        assertEquals(Soap11.SOAP_NAMESPACE, sv.getNamespace());
        assertEquals("S", sv.getPrefix());
    }

    public void testDerivedSoap12FromFactory() {
        SoapVersion sv = SoapVersionFactory.getInstance().getSoapVersion(
                        new QName(Soap12.SOAP_NAMESPACE, "Envelope", "S"));
        assertNotNull(sv);
        assertEquals(Soap12.SOAP_NAMESPACE, sv.getNamespace());
        assertEquals("S", sv.getPrefix());
    }

}
