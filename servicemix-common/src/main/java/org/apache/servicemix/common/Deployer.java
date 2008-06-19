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

import javax.jbi.management.DeploymentException;

/**
 * A deployer is responsible for handling one type of artifact deployment.
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 * @since 3.0
 */
public interface Deployer {

	/**
	 * Check if this deployer is able to handle a given artifact.
	 * 
	 * @param serviceUnitName the name of the service unit 
	 * @param serviceUnitRootPath the path of the exploded service unit
	 * @return <code>true</code> if this deployer can handle the given artifact
	 */
    boolean canDeploy(String serviceUnitName, 
                      String serviceUnitRootPath);
    
    /**
     * Actually deploys the given service unit and build a ServiceUnit object
     * that contains endpoints.
     * 
	 * @param serviceUnitName the name of the service unit 
	 * @param serviceUnitRootPath the path of the exploded service unit
     * @return a service unit containing endpoints
     * @throws DeploymentException if an error occurs
     */
    ServiceUnit deploy(String serviceUnitName, 
                       String serviceUnitRootPath) throws DeploymentException;

    /**
     * Undeploys the given service unit.
     * @param su the service unit to undeploy
     */
	void undeploy(ServiceUnit su) throws DeploymentException;
    
}
