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
package org.apache.servicemix.jms.wsdl;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.wsdl.util.xml.DOMUtils;

public class JmsAddressDeserializer implements ExtensionDeserializer {

    public ExtensibilityElement unmarshall(
            Class parentType, QName elementType, Element el,
            Definition def, ExtensionRegistry extReg)
        throws WSDLException {
        
        JmsAddress jmsAddress = (JmsAddress) extReg.createExtension(parentType, elementType);

        jmsAddress.setInitialContextFactory(
                DOMUtils.getAttribute(el, JmsExtension.INITIAL_CONTEXT_FACTORY));
        jmsAddress.setJndiProviderURL(
                DOMUtils.getAttribute(el, JmsExtension.JNDI_PROVIDER_URL));
        jmsAddress.setDestinationStyle(
                DOMUtils.getAttribute(el, JmsExtension.DESTINATION_STYLE));
        jmsAddress.setJndiConnectionFactoryName(
                DOMUtils.getAttribute(el, JmsExtension.JNDI_CONNECTION_FACTORY_NAME));
        jmsAddress.setJndiDestinationName(
                DOMUtils.getAttribute(el, JmsExtension.JNDI_DESTINATION_NAME));
        jmsAddress.setJmsProviderDestinationName(
                DOMUtils.getAttribute(el, JmsExtension.JMS_PROVIDER_DESTINATION_NAME));

        return jmsAddress;
    }

}
