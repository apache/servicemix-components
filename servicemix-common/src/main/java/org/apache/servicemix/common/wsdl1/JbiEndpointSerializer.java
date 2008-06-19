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

import java.io.PrintWriter;

import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.namespace.QName;

import com.ibm.wsdl.util.xml.DOMUtils;

public class JbiEndpointSerializer implements ExtensionSerializer {

    public void marshall(Class parentType, 
                         QName elementType, 
                         ExtensibilityElement extension, 
                         PrintWriter pw, 
                         Definition def, 
                         ExtensionRegistry extReg) throws WSDLException {

        JbiEndpoint endpoint = (JbiEndpoint) extension;
        if (endpoint != null) {
            String tagName = DOMUtils.getQualifiedValue(JbiExtension.NS_URI_JBI, JbiExtension.ELEM_ENDPOINT, def);
            pw.print("    <" + tagName);
            DOMUtils.printAttribute(
                            JbiExtension.ROLE,
                            endpoint.getRole() == Role.CONSUMER ? JbiExtension.ROLE_CONSUMER : JbiExtension.ROLE_PROVIDER,
                            pw);
            if (endpoint.getDefaultMep() != null) {
                DOMUtils.printAttribute(
                                JbiExtension.DEFAULT_MEP,
                                endpoint.getDefaultMep().toString(),
                                pw);
            }
            if (endpoint.getDefaultOperation() != null) {
                DOMUtils.printQualifiedAttribute(
                                JbiExtension.DEFAULT_OPERATION, 
                                endpoint.getDefaultOperation(), 
                                def, 
                                pw);
            }
            pw.print("/>");
        }
    }

}
