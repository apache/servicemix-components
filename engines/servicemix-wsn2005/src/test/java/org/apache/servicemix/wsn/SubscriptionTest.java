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
package org.apache.servicemix.wsn;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;

import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.bw_2.InvalidFilterFault;
import org.oasis_open.docs.wsn.bw_2.InvalidProducerPropertiesExpressionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableInitialTerminationTimeFault;

public class SubscriptionTest extends TestCase {

    private JAXBContext context;

    private Unmarshaller unmarshaller;

    private AbstractSubscription subscription;

    protected void setUp() throws Exception {
        context = JAXBContext.newInstance(Subscribe.class);
        unmarshaller = context.createUnmarshaller();
        subscription = new DummySubscription("mySubscription");
    }

    protected Subscribe getSubscription(String file) throws JAXBException, IOException {
        InputStream is = getClass().getResourceAsStream(file);
        Subscribe subscribe = (Subscribe) unmarshaller.unmarshal(is);
        is.close();
        return subscribe;
    }

    public void testWithNilITT() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-nil-itt.xml");
        subscription.validateSubscription(subscribe);
    }

    public void testWithAbsoluteITT() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-abs-itt.xml");
        try {
            subscription.validateSubscription(subscribe);
            fail("Invalid initial termination time used. Fault was expected.");
        } catch (UnacceptableInitialTerminationTimeFault e) {
            // OK
        }
    }

    public void testWithEmptyITT() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-empty-itt.xml");
        try {
            subscription.validateSubscription(subscribe);
            fail("Invalid initial termination time used. Fault was expected.");
        } catch (UnacceptableInitialTerminationTimeFault e) {
            // OK
        }
    }

    public void testWithNoITT() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-no-itt.xml");
        subscription.validateSubscription(subscribe);
    }

    public void testWithUseRaw() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-raw.xml");
        subscription.validateSubscription(subscribe);
    }

    public void testWithProducerProperties() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-pp.xml");
        try {
            subscription.validateSubscription(subscribe);
            fail("ProducerProperties used. Fault was expected.");
        } catch (InvalidProducerPropertiesExpressionFault e) {
            // OK
        }
    }

    public void testWithNoTopic() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-no-topic.xml");
        try {
            subscription.validateSubscription(subscribe);
            fail("ProducerProperties used. Fault was expected.");
        } catch (InvalidFilterFault e) {
            // OK
        }
    }

    public void testWithEPR() throws Exception {
        Subscribe subscribe = getSubscription("subscribe-epr.xml");
        subscription.validateSubscription(subscribe);
    }

}
