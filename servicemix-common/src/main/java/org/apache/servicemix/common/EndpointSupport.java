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
package org.apache.servicemix.common;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;


public class EndpointSupport {

    public static String getKey(QName service, String endpoint) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append(service.getNamespaceURI());
        sb.append("}");
        sb.append(service.getLocalPart());
        sb.append(":");
        sb.append(endpoint);
        return sb.toString();
    }
    
    public static String getKey(ServiceEndpoint endpoint) {
        return getKey(endpoint.getServiceName(), endpoint.getEndpointName());
    }
    
    public static String getKey(Endpoint endpoint) {
        return endpoint.getKey();
    }
    
}
