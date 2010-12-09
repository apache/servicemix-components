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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.rmi.marshaler.DefaultRmiMarshaler;
import org.apache.servicemix.rmi.marshaler.RmiMarshalerSupport;
import org.apache.servicemix.rmi.util.WsdlHelper;

/**
 * <p>
 * A {@link org.apache.servicemix.common.endpoints.ConsumerEndpoint ConsumerEndpoint} which uses RMI's {@link UnicastRemoteObject}
 * to consume method invocation.
 * </p>
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="consumer"
 */
public class RmiConsumerEndpoint extends ConsumerEndpoint implements RmiEndpointType, InvocationHandler {

    private Class remoteInterface; // the remote interface
    private RmiMarshalerSupport marshaler = new DefaultRmiMarshaler();
   
    private String name; // the endpoint name into the RMI registry
    private Object pojo; // delegate method call to a POJO
    private int port = Registry.REGISTRY_PORT; // the RMI registry port number
    private Registry registry = null; // component RMI registry
    
    private Remote stub; // the remote stub
    private Remote proxy; // the remote proxy
    
    private ClassLoader classLoader;
    
    public Class getRemoteInterface() {
        return this.remoteInterface;
    }
    
    /**
     * <p>
     * This attribute defines the remote interface implemented by RMI endpoint.
     * </p>
     * 
     * @param remoteInterface the remote interface.
     */
    public void setRemoteInterface(Class remoteInterface) {
        this.remoteInterface = remoteInterface;
    }
    
    public String getName() {
        return this.name;
    }
    
    /**
     * <p>
     * This attribute defines the endpoint name in the RMI registry.
     * </p>
     * 
     * @param name the endpoint name in the registry.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public Object getPojo() {
        return this.pojo;
    }
    
    /**
     * <p>
     * This attribute defines the POJO on which method calls are delegated.
     * </p>
     * 
     * @param pojo the delegation POJO.
     */
    public void setPojo(Object pojo) {
        this.pojo = pojo;
    }
    
    public int getPort() {
        return this.port;
    }
    
    /**
     * <p>
     * This attribute defines the RMI registry port number to use.
     * </p>
     * 
     * @param port the RMI registry port.
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        
        if (this.getTargetService() == null) {
            logger.warn("Target service not defined, use the endpoint one.");
            this.setTargetService(this.getService());
        }
        
        if (this.getTargetEndpoint() == null) {
            logger.warn("Target endpoint not defined, use the endpoint one.");
            this.setTargetEndpoint(this.getEndpoint());
        }

        // get the thread context class loader
        classLoader = Thread.currentThread().getContextClassLoader();

        super.validate();
        
        if (remoteInterface == null) {
            throw new DeploymentException("The remoteInterface property is mandatory.");
        }
        
        if (name == null || name.trim().length() < 1) {
            // if the user hasn't define the registry name, use the endpoint one
            logger.debug("The user hasn't define the RMI registry name, use the endpoint one.");
            name = this.getEndpoint();
        }
        
        // create the registry
        if (registry == null) {
            try {
                registry = LocateRegistry.createRegistry(this.port);
            } catch (Exception e) {
                logger.warn("Can't create RMI registry, try to use an existing one.");
                try {
                    registry = LocateRegistry.getRegistry(this.port);
                } catch (Exception exception) {
                    throw new DeploymentException("Can't locate RMI registry.", exception);
                }
            }
        }

        // generate the WSDL definition
        try {
            WsdlHelper wsdlHelper = new WsdlHelper(remoteInterface);
            definition = wsdlHelper.createDocument();
            // TODO cleanup the definition (add correct name/target namespace) and make cleanup (binding) to make an abstract WSDL
            // use wsdl4j to create the WSDL description
            WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
            description = writer.getDocument(definition);
        } catch (Exception e) {
            logger.error("Can't generate the endpoint WSDL.", e);
            throw new DeploymentException("Can't generate the endpoint WSDL.", e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();
        
        // create the dynamic proxy
        Class[] interfaceArray = new Class[]{ remoteInterface };
        proxy = (Remote) Proxy.newProxyInstance(remoteInterface.getClassLoader(), interfaceArray, this);
        // create the remote stub
        stub = UnicastRemoteObject.exportObject(proxy, 0);
        try {
            // register the object into the registry
            registry.bind(name, stub);
        } catch (Exception e) {
            // an error occurs, unexport the name from the registry
            try {
                UnicastRemoteObject.unexportObject(stub, true);
            } catch (Throwable ignore) { }
            stub = null;
            throw e;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#stop()
     */
    @Override
    public synchronized void stop() throws Exception {
        // destroy the RMI registry if local
        if (registry != null) {
            Registry reg = registry;
            registry = null;
            UnicastRemoteObject.unexportObject(reg, true);
        }
        super.stop(); 
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.error("Remote invocation made");
        if (pojo != null) {
            // delegrate method call to the POJO
            // WARNING: using POJO you bypass NMR and it's a direct POJO call
            try {
                return method.invoke(pojo, args);
            } catch (InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getTargetException();
            } catch (Exception e) {
                throw e;
            }
        } else {
            // TODO use CXF ClientProxyFactoryBean to inject into the NMR, much easier
            //ClientProxyFactoryBean clientProxyFactory = new ClientProxyFactoryBean();
            //clientProxyFactory.setServiceClass(serviceClass);
            
            // construct XML structure corresponding to the RMI call and send into the NMR
            // create in-out exchange
            InOut exchange = getExchangeFactory().createInOutExchange();
            // set the exchange operation name to the called method
            //exchange.setOperation(new QName("", ""));
            // create the in message
            NormalizedMessage in = exchange.createMessage();
            // set the exchange in message
            exchange.setInMessage(in);
            
            // TODO check if the method match a WSDL an operation
            
            // create a RMI exchange
            RmiExchange rmiExchange = new RmiExchange();
            rmiExchange.setObject(proxy);
            rmiExchange.setMethod(method);
            rmiExchange.setArgs(args);
            
            // marshal the RMI exchange into the in message
            marshaler.rmiExchangeToNmr(in, rmiExchange);
            // send the exchange to the delivery channel
            // TODO use a send and hold on to the marshaler in a map with the exchange id
            sendSync(exchange);
            // use the marshaler to get back to the object
            //return marshaler.objectFromNMR(exchange.getOutMessage());
            return null;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            // received DONE for a sent message
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // received ERROR state for a sent message
            // there is no real error handling here for now
            return;
        } else {
            throw new MessagingException("Unsupported exchange received...");
        }
    }
    
}
