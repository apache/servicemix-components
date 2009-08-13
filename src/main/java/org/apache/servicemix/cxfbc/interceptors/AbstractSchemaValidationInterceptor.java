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

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.util.DomUtil;
import org.w3c.dom.Element;


public abstract class AbstractSchemaValidationInterceptor extends
        AbstractSoapInterceptor {
    private boolean useJBIWrapper = true;
    private boolean useSOAPEnvelope = true;
    
    public AbstractSchemaValidationInterceptor(String phase, boolean useJBIWrapper, boolean useSOAPEnvelope) {
        super(phase);
        this.useJBIWrapper = useJBIWrapper;
        this.useSOAPEnvelope = useSOAPEnvelope;
    }

    protected void validateMessage(SoapMessage message) throws Fault {
        Service service = ServiceModelUtil.getService(message.getExchange());
        if (service != null) {
            Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0));
            if (schema != null) {
                javax.xml.validation.Validator validator = schema.newValidator();
                try {
                    Element sourceMessage = new SourceTransformer().toDOMElement(message.getContent(Source.class));
                    if (!useJBIWrapper && !useSOAPEnvelope) {
                        validator.validate(new DOMSource(sourceMessage));
                    } else {
                        Element partWrapper = DomUtil
                                .getFirstChildElement(sourceMessage);
                        while (partWrapper != null) {
                            Element partContent = DomUtil
                                    .getFirstChildElement(partWrapper);
                            validator.validate(new DOMSource(partContent));
                            partWrapper = DomUtil
                                    .getNextSiblingElement(partWrapper);
                        }
                    }
                    
                } catch (Exception e) {
                    throw new Fault(e);
                }
            }
        }
    }
    
}
