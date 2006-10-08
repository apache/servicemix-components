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
package org.apache.servicemix.ftp;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ResolvedEndpoint;
import org.w3c.dom.DocumentFragment;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

/**
 * An FTP based component
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="component" description="FTP Component"
 */
public class FtpComponent extends DefaultComponent {

    public final static String EPR_URI = "urn:servicemix:ftp";
    public final static QName EPR_SERVICE = new QName(FtpComponent.EPR_URI, "FtpComponent");
    public final static String EPR_NAME = "epr";

    private FtpEndpoint[] endpoints;

    public FtpEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(FtpEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return ResolvedEndpoint.resolveEndpoint(epr, FtpComponent.EPR_URI, FtpComponent.EPR_NAME, FtpComponent.EPR_SERVICE, "ftp:");
    }

    protected List getConfiguredEndpoints() {
        return asList(getEndpoints());
    }

    protected Class[] getEndpointClasses() {
        return new Class[]{FtpEndpoint.class};
    }

    protected QName getEPRServiceName() {
        return FtpComponent.EPR_SERVICE;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        FtpEndpoint ftpEndpoint = new FtpEndpoint(this, ep);

        // TODO
        //ftpEp.setRole(MessageExchange.Role.PROVIDER);

        URI uri = new URI(ep.getEndpointName());

        ftpEndpoint.setUri(uri);
        ftpEndpoint.activate();
        return ftpEndpoint;
    }

}
