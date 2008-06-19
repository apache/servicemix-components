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
package org.apache.servicemix.soap.bindings.soap.interceptors;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.Interceptor;
import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapConstants;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapInterceptor;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapOperation;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapPart;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

public class MustUnderstandInterceptor extends AbstractInterceptor {

    public MustUnderstandInterceptor() {
    }

    public void handleMessage(Message message) {
        Set<URI> serviceRoles = new HashSet<URI>();
        Set<QName> headerQNames = new HashSet<QName>();
        Set<QName> understoodQNames = new HashSet<QName>();

        buildMustUnderstandHeaders(headerQNames, message, serviceRoles);
        initServiceSideInfo(understoodQNames, message, serviceRoles);
        headerQNames.removeAll(understoodQNames);
        if (!headerQNames.isEmpty()) {
            StringBuffer sb = new StringBuffer(300);
            int pos = 0;
            for (QName qname : headerQNames) {
                pos = pos + qname.toString().length() + 2;
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(qname.toString());
            }
            throw new SoapFault(SoapConstants.SOAP_12_CODE_MUSTUNDERSTAND, sb.toString());
        }
    }

    private void initServiceSideInfo(Set<QName> understoodQNames, 
                                     Message message,
                                     Set<URI> serviceRoles) {
        // Retrieve soap headers bound to message parts
        Operation operation = message.get(Operation.class);
        if (operation instanceof Wsdl1SoapOperation) {
            Wsdl1SoapOperation soapOper = (Wsdl1SoapOperation) operation;
            for (Wsdl1SoapPart part : soapOper.getInput().getParts()) {
                if (part.isHeader()) {
                    understoodQNames.add(part.getElement());
                }
            }
        }
        InterceptorChain chain = message.get(InterceptorChain.class);
        for (Interceptor interceptorInstance : chain.getInterceptors()) {
            if (interceptorInstance instanceof SoapInterceptor) {
                SoapInterceptor si = (SoapInterceptor) interceptorInstance;
                serviceRoles.addAll(si.getRoles());
                understoodQNames.addAll(si.getUnderstoodHeaders());
            }
        }
    }

    private void buildMustUnderstandHeaders(Set<QName> mustUnderstandHeaders, 
                                            Message message,
                                            Set<URI> serviceRoles) {
        SoapVersion soapVersion = message.get(SoapVersion.class);
        for(DocumentFragment df : message.getSoapHeaders().values()) {
            for (int i = 0; i < df.getChildNodes().getLength(); i++) {
                if (df.getChildNodes().item(i) instanceof Element) {
                    Element header = (Element) df.getChildNodes().item(i);
                    String mustUnderstand = header.getAttributeNS(soapVersion.getNamespace(),
                                                                  soapVersion.getAttrNameMustUnderstand());
                    if (Boolean.valueOf(mustUnderstand) || "1".equals(mustUnderstand.trim())) {
                        String role = header.getAttributeNS(soapVersion.getNamespace(), 
                                                            soapVersion.getAttrNameRole());
                        QName headerName = new QName(header.getNamespaceURI(), header.getLocalName());
                        role = role.trim();
                        if (role != null && role.length() > 0) {
                            role = role.trim();
                            if (role.equals(soapVersion.getNextRole()) || role.equals(soapVersion.getUltimateReceiverRole())) {
                                mustUnderstandHeaders.add(headerName);
                            } else {
                                for (URI roleFromBinding : serviceRoles) {
                                    if (role.equals(roleFromBinding)) {
                                        mustUnderstandHeaders.add(headerName);
                                    }
                                }
                            }
                        } else {
                            // if role omitted, the soap node is ultimate receiver,
                            // needs to understand
                            mustUnderstandHeaders.add(headerName);
                        }
                    }
                }
            }            
        }
    }

}
