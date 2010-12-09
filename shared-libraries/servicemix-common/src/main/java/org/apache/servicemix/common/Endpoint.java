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
package org.apache.servicemix.common;

import org.apache.commons.logging.Log;
import org.apache.servicemix.common.xbean.XBeanServiceUnit;

import org.w3c.dom.Document;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

public interface Endpoint {

    String getKey();

    QName getInterfaceName();

    QName getService();

    String getEndpoint();

    Document getDescription();

    Role getRole();

    ServiceUnit getServiceUnit();

    void setServiceUnit(ServiceUnit serviceUnit);

    boolean isExchangeOkay(MessageExchange exchange);

    /**
     * Register this endpoint into the NMR and put the endpoint
     * in a STOPPED state, where the endpoint is able to process
     * incoming requests, but won't consume external requests such
     * as JMS messages or HTTP requests.
     *
     * @throws Exception
     */
    void activate() throws Exception;

    /**
     * Start consumption of external requests.
     *
     * @throws Exception
     */
    void start() throws Exception;

    /**
     * Stop consumption of external requests.
     *
     * @throws Exception
     */
    void stop() throws Exception;

    /**
     * Unregister this endpoint from the NMR.
     *
     * @throws Exception
     */
    void deactivate() throws Exception;

    /**
     * Process an incoming JBI exchange.
     *
     * @param exchange
     * @throws Exception
     */
    void process(MessageExchange exchange) throws Exception;

    /**
     * Validation is a step which is done at deployment time to check
     * that the endpoint definition is valid and that there is no
     * missing properties.
     *
     * @throws DeploymentException
     */
    void validate() throws DeploymentException;

}
