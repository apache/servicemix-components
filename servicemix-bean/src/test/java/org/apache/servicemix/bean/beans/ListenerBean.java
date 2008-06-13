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
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.jbi.util.MessageUtil;

/**
 * A simple POJO which implements the {@link MessageExchangeListener} interface
 *
 * @version $Revision: $
 */
public class ListenerBean implements MessageExchangeListener {

    private static final Log LOG = LogFactory.getLog(ListenerBean.class);

    @Resource
    private DeliveryChannel channel;
    
    private MessageExchange lastExchange;
    private String param;

    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        this.lastExchange = exchange;
        LOG.info("Received exchange: " + exchange);
        if (exchange instanceof InOnly) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            MessageUtil.transferInToOut(exchange, exchange);
            channel.send(exchange);
        }
    }

    public MessageExchange getLastExchange() {
        return lastExchange;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}

