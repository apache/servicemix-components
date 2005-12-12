/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.jms.wsdl;

import com.ibm.wsdl.util.xml.DOMUtils;

import org.w3c.dom.Element;

import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import java.net.URI;

public class JmsBindingDeserializer implements ExtensionDeserializer {

    public ExtensibilityElement unmarshall(
            Class parentType,
            QName elementType,
            Element el,
            Definition def,
            ExtensionRegistry extReg)
            throws WSDLException {
        
        JmsBinding jmsBinding = (JmsBinding) extReg.createExtension(parentType, elementType);

        return jmsBinding;
    }

}
