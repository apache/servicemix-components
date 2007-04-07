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
package org.apache.servicemix.eip.patterns;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

import org.apache.servicemix.eip.support.AbstractContentBasedRouter;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.eip.support.RoutingRule;

/**
 * ContentBasedRouter can be used for all kind of content-based routing.
 * This component implements the  
 * <a href="http://www.enterpriseintegrationpatterns.com/ContentBasedRouter.html">Content-Based Router</a> 
 * pattern.
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="content-based-router"
 *                  description="A Content-Based Router"
 */
public class ContentBasedRouter extends AbstractContentBasedRouter {

    /**
     * Routing rules that are evaluated to find the target destination
     */
    private RoutingRule[] rules;
    
    /**
     * @return Returns the rules.
     */
    public RoutingRule[] getRules() {
        return rules;
    }

    /**
     * @param rules The rules to set.
     */
    public void setRules(RoutingRule[] rules) {
        this.rules = rules;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#validate()
     */
    public void validate() throws DeploymentException {
        super.validate();
        // Check rules
        if (rules == null || rules.length == 0) {
            throw new IllegalArgumentException("rules should contain at least one RoutingRule");
        }
    }

    /**
     * Find the target destination for the given JBI exchange
     * @param exchange
     * @return the target for the given exchange
     * @throws Exception
     */
    protected ExchangeTarget getDestination(MessageExchange exchange) throws Exception {
        for (int i = 0; i < rules.length; i++) {
            if (rules[i].getPredicate() == null
                || rules[i].getPredicate().matches(exchange)) {
                return rules[i].getTarget();
            }
        }
        throw new MessagingException("No matching rule found for exchange");
    }

}
