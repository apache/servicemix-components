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
package org.apache.servicemix.truezip;

// import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.jbi.util.IntrospectionSupport;
import org.apache.servicemix.jbi.util.URISupport;

import de.schlichtherle.io.File;

/**
 * A file based component
 * 
 * @version $Revision$
 * @org.apache.xbean.XBean element="component" description="File Component"
 */
public class TrueZipComponent extends DefaultComponent {

    private TrueZipEndpointType[] endpoints;

    public TrueZipEndpointType[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(TrueZipEndpointType[] endpoints) {
        this.endpoints = endpoints;
    }

    protected List getConfiguredEndpoints() {
        return asList(getEndpoints());
    }

    protected Class[] getEndpointClasses() {
        return new Class[] { TrueZipPollerEndpoint.class, TrueZipSenderEndpoint.class };
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        TrueZipSenderEndpoint fileEp = new TrueZipSenderEndpoint(this, ep);
        URI uri = new URI(ep.getEndpointName());
        String file = null;
        String host = uri.getHost();
        String path = uri.getPath();
        if (host != null) {
            if (path != null) {
                // lets assume host really is a relative directory
                file = host + File.separator + path;
            } else {
                file = host;
            }
        } else {
            if (path != null) {
                file = path;
            } else {
                // must be an absolute URI
                file = uri.getSchemeSpecificPart();
            }
        }
        Map map = URISupport.parseQuery(uri.getQuery());
        if (IntrospectionSupport.setProperties(fileEp, map, "truezip.")) {
            uri = URISupport.createRemainingURI(uri, map);
        }
        if (file != null) {
            fileEp.setDirectory(new File(file));
        } else {
            throw new IllegalArgumentException("No file defined for URL: " + uri);
        }
        fileEp.activate();
        return fileEp;
    }

}
