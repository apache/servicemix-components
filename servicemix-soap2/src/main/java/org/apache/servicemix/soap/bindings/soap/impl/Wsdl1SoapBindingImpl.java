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
package org.apache.servicemix.soap.bindings.soap.impl;

import java.util.List;

import org.apache.servicemix.soap.api.Interceptor;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.bindings.soap.interceptors.MustUnderstandInterceptor;
import org.apache.servicemix.soap.bindings.soap.interceptors.SoapFaultInInterceptor;
import org.apache.servicemix.soap.bindings.soap.interceptors.SoapFaultOutInterceptor;
import org.apache.servicemix.soap.bindings.soap.interceptors.SoapInInterceptor;
import org.apache.servicemix.soap.bindings.soap.interceptors.SoapOutInterceptor;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapOperation;
import org.apache.servicemix.soap.core.model.AbstractBinding;
import org.apache.servicemix.soap.interceptors.jbi.JbiFaultOutInterceptor;
import org.apache.servicemix.soap.interceptors.jbi.JbiInInterceptor;
import org.apache.servicemix.soap.interceptors.jbi.JbiInWsdl1Interceptor;
import org.apache.servicemix.soap.interceptors.jbi.JbiOutInterceptor;
import org.apache.servicemix.soap.interceptors.jbi.JbiOutWsdl1Interceptor;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsInInterceptor;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsOutInterceptor;
import org.apache.servicemix.soap.interceptors.wsdl.WsdlOperationInInterceptor;
import org.apache.servicemix.soap.interceptors.xml.BodyOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxInInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxOutInterceptor;


public class Wsdl1SoapBindingImpl extends AbstractBinding<Wsdl1SoapOperation> implements Wsdl1SoapBinding {

    private SoapVersion soapVersion;
    private String locationURI;
    private String transportURI;
    private Style style;

    public Wsdl1SoapBindingImpl() {
        this(null);
    }
    
    public Wsdl1SoapBindingImpl(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
        
        List<Interceptor> phase;
        
        // ServerIn phase
        phase = getInterceptors(Phase.ServerIn);
        phase.add(new AttachmentsInInterceptor());
        phase.add(new StaxInInterceptor());
        phase.add(new SoapInInterceptor(soapVersion));
        phase.add(new WsdlOperationInInterceptor());
        phase.add(new MustUnderstandInterceptor());
        phase.add(new JbiInWsdl1Interceptor(true));
        phase.add(new JbiInInterceptor(true));
        
        // ServerOut phase
        phase = getInterceptors(Phase.ServerOut);
        phase.add(new JbiFaultOutInterceptor());
        phase.add(new JbiOutInterceptor(true));
        phase.add(new JbiOutWsdl1Interceptor(true));
        phase.add(new AttachmentsOutInterceptor());
        phase.add(new StaxOutInterceptor());
        phase.add(new SoapOutInterceptor(soapVersion));
        phase.add(new BodyOutInterceptor());
        
        // ServerOutFault phase
        phase = getInterceptors(Phase.ServerOutFault);
        phase.add(new StaxOutInterceptor());
        phase.add(new SoapOutInterceptor(soapVersion));
        phase.add(new SoapFaultOutInterceptor());
        
        // ClientOut phase
        phase = getInterceptors(Phase.ClientOut);
        phase.add(new JbiOutInterceptor(false));
        phase.add(new JbiOutWsdl1Interceptor(false));
        phase.add(new AttachmentsOutInterceptor());
        phase.add(new StaxOutInterceptor());
        phase.add(new SoapOutInterceptor(soapVersion));
        phase.add(new BodyOutInterceptor());

        // ClientIn phase
        phase = getInterceptors(Phase.ClientIn);
        phase.add(new AttachmentsInInterceptor());
        phase.add(new StaxInInterceptor());
        phase.add(new SoapInInterceptor());
        phase.add(new SoapFaultInInterceptor());
        phase.add(new JbiInWsdl1Interceptor(false));
        phase.add(new JbiInInterceptor(false));
    }

    /**
     * @return the locationURI
     */
    public String getLocationURI() {
        return locationURI;
    }

    /**
     * @param locationURI the locationURI to set
     */
    public void setLocationURI(String locationURI) {
        this.locationURI = locationURI;
    }

    /**
     * @return the transportURI
     */
    public String getTransportURI() {
        return transportURI;
    }

    /**
     * @param transportURI the transportURI to set
     */
    public void setTransportURI(String transportURI) {
        this.transportURI = transportURI;
    }

    /**
     * @return the style
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    public Message createMessage() {
        Message msg = super.createMessage();
        if (msg.get(SoapVersion.class) == null && soapVersion != null) {
            msg.put(SoapVersion.class, soapVersion);
        }
        return msg;
    }
    
    public Message createMessage(Message request) {
        Message msg = super.createMessage(request);
        if (msg.get(SoapVersion.class) == null && request.get(SoapVersion.class) != null) {
            msg.put(SoapVersion.class, request.get(SoapVersion.class));
        }
        if (msg.get(SoapVersion.class) == null && soapVersion != null) {
            msg.put(SoapVersion.class, soapVersion);
        }
        return msg;
    }
    
}
