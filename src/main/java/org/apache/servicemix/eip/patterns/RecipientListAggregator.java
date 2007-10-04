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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.eip.support.AbstractSplitter;
import org.apache.servicemix.expression.PropertyExpression;

/**
 * An aggregator specifically written to aggregate messages that have been sent using 
 * a StaticRecipientList pattern.
 *
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="recipient-list-aggregator"
 */
public class RecipientListAggregator extends SplitAggregator {

    public RecipientListAggregator() {
        super();
        this.count = new PropertyExpression(StaticRecipientList.RECIPIENT_LIST_COUNT);
        this.index = new PropertyExpression(StaticRecipientList.RECIPIENT_LIST_INDEX);
        this.corrId = new PropertyExpression(StaticRecipientList.RECIPIENT_LIST_CORRID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.eip.support.Aggregation#buildAggregate(javax.jbi.messaging.NormalizedMessage,
     *      javax.jbi.messaging.MessageExchange, boolean)
     */
    public void buildAggregate(Object aggregation, NormalizedMessage message,
            MessageExchange exchange, boolean timeout) throws Exception {
        super.buildAggregate(aggregation, message, exchange, timeout);
        Object correlationId = message.getProperty(AbstractSplitter.SPLITTER_CORRID);
        message.setProperty(AbstractSplitter.SPLITTER_CORRID, null);
        message.setProperty(StaticRecipientList.RECIPIENT_LIST_CORRID, correlationId);
    }

}
