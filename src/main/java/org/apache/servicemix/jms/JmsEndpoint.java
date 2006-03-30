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
package org.apache.servicemix.jms;

import java.util.Iterator;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.jms.wsdl.JmsBinding;
import org.apache.servicemix.soap.SoapEndpoint;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="endpoint"
 *                  description="A jms endpoint"
 * 
 */
public class JmsEndpoint extends SoapEndpoint {
    
    protected JmsBinding binding;
    // Jms informations
    protected String initialContextFactory;
    protected String jndiProviderURL;
    protected String destinationStyle;
    protected String jndiConnectionFactoryName;
    protected String jndiDestinationName;
    protected String jmsProviderDestinationName;
    
    public JmsEndpoint() {
    }
    
    /**
     * @return Returns the binding.
     */
    public JmsBinding getBinding() {
        return binding;
    }
    /**
     * @param binding The binding to set.
     */
    public void setBinding(JmsBinding binding) {
        this.binding = binding;
    }

    /**
     * @return Returns the initialContextFactory.
     */
    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    /**
     * @param initialContextFactory The initialContextFactory to set.
     */
    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }

    /**
     * @return Returns the jmsProviderDestinationName.
     */
    public String getJmsProviderDestinationName() {
        return jmsProviderDestinationName;
    }

    /**
     * @param jmsProviderDestinationName The jmsProviderDestinationName to set.
     */
    public void setJmsProviderDestinationName(String jmsProviderDestinationName) {
        this.jmsProviderDestinationName = jmsProviderDestinationName;
    }

    /**
     * @return Returns the jndiConnectionFactoryName.
     */
    public String getJndiConnectionFactoryName() {
        return jndiConnectionFactoryName;
    }

    /**
     * @param jndiConnectionFactoryName The jndiConnectionFactoryName to set.
     */
    public void setJndiConnectionFactoryName(String jndiConnectionFactoryName) {
        this.jndiConnectionFactoryName = jndiConnectionFactoryName;
    }

    /**
     * @return Returns the jndiDestinationName.
     */
    public String getJndiDestinationName() {
        return jndiDestinationName;
    }

    /**
     * @param jndiDestinationName The jndiDestinationName to set.
     */
    public void setJndiDestinationName(String jndiDestinationName) {
        this.jndiDestinationName = jndiDestinationName;
    }

    /**
     * @return Returns the jndiProviderURL.
     */
    public String getJndiProviderURL() {
        return jndiProviderURL;
    }

    /**
     * @param jndiProviderURL The jndiProviderURL to set.
     */
    public void setJndiProviderURL(String jndiProviderURL) {
        this.jndiProviderURL = jndiProviderURL;
    }

    /**
     * @return Returns the destinationStyle.
     */
    public String getDestinationStyle() {
        return destinationStyle;
    }

    /**
     * @param destinationStyle The destinationStyle to set.
     */
    public void setDestinationStyle(String destinationStyle) {
        this.destinationStyle = destinationStyle;
    }

    /**
     * @org.apache.xbean.Property alias="role"
     * @param role
     */
    public void setRoleAsString(String role) {
        super.setRoleAsString(role);
    }

    protected ExchangeProcessor createProviderProcessor() {
        return new MultiplexingProviderProcessor(this);
    }

    protected ExchangeProcessor createConsumerProcessor() {
        return new MultiplexingConsumerProcessor(this);
    }

    protected ServiceEndpoint createExternalEndpoint() {
        return new JmsExternalEndpoint(this);
    }

    protected void overrideDefinition(Definition def) {
        Service svc = null;
        Port port = null;
        if (targetService != null && targetEndpoint != null) {
            svc = def.getService(targetService);
            port = (svc != null) ? svc.getPort(targetEndpoint) : null;
        } else if (targetService != null) {
            svc = def.getService(targetService);
            if (svc != null) {
                Iterator it = svc.getPorts().values().iterator();
                port = (it.hasNext()) ? (Port) it.next() : null;
            }
        } else if (targetInterfaceName != null) {
            Iterator it = def.getServices().values().iterator();
            svc = it .hasNext() ? (Service) it.next() : null;
            if (svc != null) {
                it = svc.getPorts().values().iterator();
                port = (it.hasNext()) ? (Port) it.next() : null;
            }
        } else {
            svc = def.getService(service);
            port = (svc != null) ? svc.getPort(endpoint) : null;
        }
        if (port != null) {
            port.getExtensibilityElements().clear();
            /*
            if (isSoap()) {
                SOAPAddress address = new SOAPAddressImpl();
                address.setLocationURI(getLocationURI());
                port.addExtensibilityElement(address);
                def.addNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
            } else {
                HTTPAddress address = new HTTPAddressImpl();
                address.setLocationURI(getLocationURI());
                port.addExtensibilityElement(address);
                def.addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
            }
            */
            // TODO: add binding information
            svc.getPorts().clear();
            svc.addPort(port);
            definition = def;
        }
    }

    public String toString() {
        return "JMSEndpoint[service: " + service + ", " + 
                "endpoint: " + endpoint + ", " + 
                "address: " + jndiDestinationName + "(" + destinationStyle + "), " + 
                "soap: " + soap + "]";
    }
}