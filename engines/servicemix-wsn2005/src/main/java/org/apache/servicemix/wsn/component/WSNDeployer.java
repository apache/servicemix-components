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
package org.apache.servicemix.wsn.component;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jbi.management.DeploymentException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.servicemix.common.AbstractDeployer;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.brw_2.NotificationBroker;

public class WSNDeployer extends AbstractDeployer implements Deployer {

    protected FilenameFilter filter;

    protected JAXBContext context;

    public WSNDeployer(ServiceMixComponent component) {
        super(component);
        filter = new XmlFilter();
        try {
            context = WSNEndpoint.createJAXBContext(NotificationBroker.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create jaxb context", e);
        }
    }

    public boolean canDeploy(String serviceUnitName, String serviceUnitRootPath) {
        File[] xmls = new File(serviceUnitRootPath).listFiles(filter);
        return xmls != null && xmls.length > 0;
    }

    public ServiceUnit deploy(String serviceUnitName, String serviceUnitRootPath) throws DeploymentException {
        File[] xmls = new File(serviceUnitRootPath).listFiles(filter);
        if (xmls == null || xmls.length == 0) {
            throw failure("deploy", "No xml files found", null);
        }
        WSNServiceUnit su = new WSNServiceUnit();
        su.setComponent(component);
        su.setName(serviceUnitName);
        su.setRootPath(serviceUnitRootPath);
        for (int i = 0; i < xmls.length; i++) {
            Endpoint ep;
            URL url;
            try {
                url = xmls[i].toURL();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                throw new DeploymentException("Error deploying xml file", e);
            }
            ep = createEndpoint(url);
            ep.setServiceUnit(su);
            validate(ep);
            su.addEndpoint(ep);
        }
        if (su.getEndpoints().size() == 0) {
            throw failure("deploy", "Invalid wsdl: no endpoints found", null);
        }
        return su;
    }

    public Endpoint createEndpoint(URL url) throws DeploymentException {
        Object request = null;
        try {
            request = context.createUnmarshaller().unmarshal(url);
        } catch (JAXBException e) {
            throw failure("deploy", "Invalid xml", e);
        }
        return createEndpoint(request);
    }

    public Endpoint createEndpoint(Object request) throws DeploymentException {
        if (request instanceof Subscribe) {
            return new WSNSubscriptionEndpoint((Subscribe) request);
        } else if (request instanceof CreatePullPoint) {
            return new WSNPullPointEndpoint((CreatePullPoint) request);
        } else if (request instanceof RegisterPublisher) {
            return new WSNPublisherEndpoint((RegisterPublisher) request);
        } else {
            throw failure("deploy", "Unsupported request " + request.getClass().getName(), null);
        }
    }

    public static class XmlFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }

    }

}
