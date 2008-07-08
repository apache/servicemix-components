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

import java.io.Serializable;
import java.util.Date;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.servicemix.eip.support.AbstractAggregator;
import org.apache.servicemix.eip.support.AbstractSplitter;
import org.apache.servicemix.expression.Expression;
import org.apache.servicemix.expression.PropertyExpression;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

/**
 * Aggregator can be used to wait and combine several messages.
 * This component implements the  
 * <a href="http://www.enterpriseintegrationpatterns.com/Aggregator.html">Aggregator</a> 
 * pattern.
 * 
 * This aggregator collect  messages with a count, index and correlationId properties.
 * These properties are automatically set by splitters.
 * A timeout may be specified so that the aggregator will not keep data forever if a message is missing.
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="split-aggregator"
 */
public class SplitAggregator extends AbstractAggregator {

    protected Expression count = new PropertyExpression(AbstractSplitter.SPLITTER_COUNT);
    protected Expression index = new PropertyExpression(AbstractSplitter.SPLITTER_INDEX);
    protected Expression corrId = new PropertyExpression(AbstractSplitter.SPLITTER_CORRID);
    
    protected QName aggregateElementName = new QName("aggregate");
    protected QName messageElementName = new QName("message");
    protected String countAttribute = "count";
    protected String indexAttribute = "index";
    
    protected long timeout;
    
    /**
     * @return the aggregateElementName
     */
    public QName getAggregateElementName() {
        return aggregateElementName;
    }

    /**
     * @param aggregateElementName the aggregateElementName to set
     */
    public void setAggregateElementName(QName aggregateElementName) {
        this.aggregateElementName = aggregateElementName;
    }

    /**
     * @return the corrId
     */
    public Expression getCorrId() {
        return corrId;
    }

    /**
     * @param corrId the corrId to set
     */
    public void setCorrId(Expression corrId) {
        this.corrId = corrId;
    }

    /**
     * @return the count
     */
    public Expression getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(Expression count) {
        this.count = count;
    }

    /**
     * @return the countAttribute
     */
    public String getCountAttribute() {
        return countAttribute;
    }

    /**
     * @param countAttribute the countAttribute to set
     */
    public void setCountAttribute(String countAttribute) {
        this.countAttribute = countAttribute;
    }

    /**
     * @return the index
     */
    public Expression getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(Expression index) {
        this.index = index;
    }

    /**
     * @return the indexAttribute
     */
    public String getIndexAttribute() {
        return indexAttribute;
    }

    /**
     * @param indexAttribute the indexAttribute to set
     */
    public void setIndexAttribute(String indexAttribute) {
        this.indexAttribute = indexAttribute;
    }

    /**
     * @return the messageElementName
     */
    public QName getMessageElementName() {
        return messageElementName;
    }

    /**
     * @param messageElementName the messageElementName to set
     */
    public void setMessageElementName(QName messageElementName) {
        this.messageElementName = messageElementName;
    }

    /**
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /*(non-Javadoc)
     * @see org.apache.servicemix.eip.support.AggregationFactory#createAggregation(java.lang.String)
     */
    public Object createAggregation(String correlationID) {
        return new SplitterAggregation(correlationID);
    }

