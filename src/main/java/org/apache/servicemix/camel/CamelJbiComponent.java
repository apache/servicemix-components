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
package org.apache.servicemix.camel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.util.IntrospectionSupport;
import org.apache.servicemix.common.util.URISupport;

/**
 * Deploys the camel endpoints within JBI
 *
 * @version $Revision: 426415 $
 */
public class CamelJbiComponent extends DefaultComponent implements CamelComponent {

    protected CamelSpringDeployer deployer;

    private List<JbiComponent> jbiComponents = new ArrayList<JbiComponent>();

    /*
     * (non-Javadoc)
     *
     * @see org.servicemix.common.BaseComponent#createServiceUnitManager()
     */
    @Override
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] {new CamelSpringDeployer(this)};
        return new BaseServiceUnitManager(this, deployers);
    }

    /**
     * @return List of endpoints
     * @see org.apache.servicemix.common.DefaultComponent#getConfiguredEndpoints()
     */
    @Override
    protected List<CamelProviderEndpoint> getConfiguredEndpoints() {
        return new ArrayList<CamelProviderEndpoint>();
    }

    /**
     * @return Class[]
     * @see org.apache.servicemix.common.DefaultComponent#getEndpointClasses()
     */
    @Override
    protected Class[] getEndpointClasses() {
        return new Class[] {CamelProviderEndpoint.class, CamelConsumerEndpoint.class};
    }

    @Override
    protected String[] getEPRProtocols() {
        return new String[] {"camel"};
    }

    @Override
    protected org.apache.servicemix.common.Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        org.apache.servicemix.common.Endpoint endpoint = null;
        // extract the su name camel:su1:seda:queque
        JbiComponent jbiComponent = null;
        String uriString = "";
        String endpointName = ep.getEndpointName();
        String names[] = endpointName.split(":");
        if (names.length > 2) {
            jbiComponent = getJbiComponent(names[1]);

        } else {
            throw new IllegalStateException("Can't find the su name from the endpoint name");
        }
        if (jbiComponent != null) {
            // skip the su-name part
            int index = 0;
            for(String name : names) {
                if (index == 0) {
                    uriString = name;
                }
                if (index > 1) {
                    uriString += ":" + name;
                }
                index ++;
            }
            endpoint = createEndpoint(uriString, jbiComponent);
        } else {
            throw new IllegalStateException("Can't find the JbiComponent");
        }
        return endpoint;
    }

    public CamelProviderEndpoint createEndpoint(String uriString, JbiComponent jbiComponent) throws URISyntaxException {
        URI uri = new URI(uriString);
        Map map = URISupport.parseQuery(uri.getQuery());
        String camelUri = uri.getSchemeSpecificPart();
        Endpoint camelEndpoint = jbiComponent.getCamelContext().getEndpoint(camelUri);
        Processor processor = jbiComponent.createCamelProcessor(camelEndpoint);
        CamelProviderEndpoint endpoint = new CamelProviderEndpoint(getServiceUnit(), camelEndpoint, jbiComponent.getBinding(), processor);

        IntrospectionSupport.setProperties(endpoint, map);

        // TODO
        // endpoint.setRole(MessageExchange.Role.PROVIDER);

        return endpoint;
    }

    public synchronized void addJbiComponent(JbiComponent jbiComponent) {
        jbiComponents.add(jbiComponent);
    }

    public synchronized void removeJbiComponent(String suName) {
        JbiComponent component = getJbiComponent(suName);
        if (component != null) {
            jbiComponents.remove(component);
        }
    }

    public synchronized JbiComponent getJbiComponent(String suName) {
        JbiComponent result = null;
        for (JbiComponent component: jbiComponents) {
            if (suName.equals(component.getSuName())) {
                result = component;
                break;
            }
        }
        return result;
    }

    /**
     * Activating a JBI endpoint created by a camel consumer.
     *
     */
    public void activateJbiEndpoint(CamelProviderEndpoint jbiEndpoint) throws Exception {

        // the following method will activate the new dynamic JBI endpoint
        if (deployer != null) {
            // lets add this to the current service unit being deployed
            deployer.addService(jbiEndpoint);
        } else {
            addEndpoint(jbiEndpoint);
        }

    }

    public void deactivateJbiEndpoint(CamelProviderEndpoint jbiEndpoint) throws Exception {
        // this will be done by the ServiceUnit
        if (jbiEndpoint.getServiceUnit() == serviceUnit) {
            removeEndpoint(jbiEndpoint);
        }
    }


    /**
     * Should we expose the Camel JBI onto the NMR. <p/> We may wish to add some
     * policy stuff etc.
     *
     * @param endpoint
     *            the camel endpoint
     * @return true if the endpoint should be exposed in the NMR
     */
    public boolean isEndpointExposedOnNmr(Endpoint endpoint) {
        // TODO we should only expose consuming endpoints
        return false;
        //return !(endpoint instanceof JbiEndpoint);
    }
}
