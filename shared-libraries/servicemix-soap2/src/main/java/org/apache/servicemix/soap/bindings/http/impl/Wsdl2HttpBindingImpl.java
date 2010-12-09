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
package org.apache.servicemix.soap.bindings.http.impl;

import java.util.List;

import org.apache.servicemix.soap.api.Interceptor;
import org.apache.servicemix.soap.bindings.http.interceptors.HttpDecoderInterceptor;
import org.apache.servicemix.soap.bindings.http.interceptors.HttpInOperationInterceptor;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpBinding;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpOperation;
import org.apache.servicemix.soap.core.model.AbstractBinding;
import org.apache.servicemix.soap.interceptors.jbi.JbiFaultOutInterceptor;
import org.apache.servicemix.soap.interceptors.jbi.JbiInInterceptor;
import org.apache.servicemix.soap.interceptors.jbi.JbiOutInterceptor;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.BodyOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxOutInterceptor;

public class Wsdl2HttpBindingImpl extends AbstractBinding<Wsdl2HttpOperation> implements Wsdl2HttpBinding {

    private String httpAuthenticationRealm;
    private AuthenticationType httpAuthenticationType;
    private boolean httpCookies;
    private String httpMethodDefault;
    private String httpQueryParameterSeparatorDefault;
    private String httpTransferCodingDefault;
    private String httpVersion;
    
    public Wsdl2HttpBindingImpl() {
        List<Interceptor> phase;
        
        // ServerIn phase
        phase = getInterceptors(Phase.ServerIn);
        phase.add(new HttpInOperationInterceptor());
        phase.add(new HttpDecoderInterceptor(true));
        phase.add(new JbiInInterceptor(true));
        
        // ServerOut phase
        phase = getInterceptors(Phase.ServerOut);
        phase.add(new JbiFaultOutInterceptor());
        phase.add(new JbiOutInterceptor(true));
        phase.add(new AttachmentsOutInterceptor());
        phase.add(new StaxOutInterceptor());
        phase.add(new BodyOutInterceptor());
        
        // ClientOut phase
        phase = getInterceptors(Phase.ClientOut);

        // ClientIn phase
        phase = getInterceptors(Phase.ClientIn);
    }
    
    /**
     * @return the httpAuthenticationRealm
     */
    public String getHttpAuthenticationRealm() {
        return httpAuthenticationRealm;
    }
    /**
     * @param httpAuthenticationRealm the httpAuthenticationRealm to set
     */
    public void setHttpAuthenticationRealm(String httpAuthenticationRealm) {
        this.httpAuthenticationRealm = httpAuthenticationRealm;
    }
    /**
     * @return the httpAuthenticationType
     */
    public AuthenticationType getHttpAuthenticationType() {
        return httpAuthenticationType;
    }
    /**
     * @param httpAuthenticationType the httpAuthenticationType to set
     */
    public void setHttpAuthenticationType(AuthenticationType httpAuthenticationType) {
        this.httpAuthenticationType = httpAuthenticationType;
    }
    /**
     * @return the httpCookies
     */
    public boolean isHttpCookies() {
        return httpCookies;
    }
    /**
     * @param httpCookies the httpCookies to set
     */
    public void setHttpCookies(boolean httpCookies) {
        this.httpCookies = httpCookies;
    }
    /**
     * @return the httpMethodDefault
     */
    public String getHttpMethodDefault() {
        return httpMethodDefault;
    }
    /**
     * @param httpMethodDefault the httpMethodDefault to set
     */
    public void setHttpMethodDefault(String httpMethodDefault) {
        this.httpMethodDefault = httpMethodDefault;
    }
    /**
     * @return the httpQueryParamterSeparatorDefault
     */
    public String getHttpQueryParameterSeparatorDefault() {
        return httpQueryParameterSeparatorDefault;
    }
    /**
     * @param httpQueryParamterSeparatorDefault the httpQueryParamterSeparatorDefault to set
     */
    public void setHttpQueryParameterSeparatorDefault(String httpQueryParamterSeparatorDefault) {
        this.httpQueryParameterSeparatorDefault = httpQueryParamterSeparatorDefault;
    }
    /**
     * @return the httpTransferCodingDefault
     */
    public String getHttpTransferCodingDefault() {
        return httpTransferCodingDefault;
    }
    /**
     * @param httpTransferCodingDefault the httpTransferCodingDefault to set
     */
    public void setHttpTransferCodingDefault(String httpTransferCodingDefault) {
        this.httpTransferCodingDefault = httpTransferCodingDefault;
    }
    /**
     * @return the httpVersion
     */
    public String getHttpVersion() {
        return httpVersion;
    }
    /**
     * @param httpVersion the httpVersion to set
     */
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }
}
