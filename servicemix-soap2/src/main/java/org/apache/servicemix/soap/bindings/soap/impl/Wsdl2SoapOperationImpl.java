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

import java.net.URI;

import org.apache.servicemix.soap.bindings.soap.model.wsdl2.Wsdl2SoapMessage;
import org.apache.servicemix.soap.bindings.soap.model.wsdl2.Wsdl2SoapOperation;
import org.apache.servicemix.soap.core.model.AbstractWsdl2Operation;

public class Wsdl2SoapOperationImpl extends AbstractWsdl2Operation<Wsdl2SoapMessage> implements Wsdl2SoapOperation {

    private String soapAction;
    private URI soapMep;

    /**
     * @return the soapAction
     */
    public String getSoapAction() {
        return soapAction;
    }

    /**
     * @param soapAction the soapAction to set
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    /**
     * @return the soapMep
     */
    public URI getSoapMep() {
        return soapMep;
    }

    /**
     * @param soapMep the soapMep to set
     */
    public void setSoapMep(URI soapMep) {
        this.soapMep = soapMep;
    }
    
}
