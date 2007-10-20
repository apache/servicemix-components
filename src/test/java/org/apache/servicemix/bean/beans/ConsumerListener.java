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
package org.apache.servicemix.bean.beans;

import javax.annotation.Resource;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.jbi.util.MessageUtil;

public class ConsumerListener implements MessageExchangeListener {

    @Resource
    private DeliveryChannel channel;

    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        if (exchange.getRole() == MessageExchange.Role.CONSUMER) {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                MessageExchange io = (MessageExchange) exchange.getProperty("exchange");
                MessageUtil.transferOutToOut(exchange, io);
                io.setProperty("exchange", exchange);
                channel.send(io);
            } else if (exchange.getStatus() == ExchangeStatus.DONE) {
                MessageExchange io = (MessageExchange) exchange.getProperty("exchange");
                io.setStatus(ExchangeStatus.DONE);
                channel.send(io);
            }
        } else {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                MessageExchangeFactory factory = channel.createExchangeFactory();
                InOut io = factory.createInOutExchange();
                MessageUtil.transferInToIn(exchange, io);
                io.setService(new QName("echo"));
                io.setProperty("exchange", exchange);
                channel.send(io);
            } else if (exchange.getStatus() == ExchangeStatus.DONE) {
                // Do nothing
            }
        }
    }

}
