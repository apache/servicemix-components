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
package org.apache.servicemix.cxfbc.interceptors;

import java.net.URI;

import javax.xml.namespace.QName;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public final class JbiConstants {

    public static final String SOAP_FAULT_CODE = "org.apache.servicemix.soap.fault.code";

    public static final String SOAP_FAULT_SUBCODE = "org.apache.servicemix.soap.fault.subcode";

    public static final String SOAP_FAULT_REASON = "org.apache.servicemix.soap.fault.reason";

    public static final String SOAP_FAULT_NODE = "org.apache.servicemix.soap.fault.node";

    public static final String SOAP_FAULT_ROLE = "org.apache.servicemix.soap.fault.role";

    public static final URI IN_ONLY = URI
            .create("http://www.w3.org/2004/08/wsdl/in-only");

    public static final URI IN_OUT = URI
            .create("http://www.w3.org/2004/08/wsdl/in-out");

    public static final URI IN_OPTIONAL_OUT = URI
            .create("http://www.w3.org/2004/08/wsdl/in-opt-out");

    public static final URI ROBUST_IN_ONLY = URI
            .create("http://www.w3.org/2004/08/wsdl/robust-in-only");

    public static final String PROTOCOL_HEADERS = "javax.jbi.messaging.protocol.headers";

    public static final String USE_JBI_WRAPPER = "useJbiWrapper";

    public static final String WSDL11_WRAPPER_NAMESPACE = "http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper";

    public static final String WSDL11_WRAPPER_PREFIX = "jbi";

    public static final String WSDL11_WRAPPER_MESSAGE_LOCALNAME = "message";
    
    public static final String JBI_SUFFIX = "JBIADDRESS";

    public static final QName WSDL11_WRAPPER_MESSAGE = new QName(
            WSDL11_WRAPPER_NAMESPACE, WSDL11_WRAPPER_MESSAGE_LOCALNAME,
            WSDL11_WRAPPER_PREFIX);

    public static final String WSDL11_WRAPPER_MESSAGE_PREFIX = "msg";

    public static final String WSDL11_WRAPPER_TYPE = "type";

    public static final String WSDL11_WRAPPER_NAME = "name";

    public static final String WSDL11_WRAPPER_VERSION = "version";

    public static final String WSDL11_WRAPPER_PART_LOCALNAME = "part";
    
    public static final String WSDL11_WRAPPER_XSD_PREFIX = "xsd";
    
    public static final String WSDL11_WRAPPER_XSI_PREFIX = "xsi";
        
    public static final QName WSDL11_WRAPPER_PART = new QName(
            WSDL11_WRAPPER_NAMESPACE, WSDL11_WRAPPER_PART_LOCALNAME,
            WSDL11_WRAPPER_PREFIX);
    private JbiConstants() {
        //Added to keep checkstyle 4.3 happy.
    }

}
