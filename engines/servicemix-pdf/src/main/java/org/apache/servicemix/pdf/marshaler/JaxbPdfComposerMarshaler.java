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
package org.apache.servicemix.pdf.marshaler;

import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * JAXB PDF Composer marshaler that uses JAXB to marshal and unmarshal PdfComposerRequest/Response.
 * </p>
 * 
 * @author jbonofre
 */
public class JaxbPdfComposerMarshaler implements PdfComposerMarshalerSupport {
    
    private final static transient Log LOG = LogFactory.getLog(JaxbPdfComposerMarshaler.class);
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.pdfcomposer.marshaler.PdfComposerMarshalerSupport#unmarshal(javax.jbi.messaging.NormalizedMessage)
     */
    public PdfComposerRequest unmarshal(NormalizedMessage in) throws Exception {
        
        // create a JAXB context for the PdfComposerRequest
        LOG.debug("Create a JAXB context with PdfComposerRequest class.");
        JAXBContext jaxbContext = JAXBContext.newInstance(PdfComposerRequest.class);
        
        // create the JAXB unmarshaller
        LOG.debug("Create the JAXB unmarshaller.");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        
        // unmarshal in the "in" message content
        LOG.debug("Unmarshal the \"in\" message content.");
        return (PdfComposerRequest) unmarshaller.unmarshal(in.getContent());
    }

}
