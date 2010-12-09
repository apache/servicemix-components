/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.common;

import org.apache.commons.logging.Log;
import org.apache.servicemix.executors.Executor;

import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

/**
 * Represents an extended JBI Component implementation which exposes some extra features
 *
 * @version $Revision$
 */
public interface ServiceMixComponent extends Component {

    /**
     * @return Returns the logger.
     */
    public Log getLogger();

    /**
     * @return Returns the registry.
     */
    public Registry getRegistry();

    /**
     * @param role  the role to use
     * @return Returns the executor for this component
     */
    public Executor getExecutor(MessageExchange.Role role);

    /**
     * @return Returns the components context
     */
    public ComponentContext getComponentContext();

    /**
     * @return the JBI container
     */
    public Container getContainer();

    /**
     * @return the servicemix 3 container if deployed into it
     */
    public Object getSmx3Container();

    /**
     * @return Returns the name of the component
     */
    public String getComponentName();
    
    /**
     * Prepare an exchange sent from the given endpoint.
     * The caller need to send / sendSync the exchange. 
     * 
     * @param exchange the exchange to send
     * @param endpoint the endpoint sending the exchange
     * @throws MessagingException
     */
    public void prepareExchange(MessageExchange exchange, Endpoint endpoint) throws MessagingException;

    /**
     * Prepare shutting the given endpoint down by waiting for all know exchanges for
     * this endpoint to be fully processed.
     *
     * @param endpoint
     * @throws InterruptedException
     */
    public void prepareShutdown(Endpoint endpoint) throws InterruptedException;

    /**
     * Make the component aware of this exchange.
     * This method needs to be called for each exchange sent or received.
     *
     * @param endpoint
     * @param exchange
     * @param add
     */
    public void handleExchange(Endpoint endpoint, MessageExchange exchange, boolean add);

    /**
     * @return the QName of the element used in EPR
     */
    public QName getEPRElementName();
}
