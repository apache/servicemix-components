/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jbi.management.DeploymentException;
import javax.jbi.management.LifeCycleMBean;
import javax.jbi.messaging.MessageExchange.Role;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.activemq.util.IdGenerator;
import org.apache.servicemix.common.AbstractDeployer;
import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.wsn.EndpointManager;
import org.apache.servicemix.wsn.EndpointRegistrationException;
import org.apache.servicemix.wsn.jaxws.NotificationBroker;
import org.apache.servicemix.wsn.jbi.JbiNotificationBroker;
import org.apache.servicemix.wsn.jms.JmsCreatePullPoint;
import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;

public class WSNDeployer extends AbstractDeployer implements Deployer {

    protected FilenameFilter filter;
    protected JAXBContext context;
    
    public WSNDeployer(BaseComponent component) {
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
            throw failure("deploy", "No wsdl found", null);
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
        //} else if (request instanceof RegisterPublisher) {
        } else {
            throw failure("deploy", "Unsupported request " + request.getClass().getName(), null);
        }
    }
    
    public class WSNSubscriptionEndpoint extends Endpoint implements EndpointManager {

        private Subscribe request;
        private SubscribeResponse response;
        
        public WSNSubscriptionEndpoint(Subscribe request) throws DeploymentException {
            this.service = new QName("http://servicemix.org/wsnotification", "Subscription");
            this.endpoint = new IdGenerator().generateSanitizedId();
            this.request = request;
        }
        
        @Override
        public Role getRole() {
            return Role.CONSUMER;
        }

        @Override
        public void activate() throws Exception {
            JbiNotificationBroker broker = ((WSNLifeCycle) serviceUnit.getComponent().getLifeCycle()).getNotificationBroker();
            response = broker.handleSubscribe(request, this);
        }

        @Override
        public void deactivate() throws Exception {
            JbiNotificationBroker broker = ((WSNLifeCycle) serviceUnit.getComponent().getLifeCycle()).getNotificationBroker();
            broker.unsubscribe(response.getSubscriptionReference().getAddress().getValue());
        }

        @Override
        public ExchangeProcessor getProcessor() {
            return null;
        }

        public Object register(String address, Object service) throws EndpointRegistrationException {
            return null;
        }

        public void unregister(Object endpoint) throws EndpointRegistrationException {
        }

    }
    
    public class WSNPullPointEndpoint extends Endpoint implements EndpointManager {

        private CreatePullPoint request;
        private CreatePullPointResponse response;
        
        public WSNPullPointEndpoint(CreatePullPoint request) throws DeploymentException {
            this.service = new QName("http://servicemix.org/wsnotification", "Subscription");
            this.endpoint = new IdGenerator().generateSanitizedId();
            this.request = request;
        }
        
        @Override
        public Role getRole() {
            return Role.PROVIDER;
        }

        @Override
        public void activate() throws Exception {
            JmsCreatePullPoint createPullPoint = ((WSNLifeCycle) serviceUnit.getComponent().getLifeCycle()).getCreatePullPoint();
            response = createPullPoint.createPullPoint(request);
        }

        @Override
        public void deactivate() throws Exception {
            JmsCreatePullPoint createPullPoint = ((WSNLifeCycle) serviceUnit.getComponent().getLifeCycle()).getCreatePullPoint();
            createPullPoint.destroyPullPoint(response.getPullPoint().getAddress().getValue());
        }

        @Override
        public ExchangeProcessor getProcessor() {
            return null;
        }

        public Object register(String address, Object service) throws EndpointRegistrationException {
            return null;
        }

        public void unregister(Object endpoint) throws EndpointRegistrationException {
        }

    }
    
    public static class WSNServiceUnit extends ServiceUnit {
        public void start() throws Exception {
            List<Endpoint> activated = new ArrayList<Endpoint>();
            try {
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNPullPointEndpoint) {
                        endpoint.activate();
                        activated.add(endpoint);
                    }
                }
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNSubscriptionEndpoint) {
                        endpoint.activate();
                        activated.add(endpoint);
                    }
                }
                this.status = LifeCycleMBean.STARTED;
            } catch (Exception e) {
                // Deactivate activated endpoints
                for (Endpoint endpoint : activated) {
                    try {
                        endpoint.deactivate();
                    } catch (Exception e2) {
                        // do nothing
                    }
                }
                throw e;
            }
        }
    }
    
    public static class XmlFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }
        
    }
    
}
