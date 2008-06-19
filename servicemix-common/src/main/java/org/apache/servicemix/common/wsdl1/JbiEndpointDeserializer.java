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

import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.wsdl.util.xml.DOMUtils;

public class JbiEndpointDeserializer implements ExtensionDeserializer {

    public ExtensibilityElement unmarshall(
            Class parentType,
            QName elementType,
            Element el,
            Definition def,
            ExtensionRegistry extReg)
            throws WSDLException {
        
        JbiEndpoint jbiEndpoint = (JbiEndpoint) extReg.createExtension(parentType, elementType);

        String role = DOMUtils.getAttribute(el, JbiExtension.ROLE);
        if (role == null) {
            throw new WSDLException(WSDLException.OTHER_ERROR, "Role must be specified");
        } else if (JbiExtension.ROLE_CONSUMER.equals(role)) {
            jbiEndpoint.setRole(Role.CONSUMER);
        } else if (JbiExtension.ROLE_PROVIDER.equals(role)) {
            jbiEndpoint.setRole(Role.PROVIDER);
        } else {
            throw new WSDLException(WSDLException.OTHER_ERROR, "Unrecognized role: " + role);
        }

        String defaultMep = DOMUtils.getAttribute(el, JbiExtension.DEFAULT_MEP);
        if (defaultMep == null) {
            defaultMep = JbiExtension.DEFAULT_MEP_IN_OUT;
        }
        if (JbiExtension.DEFAULT_MEP_IN_ONLY.equals(defaultMep) ||
            JbiExtension.DEFAULT_MEP_ROBUST_IN_ONLY.equals(defaultMep) ||
            JbiExtension.DEFAULT_MEP_IN_OUT.equals(defaultMep)) {
            jbiEndpoint.setDefaultMep(URI.create(JbiExtension.WSDL2_NS + defaultMep));
        }
        
        QName defaultOperation = DOMUtils.getQualifiedAttributeValue(el, JbiExtension.DEFAULT_OPERATION, null, false, def);
        if (defaultOperation != null) {
            jbiEndpoint.setDefaultOperation(defaultOperation);
        }
        
        return jbiEndpoint;
    }

}
