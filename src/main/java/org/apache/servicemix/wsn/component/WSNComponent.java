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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jms.ConnectionFactory;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.EndpointSupport;
import org.apache.servicemix.common.tools.wsdl.WSDLFlattener;
import org.w3c.dom.Document;

public class WSNComponent extends BaseComponent {

    private WSDLFlattener flattener;
    private Map descriptions;
    
    @Override
	protected BaseLifeCycle createLifeCycle() {
		return new WSNLifeCycle(this);
	}

    @Override
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] { new WSNDeployer(this) };
        return new BaseServiceUnitManager(this, deployers);
    }

	public ConnectionFactory getConnectionFactory() {
		return ((WSNLifeCycle) lifeCycle).getConnectionFactory();
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		((WSNLifeCycle) lifeCycle).setConnectionFactory(connectionFactory);
	}

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.BaseComponent#getServiceDescription(javax.jbi.servicedesc.ServiceEndpoint)
     */
    @Override
    public Document getServiceDescription(ServiceEndpoint endpoint) {
        if (logger.isDebugEnabled()) {
            logger.debug("Querying service description for " + endpoint);
        }
        String key = EndpointSupport.getKey(endpoint);
        Endpoint ep = this.registry.getEndpoint(key);
        if (ep != null) {
            QName interfaceName = ep.getInterfaceName();
            if (interfaceName == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not retrieve description for endpoint " + key + " (no interface defined)");
                }
                return null;
            }
            Document doc = getDescription(interfaceName);
            return doc;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No endpoint found for " + key);
            }
            return null;
        }
    }

    private synchronized Document getDescription(QName interfaceName) {
        try {
            if (descriptions == null) {
                descriptions = new HashMap(); 
            }
            Document doc = (Document) descriptions.get(interfaceName);
            if (doc == null) {
                if (flattener == null) {
                    URL resource = getClass().getClassLoader().getResource("org/apache/servicemix/wsn/wsn.wsdl");
                    WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
                    Definition definition = reader.readWSDL(null, resource.toString());
                    flattener = new WSDLFlattener(definition);
                }
                Definition flatDef = flattener.getDefinition(interfaceName);
                doc = WSDLFactory.newInstance().newWSDLWriter().getDocument(flatDef);
                descriptions.put(interfaceName, doc);
            }
            return doc;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error retrieving endpoint description", e);
            }
            return null;
        }
    }
	
}
