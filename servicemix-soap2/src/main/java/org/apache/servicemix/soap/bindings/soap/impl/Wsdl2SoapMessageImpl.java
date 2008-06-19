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
package org.apache.servicemix.soap.bindings.soap.impl;

import java.util.List;

import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpHeader;
import org.apache.servicemix.soap.bindings.soap.model.wsdl2.Wsdl2SoapHeader;
import org.apache.servicemix.soap.bindings.soap.model.wsdl2.Wsdl2SoapMessage;
import org.apache.servicemix.soap.bindings.soap.model.wsdl2.Wsdl2SoapModule;
import org.apache.servicemix.soap.core.model.AbstractWsdl2Message;

public class Wsdl2SoapMessageImpl extends AbstractWsdl2Message implements Wsdl2SoapMessage {

    private List<Wsdl2SoapModule> soapModules;
    private List<Wsdl2SoapHeader> soapHeaders;
    private List<Wsdl2HttpHeader> httpHeaders;

    /**
     * @return the soapModules
     */
    public List<Wsdl2SoapModule> getSoapModules() {
        return soapModules;
    }

    /**
     * @param soapModules the soapModules to set
     */
    public void setSoapModules(List<Wsdl2SoapModule> soapModules) {
        this.soapModules = soapModules;
    }

    /**
     * @return the soapHeaders
     */
    public List<Wsdl2SoapHeader> getSoapHeaders() {
        return soapHeaders;
    }

    /**
     * @param soapHeaders the soapHeaders to set
     */
    public void setSoapHeaders(List<Wsdl2SoapHeader> soapHeaders) {
        this.soapHeaders = soapHeaders;
    }

    /**
     * @return the httpHeaders
     */
    public List<Wsdl2HttpHeader> getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * @param httpHeaders the httpHeaders to set
     */
    public void setHttpHeaders(List<Wsdl2HttpHeader> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }
    
}
