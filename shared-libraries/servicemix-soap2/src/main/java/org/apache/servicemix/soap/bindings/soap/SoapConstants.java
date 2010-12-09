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
package org.apache.servicemix.soap.bindings.soap;

import javax.xml.namespace.QName;

public interface SoapConstants {
    
    public static final String SOAP_ACTION_HEADER = "SOAPAction";

    public static final String SOAP_11_URI = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_12_URI = "http://www.w3.org/2003/05/soap-envelope";

    public static final QName SOAP_11_FAULTCODE = new QName("faultcode");
    public static final QName SOAP_11_FAULTSTRING = new QName("faultstring");
    public static final QName SOAP_11_FAULTACTOR = new QName("faultactor");
    public static final QName SOAP_11_FAULTDETAIL = new QName("detail");
    public static final QName SOAP_11_CODE_VERSIONMISMATCH = new QName(SOAP_11_URI, "VersionMismatch");
    public static final QName SOAP_11_CODE_MUSTUNDERSTAND = new QName(SOAP_11_URI, "MustUnderstand");
    public static final QName SOAP_11_CODE_CLIENT = new QName(SOAP_11_URI, "Client");
    public static final QName SOAP_11_CODE_SERVER = new QName(SOAP_11_URI, "Server");
    
    public static final QName SOAP_12_FAULTCODE = new QName(SOAP_12_URI, "Code");
    public static final QName SOAP_12_FAULTSUBCODE = new QName(SOAP_12_URI, "Subcode");
    public static final QName SOAP_12_FAULTVALUE = new QName(SOAP_12_URI, "Value");
    public static final QName SOAP_12_FAULTREASON = new QName(SOAP_12_URI, "Reason");
    public static final QName SOAP_12_FAULTTEXT = new QName(SOAP_12_URI, "Text");
    public static final QName SOAP_12_FAULTNODE = new QName(SOAP_12_URI, "Node");
    public static final QName SOAP_12_FAULTROLE = new QName(SOAP_12_URI, "Role");
    public static final QName SOAP_12_FAULTDETAIL = new QName(SOAP_12_URI, "Detail");
    public static final QName SOAP_12_CODE_DATAENCODINGUNKNOWN = new QName(SOAP_12_URI, "DataEncodingUnknown");
    public static final QName SOAP_12_CODE_VERSIONMISMATCH = new QName(SOAP_12_URI, "VersionMismatch");
    public static final QName SOAP_12_CODE_MUSTUNDERSTAND = new QName(SOAP_12_URI, "MustUnderstand");
    public static final QName SOAP_12_CODE_RECEIVER = new QName(SOAP_12_URI, "Receiver");
    public static final QName SOAP_12_CODE_SENDER = new QName(SOAP_12_URI, "Sender");
    
}
