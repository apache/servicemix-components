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

import java.util.Collection;

import javax.jbi.management.DeploymentException;

/**
 * <p>
 * This interface defines the lifecycle and needed
 * methods to a collection of endpoints grouped into
 * a service unit.
 * </p>
 */
public interface ServiceUnit {

    /**
     * Retrieves the name of this service unit.
     *
     * @return
     */
    String getName();

    /**
     * Retrieves the root path of this service unit.
     * @return
     */
    String getRootPath();

    /**
     * Retrieves the component where this SU is deployed.
     *
     * @return
     */
    ServiceMixComponent getComponent();

    /**
     * Retrieves the list of deployed endpoints.
     *
     * @return
     */
    Collection<Endpoint> getEndpoints();

    /**
     * Retrieve this service unit specific classloader.
     *
     * @return
     */
    ClassLoader getConfigurationClassLoader();

    /**
     * Retrieve the state of this service unit.
     * States can be: STOPPED, STARTED or SHUTDOWN
     *
     * @return
     */
    String getCurrentState();

    /**
     * Puts the SU in a STOPPED state.
     * This call is only valid if the service unit is in a SHUTDOWN state.
     * It means it is able to process incoming exchange but will not
     * initiate new exchanges. The process of initializing a service unit
     * should activate all endpoints, but not start them.
     *
     * @throws Exception
     * @see Endpoint#start()
     */
    void init() throws Exception;

    /**
     * Transition this service unit into the STARTED state.
     * This call is only valid if the service unit is in a STOPPED state.
     * Start consumption of external requests by starting all
     * the endpoints.
     *
     * @throws Exception
     * @see Endpoint#start()
     */
    void start() throws Exception;

    /**
     * Transition this service unit to a STOPPED state.
     * This call is only valid if the service unit is in a STARTED state.
     *
     * @throws Exception
     * @see Endpoint#stop()
     */
    void stop() throws Exception;

    /**
     * Transition this service unit into the SHUTDOWN state.
     * This call is only valid if the service unit is in a STOPPED state.

     * @throws Exception
     * @see Endpoint#deactivate()
     */
    void shutDown() throws Exception;

    /**
     * Adds an endpoint to this service unit.
     * Adding an endpoint will transition this endpoint into a state
     * which is consistent with this service unit state.
     *
     * @param ep
     * @throws DeploymentException
     */
    void addEndpoint(Endpoint ep) throws DeploymentException;

    /**
     * Removes an endpoint from this service unit.
     * Removing an endpoint will transition it into the SHUTDOWN state.
     *
     * @param ep
     * @throws DeploymentException
     */
    void removeEndpoint(Endpoint ep) throws DeploymentException;

}
