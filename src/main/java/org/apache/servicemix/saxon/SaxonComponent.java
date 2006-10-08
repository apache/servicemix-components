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
package org.apache.servicemix.saxon;

import org.apache.servicemix.common.DefaultComponent;

import java.util.List;

/**
 * 
 * @org.apache.xbean.XBean element="component"
 *                  description="XSLT component"
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class SaxonComponent extends DefaultComponent {

    private SaxonEndpoint[] endpoints;

    public SaxonEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(SaxonEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    protected Class[] getEndpointClasses() {
        return new Class[] { SaxonEndpoint.class };
    }

    protected List getConfiguredEndpoints() {
        return asList(getEndpoints());
    }
}
