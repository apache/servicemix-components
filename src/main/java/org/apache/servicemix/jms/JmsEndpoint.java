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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Properties;

import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;
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
    // Spring configuration
    protected ConnectionFactory connectionFactory;
    protected Destination destination;
    protected String processorName;
    
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
     * @return Returns the connectionFactory.
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * @param connectionFactory The connectionFactory to set.
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    /**
     * @return Returns the destination.
     */
    public Destination getDestination() {
        return destination;
    }

    /**
     * @param destination The destination to set.
     */
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * @org.apache.xbean.Property alias="role"
     * @param role
     */
    public void setRoleAsString(String role) {
        super.setRoleAsString(role);
    }

    protected ExchangeProcessor createProviderProcessor() {
        return createProcessor("provider");
    }

    protected ExchangeProcessor createConsumerProcessor() {
        return createProcessor("consumer");
    }
    
    protected ExchangeProcessor createProcessor(String type) {
        try {
            String procName = processorName;
            if (processorName == null) {
                JmsLifeCycle lf = (JmsLifeCycle) getServiceUnit().getComponent().getLifeCycle();
                procName = lf.getConfiguration().getProcessorName();
            }
            String uri = "META-INF/services/org/apache/servicemix/jms/" + procName;
            InputStream in = loadResource(uri);
            Properties props = new Properties();
            props.load(in);
            String className = props.getProperty(type);
            Class cl = loadClass(className);
            Constructor cns = cl.getConstructor(new Class[] { getClass() });
            return (ExchangeProcessor) cns.newInstance(new Object[] { this });
        } catch (Exception e) {
            throw new RuntimeException("Could not create processor of type " + type + " and name " + processorName, e);
        }
    }

    /**
     * Attempts to load the class on the current thread context class loader or
     * the class loader which loaded us
     */
    protected Class loadClass(String name) throws ClassNotFoundException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return contextClassLoader.loadClass(name);
            }
            catch (ClassNotFoundException e) {
            }
        }
        return getClass().getClassLoader().loadClass(name);
    }

    /**
     * Loads the resource from the given URI
     */
    protected InputStream loadResource(String uri) {
        // lets try the thread context class loader first
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(uri);
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream(uri);
            if (in == null) {
                logger.debug("Could not find resource: " + uri);
            }
        }
        return in;
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

    /**
     * @return Returns the processorName.
     */
    public String getProcessorName() {
        return processorName;
    }

    /**
     * @param processorName The processorName to set.
     */
    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }
    
    public JmsConfiguration getConfiguration() {
        JmsLifeCycle lifeCycle = (JmsLifeCycle) getServiceUnit().getComponent().getLifeCycle();
        return lifeCycle.getConfiguration();
    }

    public AuthenticationService getAuthenticationService() {
        ComponentLifeCycle lf = getServiceUnit().getComponent().getLifeCycle();
        return ((JmsLifeCycle) lf).getAuthenticationService();
    }

    public KeystoreManager getKeystoreManager() {
        ComponentLifeCycle lf = getServiceUnit().getComponent().getLifeCycle();
        return ((JmsLifeCycle) lf).getKeystoreManager();
    }

}