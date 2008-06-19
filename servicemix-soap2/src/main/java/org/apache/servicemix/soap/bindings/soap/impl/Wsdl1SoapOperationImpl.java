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

import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapMessage;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapOperation;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding.Style;
import org.apache.servicemix.soap.core.model.AbstractWsdl1Operation;

public class Wsdl1SoapOperationImpl extends AbstractWsdl1Operation<Wsdl1SoapMessage> implements Wsdl1SoapOperation {

    private String soapAction;
    private Style style;
    private String transport;

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
     * @return the style
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    /**
     * @return the transport
     */
    public String getTransport() {
        return transport;
    }

    /**
     * @param transport the transport to set
     */
    public void setTransport(String transport) {
        this.transport = transport;
    }

}
