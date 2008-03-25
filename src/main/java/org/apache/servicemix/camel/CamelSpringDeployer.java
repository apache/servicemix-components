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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jbi.management.DeploymentException;

import org.apache.camel.Endpoint;
import org.apache.camel.component.bean.BeanComponent;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.xbean.AbstractXBeanDeployer;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * A deployer of the spring XML file
 * 
 * @version $Revision: 1.1 $
 */
public class CamelSpringDeployer extends AbstractXBeanDeployer {

    private final CamelJbiComponent component;

    private List<CamelJbiEndpoint> activatedEndpoints = new ArrayList<CamelJbiEndpoint>();

    private String serviceUnitName;

    public CamelSpringDeployer(CamelJbiComponent component) {
        super(component);
        this.component = component;
    }

    @Override
    protected String getXBeanFile() {
        return "camel-context";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Deployer#deploy(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public ServiceUnit deploy(String suName, String serviceUnitRootPath) throws DeploymentException {
        // lets register the deployer so that any endpoints activated are added
        // to this SU
        component.deployer = this;

        this.serviceUnitName = suName;

        // lets install the context class loader
        ServiceUnit serviceUnit = super.deploy(suName, serviceUnitRootPath);
        Thread.currentThread().setContextClassLoader(serviceUnit.getConfigurationClassLoader());
        return serviceUnit;
    }

    public void addService(CamelJbiEndpoint endpoint) {
        activatedEndpoints.add(endpoint);
    }

    @Override
    protected Collection<org.apache.servicemix.common.Endpoint> getServices(
                        AbstractXmlApplicationContext applicationContext) throws Exception {
        List<org.apache.servicemix.common.Endpoint> services = new ArrayList<org.apache.servicemix.common.Endpoint>(activatedEndpoints);
        activatedEndpoints.clear();

        SpringCamelContext camelContext = SpringCamelContext.springCamelContext(applicationContext);

        // now lets iterate through all the endpoints
        Collection<Endpoint> endpoints = camelContext.getSingletonEndpoints();

        for (Endpoint endpoint : endpoints) {
            if (component.isEndpointExposedOnNmr(endpoint)) {
                services.add(component.createJbiEndpointFromCamel(endpoint));
            }
        }

        // lets add a control bus endpoint to ensure we have at least one endpoint to deploy
        BeanComponent beanComponent = camelContext.getComponent("bean", BeanComponent.class);
        Endpoint endpoint = beanComponent.createEndpoint(new CamelControlBus(camelContext),
                                                         "camel:" + serviceUnitName + "-controlBus");
        services.add(component.createJbiEndpointFromCamel(endpoint));

        return services;
    }

    protected Map getParentBeansMap() {
        Map beans =  super.getParentBeansMap();
        beans.put("jbi", component);
        return beans;
    }

}
