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
package org.apache.servicemix.http.processors;

import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpConfiguration;
import org.apache.servicemix.http.HttpEndpoint;

/**
 * Abstract base class for provider and consumer processor. 
 */
public class AbstractProcessor {

    public static final String HEADER_SOAP_ACTION = "SOAPAction";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";

    protected HttpEndpoint endpoint;

    AbstractProcessor(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected HttpConfiguration getConfiguration() {
        HttpComponent comp = (HttpComponent) endpoint.getServiceUnit().getComponent();
        return comp.getConfiguration();
    }
}