    /*(non-Javadoc)
     * @see org.apache.servicemix.eip.support.AggregationFactory#getCorrelationID(
     *      javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage)
     */
    public String getCorrelationID(MessageExchange exchange, NormalizedMessage message) throws Exception {
        return (String) corrId.evaluate(exchange, message);
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.support.Aggregation#addMessage(
     *      javax.jbi.messaging.NormalizedMessage, javax.jbi.messaging.MessageExchange)
     */
    public boolean addMessage(Object aggregation, NormalizedMessage message, MessageExchange exchange) 
        throws Exception {
        NormalizedMessage[] messages = ((SplitterAggregation) aggregation).messages;
        // Retrieve count, index
        Integer cnt = (Integer) SplitAggregator.this.count.evaluate(exchange, message);
        if (cnt == null) {
            throw new IllegalArgumentException("Property " + AbstractSplitter.SPLITTER_COUNT
                    + " not specified on message");
        }
        if (messages == null) {
            messages = new NormalizedMessage[cnt];
            ((SplitterAggregation) aggregation).messages = messages;
        } else if (cnt != messages.length) {
            throw new IllegalArgumentException("Property " + AbstractSplitter.SPLITTER_COUNT
                    + " is not consistent (received " + cnt + ", was " + messages.length + ")");
        }
        Integer idx = (Integer) SplitAggregator.this.index.evaluate(exchange, message);
        if (idx == null) {
            throw new IllegalArgumentException("Property " + AbstractSplitter.SPLITTER_INDEX
                    + " not specified on message");
        }
        if (idx < 0 || idx >= messages.length) {
            throw new IllegalArgumentException("Index is ouf of bound: " + idx + " [0.." + messages.length + "]");
        }
        if (messages[idx] != null) {
            throw new IllegalStateException("Message with index " + idx + " has already been received");
        }
        // Store message
        messages[idx] = message;
        // Check if all messages have been received
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] == null) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.support.Aggregation#buildAggregate(
     *      javax.jbi.messaging.NormalizedMessage, javax.jbi.messaging.MessageExchange, boolean)
     */
    public void buildAggregate(Object aggregation, NormalizedMessage message, 
            MessageExchange exchange, boolean doTimeout) throws Exception {
        NormalizedMessage[] messages = ((SplitterAggregation) aggregation).messages;
        String correlationId = ((SplitterAggregation) aggregation).correlationId;
        SourceTransformer st = new SourceTransformer();
        Document doc = st.createDocument();
        Element root = createChildElement(aggregateElementName, doc);
        root.setAttribute(countAttribute, Integer.toString(messages.length));
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] != null) {
                Element elem = st.toDOMElement(messages[i]);
                if (messageElementName != null) {
                    Element msg = createChildElement(messageElementName, root);
                    msg.setAttribute(indexAttribute, Integer.toString(i));
                    msg.appendChild(doc.importNode(elem, true));
                } else {
                    root.appendChild(doc.importNode(elem, true));
                }
                if (isCopyProperties()) {
                    copyProperties(messages[i], message);
                }
                if (isCopyAttachments()) {
                    copyAttachments(messages[i], message);
                }
            }
        }
        message.setContent(new DOMSource(doc));
        message.setProperty(AbstractSplitter.SPLITTER_CORRID, correlationId);
    }
    
    protected Element createChildElement(QName name, Node parent) {
        Document doc = parent instanceof Document ? (Document) parent : parent.getOwnerDocument();
        Element elem;
        if ("".equals(name.getNamespaceURI())) {
            elem = doc.createElement(name.getLocalPart());   
        } else {
            elem = doc.createElementNS(name.getNamespaceURI(),
                                       name.getPrefix() + ":" + name.getLocalPart());
        }
        parent.appendChild(elem);
        return elem;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.support.Aggregation#getTimeout()
     */
    public Date getTimeout(Object aggregation) {
        if (timeout > 0) {
            return new Date(System.currentTimeMillis() + timeout);
        }
        return null;
    }
    
    /**
     * 
     * @author gnodet
     */
    protected static class SplitterAggregation implements Serializable {

        /**
         * Serial version UID 
         */
        private static final long serialVersionUID = 8555934895155403923L;
        
        protected NormalizedMessage[] messages;
        protected String correlationId;
      
        public SplitterAggregation(String correlationId) {
            this.correlationId = correlationId;
        }
        
        /**
         * @return the correlationId
         */
        public String getCorrelationId() {
            return correlationId;
        }

        /**
         * @param correlationId the correlationId to set
         */
        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }

        /**
         * @return the messages
         */
        public NormalizedMessage[] getMessages() {
            return messages;
        }

        /**
         * @param messages the messages to set
         */
        public void setMessages(NormalizedMessage[] messages) {
            this.messages = messages;
        }

    }
    
}
