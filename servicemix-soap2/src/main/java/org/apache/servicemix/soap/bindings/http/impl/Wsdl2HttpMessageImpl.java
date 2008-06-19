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
package org.apache.servicemix.soap.bindings.http.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpHeader;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpMessage;
import org.apache.servicemix.soap.core.model.AbstractWsdl2Message;

public class Wsdl2HttpMessageImpl extends AbstractWsdl2Message implements Wsdl2HttpMessage {

    private String httpTransferCoding;
    private List<Wsdl2HttpHeader> httpHeaders;
    
    public Wsdl2HttpMessageImpl() {
        httpHeaders = new ArrayList<Wsdl2HttpHeader>();
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
    public void addHttpHeader(Wsdl2HttpHeader httpHeader) {
        this.httpHeaders.add(httpHeader);
    }
    /**
     * @return the httpTransferCoding
     */
    public String getHttpTransferCoding() {
        return httpTransferCoding;
    }
    /**
     * @param httpTransferCoding the httpTransferCoding to set
     */
    public void setHttpTransferCoding(String httpTransferCoding) {
        this.httpTransferCoding = httpTransferCoding;
    }
    
}
