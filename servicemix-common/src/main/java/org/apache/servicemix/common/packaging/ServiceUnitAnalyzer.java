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
package org.apache.servicemix.common.packaging;

import java.io.File;
import java.util.List;

/**
 * Provides a interface that components can implement, if they implement this
 * interface and expose the name of the analyzer in their Maven Project Object
 * Model then the tooling can use this to analyze a Service Unit for the
 * component during packaging to generate the consumes and provides elements for
 * the service unit's jbi.xml
 * 
 * @author Philip Dodds
 * @version $Revision: 426415 $
 * @since 3.0
 * 
 * @see Consumes, {@link Consumes}
 * @see Provides, {@link Provides}
 */
public interface ServiceUnitAnalyzer {

	/**
	 * Initializes the analyzer based on the root directory of an exploded
	 * service unit
	 * 
	 * @param explodedServiceUnitRoot
	 */
	public void init(File explodedServiceUnitRoot);

	/**
	 * Returns a list of Consumes representing the service unit being analyzed
	 * 
	 * @return A list of Consumes
	 */
	public List getConsumes();

	/**
	 * Returns a list of Provides representing the service unit being analyzed
	 * 
	 * @return A list of provides
	 */
	public List getProvides();

}
