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

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.jdom.xpath.XPath;
import org.w3c.dom.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * XPath PDF composer marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public class XpathPdfComposerMarshaler implements PdfComposerMarshalerSupport {
    
    private final Logger logger = LoggerFactory.getLogger(XpathPdfComposerMarshaler.class);
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.pdfcomposer.marshaler.PdfComposerMarshalerSupport#unmarshal(javax.jbi.messaging.NormalizedMessage)
     */
    public PdfComposerRequest unmarshal(NormalizedMessage in) throws Exception {
        // TODO check performance issue by using stream
        
        logger.debug("Create the PDF composer request.");
        PdfComposerRequest request = new PdfComposerRequest();

        logger.debug("Transform the in NoramlizedMessage to XML node.");
        SourceTransformer transformer = new SourceTransformer();
        Node root = transformer.toDOMNode(in);
        
        logger.debug("Extract template.");
        XPath templateXPath = XPath.newInstance("//template");
        request.setTemplate(templateXPath.valueOf(root));
        
        //logger.debug("Extract rows.");
        //XPath rowsXPath = XPath.newInstance("*/row");
        //List rows = rowsXPath.selectNodes(root);
        
        return request;
    }

}
