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
package org.apache.servicemix.pdfcomposer;

import java.util.List;

import org.apache.servicemix.common.DefaultComponent;

/**
 * <p>
 * A JBI component which generated PDF documents populating fields in a PDF template using incoming messages.
 * </p>
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="component" description="PDFComposer Component"
 */
public class PdfComposerComponent extends DefaultComponent {
    
    private PdfComposerEndpoint[] endpoints;
    
    /**
     * <p>
     * Get the component endpoints.
     * </p>
     * 
     * @return the component endpoints. 
     */
    public PdfComposerEndpoint[] getEndpoints() {
        return this.endpoints;
    }
    
    /**
     * <p>
     * Set the component endpoints.
     * </p>
     * 
     * @param endpoints the component endpoints.
     */
    public void setEndpoints(PdfComposerEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getConfiguredEndpoints()
     */
    @Override
    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getEndpointClasses()
     */
    @Override
    protected Class[] getEndpointClasses() {
        return new Class[] { PdfComposerEndpoint.class };
    }

}
