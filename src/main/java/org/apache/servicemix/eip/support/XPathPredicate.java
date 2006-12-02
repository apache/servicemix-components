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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.expression.JAXPBooleanXPathExpression;

/**
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="xpath-predicate"
 */
public class XPathPredicate extends JAXPBooleanXPathExpression implements Predicate {

    private static final Log log = LogFactory.getLog(XPathPredicate.class);
    
    public XPathPredicate() {
    }
    
    public XPathPredicate(String xpath) throws Exception {
        super(xpath);
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.components.eip.RoutingRule#matches(javax.jbi.messaging.MessageExchange)
     */
    public boolean matches(MessageExchange exchange) {
        try {
            NormalizedMessage in = exchange.getMessage("in");
            Boolean match = (Boolean) evaluate(exchange, in);
            return Boolean.TRUE.equals(match);
        } catch (Exception e) {
            log.warn("Could not evaluate xpath expression", e);
            return false;
        }
    }

}
