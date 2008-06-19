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
package org.apache.servicemix.soap.wsdl;

import java.util.Iterator;
import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;

import org.apache.servicemix.soap.api.model.Binding;

public class Wsdl1BindingFactory {

    public static Binding<?> createBinding(javax.wsdl.Port wsdlPort) {
        List elements = wsdlPort.getExtensibilityElements();
        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) iter.next();
            if (element instanceof SOAPAddress) {
                return Wsdl1Soap11BindingFactory.createWsdl1SoapBinding(wsdlPort);
            } else if (element instanceof SOAP12Address) {
                return Wsdl1Soap12BindingFactory.createWsdl1SoapBinding(wsdlPort);
            }
        }
        return null;
    }

}
