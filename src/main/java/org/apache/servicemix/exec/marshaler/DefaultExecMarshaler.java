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
package org.apache.servicemix.exec.marshaler;

import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.w3c.dom.Document;

/**
 * <p>
 * Default exec marshaler that use JAXB to marshal and unmarshal exec objects.
 * </p>
 * 
 * @author jbonofre
 */
public class DefaultExecMarshaler implements ExecMarshalerSupport {
    
    // logging facility
    private final static transient Log LOG = LogFactory.getLog(DefaultExecMarshaler.class);
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.exec.marshaler.ExecMarshalerSupport#unmarshal(javax.jbi.messaging.NormalizedMessage)
     */
    public ExecRequest unmarshal(NormalizedMessage in) throws Exception {
        
        // create a JAXB context for the exec request
        LOG.debug("Create a JAXB context.");
        JAXBContext jaxbContext = JAXBContext.newInstance(ExecRequest.class);
        
        // create a unmarshaller
        LOG.debug("Create the JAXB unmarshaller.");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        
        // unmarshal the in message content
        SourceTransformer transformer = new SourceTransformer();
        LOG.debug("In message: " + transformer.contentToString(in));
        return (ExecRequest)unmarshaller.unmarshal(in.getContent());
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.exec.marshaler.ExecMarshalerSupport#marshal(org.apache.servicemix.exec.marshaler.ExecResponse, javax.jbi.messaging.NormalizedMessage)
     */
    public void marshal(ExecResponse execResponse, NormalizedMessage out) throws Exception {
        
        // create a JAXB context for the exec response
        JAXBContext jaxbContext = JAXBContext.newInstance(ExecResponse.class);
        
        // create a marshaller
        Marshaller marshaller = jaxbContext.createMarshaller();
        
        // marshal into the out message node
        SourceTransformer transformer = new SourceTransformer();
        Document document = transformer.createDocument();
        marshaller.marshal(execResponse, document);
        
        // populate the out message content
        out.setContent(new DOMSource(document));
    }
    
}
