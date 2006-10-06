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
package org.apache.servicemix.file;

import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.URISupport;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ResolvedEndpoint;
import org.w3c.dom.DocumentFragment;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.io.File;

/**
 * @org.apache.xbean.XBean element="component"
 * description="File Component"
 */
public class FileComponent extends DefaultComponent {

    public final static String EPR_URI = "urn:servicemix:file";
    public final static QName EPR_SERVICE = new QName(EPR_URI, "FileComponent");
    public final static String EPR_NAME = "epr";


    protected Class[] getEndpointClasses() {
        return new Class[]{FileEndpoint.class, FilePollEndpoint.class};
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return ResolvedEndpoint.resolveEndpoint(epr, EPR_URI, EPR_NAME, EPR_SERVICE, "file:");
    }

    protected QName getEPRServiceName() {
        return EPR_SERVICE;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        FileEndpoint fileEp = new FileEndpoint(this, ep);

        // TODO
        //fileEp.setRole(MessageExchange.Role.PROVIDER);

        // lets use a URL to parse the path
        URL url = new URL(ep.getEndpointName());

        Map map = URISupport.parseQuery(url.getQuery());
        IntrospectionSupport.setProperties(fileEp, map, "file.");

        String path = url.getPath();
        if (path != null) {
            fileEp.setDirectory(new File(path));
        }
        else {
            throw new IllegalArgumentException("No path defined for URL: " + url);
        }
        fileEp.activate();
        return fileEp;
    }

}
