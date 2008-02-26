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
package org.apache.servicemix.osworkflow.conditions;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Condition;
import com.opensymphony.workflow.WorkflowException;

public class ExampleCondition implements Condition {

    private static Log logger = LogFactory.getLog(ExampleCondition.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.opensymphony.workflow.Condition#passesCondition(java.util.Map,
     *      java.util.Map, com.opensymphony.module.propertyset.PropertySet)
     */
    public boolean passesCondition(Map transientVars, Map args,
            PropertySet propertySet) throws WorkflowException {
        boolean result = false;

        logger.info("Checking condition...");

        if (propertySet.exists("ExampleKey")) {
            logger.info("PropertySet contains ExampleKey...");
            if (propertySet.getString("ExampleKey").equals("ExampleValue")) {
                logger.info("Value is correct...");
                result = true;
            } else {
                logger.info("Value is not correct...");
                result = false;
            }
        } else {
            logger.info("PropertySet does not contain ExampleKey...");
            result = false;
        }

        return result;
    }
}
