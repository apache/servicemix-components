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
package org.apache.servicemix.jms;

import javax.xml.namespace.QName;

import org.apache.activemq.jndi.ActiveMQInitialContextFactory;

public class JmsStandardTest extends AbstractJmsTestCase {

    public void test() throws Exception {
        JmsComponent jms = new JmsComponent();
        JmsEndpoint ep = new JmsEndpoint();
        ep.setInitialContextFactory(ActiveMQInitialContextFactory.class.getName());
        ep.setJndiProviderURL("tcp://localhost:61216");
        ep.setJndiConnectionFactoryName("ConnectionFactory");
        ep.setJndiDestinationName("MyQueue");
        ep.setProcessorName("standard");
        ep.setRoleAsString("provider");
        ep.setService(new QName("service"));
        ep.setEndpoint("endpoint");
        jms.setEndpoints(new JmsEndpoint[] { ep });
        container.activateComponent(jms, "jms");
    }
}
