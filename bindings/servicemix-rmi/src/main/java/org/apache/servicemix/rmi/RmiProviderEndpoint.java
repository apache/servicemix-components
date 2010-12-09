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
package org.apache.servicemix.rmi;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.wsdl.PortType;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.rmi.marshaler.DefaultRmiMarshaler;
import org.apache.servicemix.rmi.marshaler.RmiMarshalerSupport;
import org.apache.servicemix.rmi.util.WsdlHelper;
import org.springframework.core.io.Resource;

/**
 * <p>
 * A {@link org.apache.servicemix.common.endpoints.ProviderEndpoint ProviderEndpoint} that call an external RMI remote object.
 * </p>
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="provider"
 */
public class RmiProviderEndpoint extends ProviderEndpoint implements RmiEndpointType {

    private String host; // target RMI host name
    private int port = Registry.REGISTRY_PORT; // target RMI port number
    private String name; // target name into the RMI registry
    private Class remoteInterface; // the remote interface
    private Resource wsdl; // the used WSDL
    
    private RmiMarshalerSupport marshaler = new DefaultRmiMarshaler(); // the RMI marshaler
    
    private Registry registry = null; // the RMI registry
    private Remote stub = null; // the remote stub
    
    public String getHost() {
        return this.host;
    }
    
    /**
     * <p>
     * This attribute defines the remote RMI server host name.
     * </p>
     * 
     * @param host the remote RMI server host name.
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return this.port;
    }
    
    /**
     * <p>
     * This attribute defines the remote RMI server port number.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>1099</b>.</i>
     * 
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getName() {
        return this.name;
    }
    
    /**
     * <p>
     * This attribute defines the lookup name into the remote RMI registry.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is the <b>the endpoint name</b>.</i>
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public Class getRemoteInterface() {
        return this.remoteInterface;
    }
    
    /**
     * <p>
     * The attribute defines the remote interface of the RMI stub.
     * </p>
     * 
     * @param remoteInterface the remote interface to use.
     */
    public void setRemoteInterface(Class remoteInterface) {
        this.remoteInterface = remoteInterface;
    }
    
    public Resource getWsdl() {
        return this.wsdl;
    }
    
    /**
     * <p>
     * This attribute defines the WSDL used by the endpoint to generate message content.
     * </p>
     * 
     * @param wsdl the WSDL to use.
     */
    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }
    
    public RmiMarshalerSupport getMarshaler() {
        return this.marshaler;
    }
    
    /**
     * <p>
     * This attribute defines the RMI marshaler to use.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is a instance of <b>DefaultRmiMarshaler</b>.
     * 
     * @param marshaler
     */
    public void setMarshaler(RmiMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        super.validate();
        
        if (host == null || host.trim().length() < 1) {
            throw new DeploymentException("The RMI host is mandatory.");
        }
        
        if (remoteInterface == null) {
            throw new DeploymentException("The remoteInterface is mandatory.");
        }
        
        if (name == null || name.trim().length() < 1) {
            // the user hasn't provide the RMI name, use the endpoint one
            name = this.getEndpoint();
        }
        
        if (wsdl != null) {
            // the user provides the WSDL, load it into endpoint definition/description
            
        } else {
            try {
                // generate the wsdl using the remote interface
                WsdlHelper wsdlHelper = new WsdlHelper(remoteInterface);
                // define the databinding to use in the marshaler
                marshaler.setDataBinding(wsdlHelper.getDataBinding());
                // create the WSDL document
                definition = wsdlHelper.createDocument();
                // TODO make some cleanups in the WSDL definition
                // use wsdl4j to populate the endpoint description
                WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
                description = writer.getDocument(definition);
            } catch (Exception e) {
                throw new DeploymentException("Can't generate the WSDL.", e);
            }
        }
        
        try {
            registry = LocateRegistry.getRegistry(host, port);
        } catch (Exception e) {
            throw new DeploymentException("Can't connect using RMI on " + host + ":" + port, e);
        }
        
        // lookup the stub
        try {
            stub = registry.lookup(name);
        } catch (Exception e) {
            throw new DeploymentException("Remote object " + name + " lookup fails.", e);
        }
        
        if (stub == null) {
            throw new DeploymentException("Remote object " + name + " is not found.");
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            // the exchange is active
            NormalizedMessage in = exchange.getMessage("in");
            if (in == null) {
                throw new IllegalStateException("Exchange has not in message.");
            }
            // create a RMI exchange container
            RmiExchange rmiExchange = new RmiExchange();
            // populate the RMI exchange
            //marshaler.createRmiCall(in, rmiExchange);
            // perform the RMI call
            Object response = rmiExchange.invoke(stub);
            // TODO handle RMI invocation exception using exchange.setError();
            // marshal the response object into a normalized message
            //marshaler.handleRmiResponse(exchange, response, rmiExchange.getMethod().getReturnType());
            // send the exchange into the NMR
            send(exchange);
        }
    }
    
}
