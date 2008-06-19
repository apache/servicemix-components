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
package org.apache.servicemix.common.wsdl1;

import org.apache.servicemix.common.AbstractDeployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.w3c.dom.Document;

import com.ibm.wsdl.Constants;

import javax.jbi.management.DeploymentException;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractWsdl1Deployer extends AbstractDeployer {

    protected FilenameFilter filter;
    
    public AbstractWsdl1Deployer(ServiceMixComponent component) {
        super(component);
        filter = new WsdlFilter();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Deployer#canDeploy(java.lang.String, java.lang.String)
     */
    public boolean canDeploy(String serviceUnitName, 
                             String serviceUnitRootPath) {
        File[] wsdls = new File(serviceUnitRootPath).listFiles(filter);
        return wsdls != null && wsdls.length > 0;
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Deployer#deploy(java.lang.String, java.lang.String)
     */
    public ServiceUnit deploy(String serviceUnitName, 
                              String serviceUnitRootPath) throws DeploymentException {
        File[] wsdls = new File(serviceUnitRootPath).listFiles(filter);
        if (wsdls == null || wsdls.length == 0) {
            throw failure("deploy", "No wsdl found", null);
        }
        ServiceUnit su = createServiceUnit();
        su.setComponent(component);
        su.setName(serviceUnitName);
        su.setRootPath(serviceUnitRootPath);
        for (int i = 0; i < wsdls.length; i++) {
            initFromWsdl(su, wsdls[i]);
        }
        if (su.getEndpoints().size() == 0) {
            throw failure("deploy", "Invalid wsdl: no endpoints found", null);
        }
        return su;
    }
    
    protected void initFromWsdl(ServiceUnit su, File wsdl) throws DeploymentException {
        Document description;
        Definition definition;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            description = factory.newDocumentBuilder().parse(wsdl);
            definition = createWsdlReader().readWSDL(null, description);
        } catch (Exception e) {
            throw failure("deploy", "Could not parse " + wsdl, e);
        }
        Map services = definition.getServices();
        if (services.size() == 0) {
            failure("deploy", "Invalid wsdl " + wsdl + ": no defined services", null);
        }
        for (Iterator itSvc = services.values().iterator(); itSvc.hasNext();) {
            Service svc = (Service) itSvc.next();
            for (Iterator itPorts = svc.getPorts().values().iterator(); itPorts.hasNext();) {
                JbiEndpoint jbiEndpoint = null;
                Port port = (Port) itPorts.next();
                ExtensibilityElement portElement = null;
                for (Iterator itElems = port.getExtensibilityElements().iterator(); itElems.hasNext();) {
                    ExtensibilityElement elem = (ExtensibilityElement) itElems.next();
                    if (elem instanceof JbiEndpoint) {
                        jbiEndpoint = (JbiEndpoint) elem;
                    } else if (filterPortElement(elem)) {
                        if (portElement == null) {
                            portElement = elem;
                        } else {
                            throw failure("deploy", "Invalid wsdl " + wsdl + ": more than one port element match", null);
                        }
                    }
                }
                if (portElement != null) {
                    Binding binding = port.getBinding();
                    ExtensibilityElement bindingElement = null;
                    for (Iterator itElems = binding.getExtensibilityElements().iterator(); itElems.hasNext();) {
                        ExtensibilityElement elem = (ExtensibilityElement) itElems.next();
                        if (filterBindingElement(elem)) {
                            if (bindingElement == null) {
                                bindingElement = elem;
                            } else {
                                throw failure("deploy", "Invalid wsdl " + wsdl + ": more than one binding element match", null);
                            }
                        }
                    }
                    if (bindingElement == null) {
                        throw failure("deploy", "Invalid wsdl " + wsdl + ": no matching binding element found", null);
                    }
                    Endpoint ep = createEndpoint(portElement, bindingElement, jbiEndpoint);
                    if (ep != null) {
                        ep.setServiceUnit(su);
                        ep.setDescription(description);
                        ep.setDefinition(definition);
                        ep.setService(svc.getQName());
                        ep.setEndpoint(port.getName());
                        ep.setInterfaceName(binding.getPortType().getQName());
                        validate(ep);
                        su.addEndpoint(ep);
                    }
                }
            }
        }
    }    
    
    protected WSDLReader createWsdlReader() throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        ExtensionRegistry registry = factory.newPopulatedExtensionRegistry();
        registerExtensions(registry);
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature(Constants.FEATURE_VERBOSE, false);
        reader.setExtensionRegistry(registry);
        return reader;
    }
    
    protected void registerExtensions(ExtensionRegistry registry) {
        JbiExtension.register(registry);
    }

    protected ServiceUnit createServiceUnit() {
        return new ServiceUnit();
    }
    
    protected abstract Endpoint createEndpoint(ExtensibilityElement portElement,
                                               ExtensibilityElement bindingElement,
                                               JbiEndpoint jbiEndpoint);
    
    protected abstract boolean filterPortElement(ExtensibilityElement element);
    
    protected abstract boolean filterBindingElement(ExtensibilityElement element);
    
    public static class WsdlFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".wsdl");
        }
        
    }
    
}
