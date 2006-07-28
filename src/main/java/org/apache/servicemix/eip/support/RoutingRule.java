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
package org.apache.servicemix.eip.support;

/**
 * The RoutingRule interface is used by content based routers.
 * If the rule predicate matches the MessageExchange, the
 * target defined on the rule will be used as the destination for
 * the given MessageExchange.
 *  
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="routing-rule" 
 */
public class RoutingRule {
    
    private Predicate predicate;
    private ExchangeTarget target;
    
    public RoutingRule() {
    }
    
    public RoutingRule(Predicate predicate, ExchangeTarget target) {
        this.predicate = predicate;
        this.target = target;
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.components.eip.support.RoutingRule#getPredicate()
     */
    public Predicate getPredicate() {
        return predicate;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.components.eip.support.RoutingRule#getTarget()
     */
    public ExchangeTarget getTarget() {
        return target;
    }

    /**
     * @param predicate The predicate to set.
     */
    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    /**
     * @param target The target to set.
     */
    public void setTarget(ExchangeTarget target) {
        this.target = target;
    }

}
