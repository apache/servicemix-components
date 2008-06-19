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

import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

public class JbiExtension {

    public static final String NS_URI_JBI = "http://servicemix.org/wsdl/jbi/";

    public static final String ELEM_ENDPOINT = "endpoint";
    
    public static final QName Q_ELEM_JBI_ENDPOINT = new QName(NS_URI_JBI, ELEM_ENDPOINT);

    public static final String ROLE = "role";
    public static final String ROLE_CONSUMER = "consumer";
    public static final String ROLE_PROVIDER = "provider";
    
    public static final String DEFAULT_MEP = "defaultMep";
    public static final String DEFAULT_MEP_IN_ONLY = "in-only";
    public static final String DEFAULT_MEP_ROBUST_IN_ONLY = "robust-in-only";
    public static final String DEFAULT_MEP_IN_OUT = "in-out";
    
    public static final String DEFAULT_OPERATION = "defaultOperation";
    
    public static final String WSDL2_NS = "http://www.w3.org/2004/08/wsdl/";

    public static void register(ExtensionRegistry registry) {
        registry.registerDeserializer(            
                        javax.wsdl.Port.class,
                        Q_ELEM_JBI_ENDPOINT,
                        new JbiEndpointDeserializer());
        registry.registerSerializer(            
                        javax.wsdl.Port.class,
                        Q_ELEM_JBI_ENDPOINT,
                        new JbiEndpointSerializer());
        registry.mapExtensionTypes(
                        javax.wsdl.Port.class,
                        Q_ELEM_JBI_ENDPOINT,
                        JbiEndpoint.class);
    }
    
}
