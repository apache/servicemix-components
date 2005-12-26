/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import com.ibm.wsdl.Constants;

import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

public class JmsExtension {

    public static final String NS_URI_JMS = "http://servicemix.org/wsdl/jms/";

    public static final String ELEM_ADDRESS = "address";
    
    public static final QName Q_ELEM_JMS_ADDRESS = new QName(NS_URI_JMS, ELEM_ADDRESS);
    public static final QName Q_ELEM_JMS_BINDING = new QName(NS_URI_JMS, Constants.ELEM_BINDING);

    public static final String INITIAL_CONTEXT_FACTORY = "initialContextFactory";
    public static final String JNDI_PROVIDER_URL = "jndiProviderURL";
    public static final String DESTINATION_STYLE = "destinationStyle";
    public static final String JNDI_CONNECTION_FACTORY_NAME = "jndiConnectionFactoryName";
    public static final String JNDI_DESTINATION_NAME = "jndiDestinationName";
    public static final String JMS_PROVIDER_DESTINATION_NAME = "jmsProviderDestinationName";
    
    public static final String WSDL2_NS = "http://www.w3.org/2004/08/wsdl/";

    public static void register(ExtensionRegistry registry) {
        registry.registerDeserializer(            
                javax.wsdl.Port.class,
                Q_ELEM_JMS_ADDRESS,
                new JmsAddressDeserializer());
        registry.mapExtensionTypes(
                javax.wsdl.Port.class,
                Q_ELEM_JMS_ADDRESS,
                JmsAddress.class);
        registry.registerDeserializer(            
                javax.wsdl.Binding.class,
                Q_ELEM_JMS_BINDING,
                new JmsBindingDeserializer());
        registry.mapExtensionTypes(
                javax.wsdl.Binding.class,
                Q_ELEM_JMS_BINDING,
                JmsBinding.class);
    }
    
}
