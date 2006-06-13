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

import java.net.URI;
import java.util.Map;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.URISupport;
import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.auth.impl.JAASAuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;

public class JmsLifeCycle extends BaseLifeCycle {

    protected JmsConfiguration configuration;
    
    public JmsLifeCycle(BaseComponent component) {
        super(component);
        configuration = new JmsConfiguration();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.BaseLifeCycle#doInit()
     */
    protected void doInit() throws Exception {
        super.doInit();
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.load();
        // Lookup keystoreManager and authenticationService
        if (configuration.getKeystoreManager() == null) {
            try {
                String name = configuration.getKeystoreManagerName();
                Object km =  context.getNamingContext().lookup(name);
                configuration.setKeystoreManager((KeystoreManager) km); 
            } catch (Exception e) {
                // ignore
            }
        }
        if (configuration.getAuthenticationService() == null) {
            try {
                String name = configuration.getAuthenticationServiceName();
                Object as =  context.getNamingContext().lookup(name);
                configuration.setAuthenticationService((AuthenticationService) as); 
            } catch (Exception e) {
                configuration.setAuthenticationService(new JAASAuthenticationService());
            }
        }
    }
    
    /**
     * @return Returns the configuration.
     */
    public JmsConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration The configuration to set.
     */
    public void setConfiguration(JmsConfiguration configuration) {
        this.configuration = configuration;
    }

    protected QName getEPRServiceName() {
        return JmsResolvedEndpoint.EPR_SERVICE;
    }
    
    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        JmsEndpoint jmsEp = new JmsEndpoint();
        jmsEp.setServiceUnit(new ServiceUnit(component));
        jmsEp.setService(ep.getServiceName());
        jmsEp.setEndpoint(ep.getEndpointName());
        jmsEp.setRole(MessageExchange.Role.PROVIDER);
        URI uri = new URI(ep.getEndpointName());
        Map map = URISupport.parseQuery(uri.getQuery());
        if( IntrospectionSupport.setProperties(jmsEp, map, "jms.") ) {
            uri = URISupport.createRemainingURI(uri, map);
        }
        if (uri.getPath() != null) {
            String path = uri.getSchemeSpecificPart();
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.startsWith(AbstractJmsProcessor.STYLE_QUEUE + "/")) {
                jmsEp.setDestinationStyle(AbstractJmsProcessor.STYLE_QUEUE);
                jmsEp.setJmsProviderDestinationName(path.substring(AbstractJmsProcessor.STYLE_QUEUE.length() + 1));
            } else if (path.startsWith(AbstractJmsProcessor.STYLE_TOPIC + "/")) {
                jmsEp.setDestinationStyle(AbstractJmsProcessor.STYLE_TOPIC);
                jmsEp.setJmsProviderDestinationName(path.substring(AbstractJmsProcessor.STYLE_TOPIC.length() + 1));
            }
        }
        jmsEp.activateDynamic();
        // TODO: need to find some usefull syntax for jms
        // jms://
        return jmsEp;
    }

    /**
     * @return the keystoreManager
     */
    public KeystoreManager getKeystoreManager() {
        return configuration.getKeystoreManager();
    }

    /**
     * @param keystoreManager the keystoreManager to set
     */
    public void setKeystoreManager(KeystoreManager keystoreManager) {
        this.configuration.setKeystoreManager(keystoreManager);
    }

    /**
     * @return the authenticationService
     */
    public AuthenticationService getAuthenticationService() {
        return configuration.getAuthenticationService();
    }

    /**
     * @param authenticationService the authenticationService to set
     */
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.configuration.setAuthenticationService(authenticationService);
    }

}
