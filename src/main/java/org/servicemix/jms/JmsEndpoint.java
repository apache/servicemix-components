/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.jms;

import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.servicemix.common.Endpoint;
import org.servicemix.common.ExchangeProcessor;
import org.servicemix.jms.wsdl.JmsAddress;
import org.servicemix.jms.wsdl.JmsBinding;

public class JmsEndpoint extends Endpoint {
    
    protected JmsAddress address;
    protected JmsBinding binding;
    protected boolean soap;
    protected ExchangeProcessor processor;
    protected ServiceEndpoint activated;
    protected Role role;
    protected URI defaultMep;
    
    public JmsEndpoint() {
    }
    
    /**
     * @return Returns the address.
     */
    public JmsAddress getAddress() {
        return address;
    }
    /**
     * @param address The address to set.
     */
    public void setAddress(JmsAddress address) {
        this.address = address;
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
     * @return Returns the soap.
     */
    public boolean isSoap() {
        return soap;
    }
    /**
     * @param soap The soap to set.
     */
    public void setSoap(boolean soap) {
        this.soap = soap;
    }
    
    public String toString() {
        return "JMSEndpoint[service: " + service + ", " + 
                "endpoint: " + endpoint + ", " + 
                "address: " + address.getJndiDestinationName() + "(" + address.getDestinationStyle() + "), " + 
                "soap: " + soap + "]";
    }

    public ExchangeProcessor getProcessor() {
        return this.processor;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#activate()
     */
    public void activate() throws Exception {
        if (getRole() == Role.PROVIDER) {
            ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
            activated = ctx.activateEndpoint(service, endpoint);
            processor = new MultiplexingProviderProcessor(this);
        } else {
            processor = new MultiplexingConsumerProcessor(this);
        }
        processor.start();
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#deactivate()
     */
    public void deactivate() throws Exception {
        if (getRole() == Role.PROVIDER) {
            ServiceEndpoint ep = activated;
            activated = null;
            ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
            ctx.deactivateEndpoint(ep);
        }
        processor.stop();
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public URI getDefaultMep() {
        return defaultMep;
    }

}