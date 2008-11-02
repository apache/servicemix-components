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
package org.apache.servicemix.eip;

import javax.xml.namespace.QName;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.eip.patterns.StaticRecipientList;
import org.apache.servicemix.eip.patterns.RecipientListAggregator;
import org.apache.servicemix.eip.support.ExchangeTarget;

public class RecipientListAggregatorTest extends AbstractEIPTest {

    private StaticRecipientList recipientList;
    private RecipientListAggregator aggregator;

    public void testReportErrors() throws Exception {
        recipientList = new StaticRecipientList();
        recipientList.setReportErrors(true);
        recipientList.setRecipients(new ExchangeTarget[] {
                createServiceExchangeTarget(new QName("aggregator")),
                createServiceExchangeTarget(new QName("aggregator")),
        });
        configurePattern(recipientList);
        activateComponent(recipientList, "recipientList");

        aggregator = new RecipientListAggregator();
        aggregator.setReportErrors(true);
        aggregator.setTarget(createServiceExchangeTarget(new QName("error")));
        configurePattern(aggregator);
        activateComponent(aggregator, "aggregator");

        activateComponent(new ReturnErrorComponent(), "error");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("recipientList"));
        NormalizedMessage message = me.getMessage("in");
        message.setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());

        listener.assertExchangeCompleted();
    }

}
