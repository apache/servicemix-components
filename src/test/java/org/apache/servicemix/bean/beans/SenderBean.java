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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;

public class SenderBean implements MessageExchangeListener {

	Thread senderThread;
	private AtomicBoolean keepRunning = new AtomicBoolean(true);
	private QName target;

	@Resource
	private DeliveryChannel channel;

	@PostConstruct
	public void init() {
		senderThread = new Thread(

		new Runnable() {
			public void run() {
				while (keepRunning.get()) {

					try {
						String text = "<Hello/>";
						InOnly exchange = channel
								.createExchangeFactoryForService(target)
								.createInOnlyExchange();
						NormalizedMessage msg = exchange.createMessage();
						msg.setContent(new StringSource(text));
                        exchange.setInMessage(msg);
                        System.out.println("Sending message: " + text);
						channel.send(exchange);
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		});
		senderThread.start();
	}

	@PreDestroy
	public void destroy() {
		keepRunning.set(false);
		if (senderThread != null && senderThread.isAlive()) {
			senderThread.interrupt();
		}
	}

    public void onMessageExchange(MessageExchange messageExchange) throws MessagingException {
        // Do nothing
    }

    public QName getTarget() {
		return target;
	}

	public void setTarget(QName target) {
		this.target = target;
	}

}
