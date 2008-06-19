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

/**
 * Represents the SOAP 1.1 version
 * 
 * @version $Revision: 437862 $
 */
public class Soap11 implements SoapVersion {
    
    public static final String SOAP_NAMESPACE = SoapConstants.SOAP_11_URI;
    
    public static final String SOAP_DEFAULT_PREFIX = "soap";
    
    private static final Soap11 INSTANCE = new Soap11(SOAP_DEFAULT_PREFIX);

    private final double version = 1.1;

    private final String namespace = SOAP_NAMESPACE;

    private final String prefix;

    private final String noneRole = namespace + "/role/none";

    private final String ultimateReceiverRole = namespace + "/role/ultimateReceiver";

    private final String nextRole = namespace + "/role/next";

    private final String soapEncodingStyle = "http://schemas.xmlsoap.org/soap/encoding/";

    private final QName envelope;

    private final QName header;

    private final QName body;

    private final QName fault;

    public static Soap11 getInstance() {
        return INSTANCE;
    }
    
    public Soap11(String prefix) {
        this.prefix = prefix;
        envelope = new QName(namespace, "Envelope", prefix);
        header = new QName(namespace, "Header", prefix);
        body = new QName(namespace, "Body", prefix);
        fault = new QName(namespace, "Fault", prefix);
    }

    public String getSoapMimeType() {
        return "text/xml; charset=utf-8";
    }

    public double getVersion() {
        return version;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public QName getEnvelope() {
        return envelope;
    }

    public QName getHeader() {
        return header;
    }

    public QName getBody() {
        return body;
    }

    public QName getFault() {
        return fault;
    }

    public String getSoapEncodingStyle() {
        return soapEncodingStyle;
    }

    // Role URIs
    // -------------------------------------------------------------------------
    public String getNoneRole() {
        return noneRole;
    }

    public String getUltimateReceiverRole() {
        return ultimateReceiverRole;
    }

    public String getNextRole() {
        return nextRole;
    }
    
    public String getAttrNameRole() {
        return "actor";
    }

    public String getAttrNameMustUnderstand() {
        return "mustUnderstand";
    }
    
    public SoapVersion getDerivedVersion(String prefix) {
        return new Soap11(prefix);
    }

}
