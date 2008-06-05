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
package org.apache.servicemix.bean.support;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.resolver.URIResolver;
import org.springframework.beans.factory.InitializingBean;

/**
 * An ExchangeTarget may be used to specify the target of an exchange,
 * while retaining all the JBI features (interface based routing, service
 * name based routing or endpoint routing).
 *   
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="exchange-target"
 */
public class ExchangeTarget implements InitializingBean {

    private QName interf;

    private QName operation;

    private QName service;

    private String endpoint;
    
    private String uri;

    /**
     * @return Returns the endpoint.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint
     *            The endpoint to set.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return Returns the interface name.
     */
    public QName getInterface() {
        return interf;
    }

    /**
     * @param interface name
     *            The interface name to set.
     */
    public void setInterface(QName itf) {
        this.interf = itf;
    }

    /**
     * @return Returns the operation name.
     */
    public QName getOperation() {
        return operation;
    }

    /**
     * @param operation
     *            The operation to set.
     */
    public void setOperation(QName operation) {
        this.operation = operation;
    }

    /**
     * @return Returns the service.
     */
    public QName getService() {
        return service;
    }

    /**
     * @param service
     *            The service to set.
     */
    public void setService(QName service) {
        this.service = service;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Configures the target on the newly created exchange 
     * @param exchange the exchange to configure
     * @throws MessagingException if the target could not be configured
     */
    public void configureTarget(MessageExchange exchange, ComponentContext context) throws MessagingException {
        if (interf == null && service == null && uri == null) {
            throw new MessagingException("interface, service or uri should be specified");
        }
        if (uri != null) {
            URIResolver.configureExchange(exchange, context, uri);
        }
        if (interf != null) {
            exchange.setInterfaceName(interf);
        }
        if (operation != null) {
            exchange.setOperation(operation);
        }
        if (service != null) {
            exchange.setService(service);
            if (endpoint != null) {
                ServiceEndpoint se = context.getEndpoint(service, endpoint);
                exchange.setEndpoint(se);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (interf == null && service == null && uri == null) {
            throw new MessagingException("interface, service or uri should be specified");
        }
    }

}
