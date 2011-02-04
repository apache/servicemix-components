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
package org.apache.servicemix.bean.pojos;

import java.util.Set;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.ExchangeStatus;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import org.apache.servicemix.bean.support.BeanSupport;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.common.util.MessageUtil;

/**
 * A simple tracing component which can be placed inside a pipeline
 * to trace the message exchange though the component.
 * If an InOut exchange is received by this component, it will logger the
 * input message and copy it to the output message.
 *
 * @version $Revision: 648504 $
 */
public class LoggingPojo extends BeanSupport implements MessageExchangeListener {

    private Logger logger = LoggerFactory.getLogger(LoggingPojo.class);

    private final SourceTransformer sourceTransformer = new SourceTransformer();

    private int maxMsgDisplaySize = 1500;

    public Logger getLog() {
        return logger;
    }

    public void setLog(Logger log) {
        this.logger = log;
    }

    public int getMaxMsgDisplaySize() {
        return maxMsgDisplaySize;
    }

    public void setMaxMsgDisplaySize(int maxMsgDisplaySize) {
        this.maxMsgDisplaySize = maxMsgDisplaySize;
    }

    /**
     * Intercepts the {@link MessageExchange} to output the message and its
     * properties for debugging purposes.
     *
     * @param exchange A JBI {@link MessageExchange} between two endpoints
     */
    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            // lets dump the incoming message
            NormalizedMessage message = exchange.getMessage("in");
            StringBuffer sb = new StringBuffer();
            sb.append("[\n");
            sb.append("  id: ").append(exchange.getExchangeId()).append('\n');
            sb.append("  mep: ").append(exchange.getPattern()).append('\n');
            sb.append("  status: ").append(exchange.getStatus()).append('\n');
            sb.append("  role: ").append(exchange.getRole() == MessageExchange.Role.CONSUMER ? "consumer" : "provider").append('\n');
            if (exchange.getInterfaceName() != null) {
                sb.append("  interface: ").append(exchange.getInterfaceName()).append('\n');
            }
            if (exchange.getService() != null) {
                sb.append("  service: ").append(exchange.getService()).append('\n');
            }
            if (exchange.getEndpoint() != null) {
                sb.append("  endpoint: ").append(exchange.getEndpoint().getEndpointName()).append('\n');
            }
            if (exchange.getOperation() != null) {
                sb.append("  operation: ").append(exchange.getOperation()).append('\n');
            }
            if (exchange.getPropertyNames().size() > 0) {
                sb.append("  properties: [").append('\n');
                for (String key : (Set<String>) exchange.getPropertyNames()) {
                    sb.append("      ").append(key).append(" = ");
                    Object contents = exchange.getProperty(key);
                    if (contents instanceof Source) {
                        try {
                            contents = sourceTransformer.toString((Source) contents);
                        } catch (Exception e) { }
                    }
                    sb.append(contents);
                    sb.append('\n');
                }
                sb.append("  ]").append('\n');
            }
            display(exchange, "in", sb);
            logger.info("Exchange received " + sb.toString());
            if (exchange instanceof InOut) {
                MessageUtil.transferInToOut(exchange, exchange);
                send(exchange);
            } else {
                done(exchange);
            }
        }
    }

    private void display(MessageExchange exchange, String msg, StringBuffer sb) {
        NormalizedMessage message = exchange.getMessage(msg);
        if (message != null) {
            sb.append("  ").append(msg).append(": [").append('\n');
            sb.append("    content: ");
            try {
                if (message.getContent() != null) {
                    Node node = sourceTransformer.toDOMNode(message.getContent());
                    message.setContent(new DOMSource(node));
                    String str = sourceTransformer.toString(node);
                    if (maxMsgDisplaySize < 0 || str.length() <= maxMsgDisplaySize) { 
                        sb.append(str);
                    } else {
                        sb.append(str.substring(0, maxMsgDisplaySize)).append("...");
                    }                                         
                } else {
                    sb.append("null");
                }
            } catch (Exception e) {
                sb.append("Unable to display: ").append(e);
            }
            sb.append('\n');
            if (message.getAttachmentNames().size() > 0) {
                sb.append("    attachments: [").append('\n');
                for (String key : (Set<String>) message.getAttachmentNames()) {
                    sb.append("      ").append(key).append(" = ").append(message.getAttachment(key)).append('\n');
                }
                sb.append("    ]").append('\n');
            }
            if (message.getPropertyNames().size() > 0) {
                sb.append("    properties: [").append('\n');
                for (String key : (Set<String>) message.getPropertyNames()) {
                    sb.append("      ").append(key).append(" = ");
                    Object contents = message.getProperty(key);
                    if (contents instanceof Source) {
                        try {
                            contents = sourceTransformer.toString((Source) contents);
                        } catch (Exception e) { }
                    }
                    sb.append(contents);
                    sb.append('\n');
                }
                sb.append("    ]").append('\n');
            }
            sb.append("  ]").append('\n');
        }
    }

}
