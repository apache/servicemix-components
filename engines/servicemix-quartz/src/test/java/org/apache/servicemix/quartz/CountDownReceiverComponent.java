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
package org.apache.servicemix.quartz;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.tck.MessageList;
import org.apache.servicemix.tck.Receiver;

/**
 * A simple receiver component which can be configured to
 * wait for messages before the method getMessageList returns.
 *
 * @version $Revision$
 */
public class CountDownReceiverComponent extends ComponentSupport implements MessageExchangeListener, Receiver {

    public static final QName SERVICE = new QName("http://servicemix.org/example/", "countDownReceiver");
    public static final String ENDPOINT = "countDownReceiver";

    private CountDownLatch countDownLatch;
    private long timeout;
    private int messageCount;
    
    private MessageList messageList = new MessageList();

    public CountDownReceiverComponent(int messageCount, long milliSecTimeOut) {
        this(SERVICE, ENDPOINT, messageCount, milliSecTimeOut);
    }
    
    public CountDownReceiverComponent(QName service, String endpoint, int messageCount, long milliSecTimeOut) {
        super(service, endpoint);
        this.countDownLatch = new CountDownLatch(messageCount);
        this.timeout = milliSecTimeOut;
        this.messageCount = messageCount;
    }
    
    // MessageExchangeListener interface
    //-------------------------------------------------------------------------
    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        NormalizedMessage inMessage = getInMessage(exchange);
        // Copy message to avoid possible closed stream exceptions
        // when using StreamSource
        NormalizedMessage copyMessage = exchange.createMessage();
        getMessageTransformer().transform(exchange, inMessage, copyMessage);
        messageList.addMessage(copyMessage);
        done(exchange);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    // Receiver interface
    //-------------------------------------------------------------------------
    public MessageList getMessageList() {
        if (countDownLatch != null) {
            try {
                countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return messageList;
    }
    
    public void reset() {
        this.countDownLatch = new CountDownLatch(messageCount);
    }
    
}
