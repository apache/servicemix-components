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
package org.apache.servicemix.eip.support.resequence;

import javax.jbi.messaging.MessageExchange;

/**
 * Compares {@link MessageExchange} sequence elements based on sequence numbers
 * defined by their in-{@link NormalizedMessage}s. This comparator works on
 * sequence numbers of type {@link Long}. Sequence numbers must be stored as
 * {@link NormalizedMessage} properties. The property name under which the
 * sequence number is stored is configured via this comparator's
 * <code>sequenceNumberKey</code> property.
 * 
 * @author Martin Krasser
 * 
 * @org.apache.xbean.XBean element="default-comparator"
 */
public class DefaultComparator implements SequenceElementComparator<MessageExchange> {

    public static final String SEQUENCE_NUMBER_KEY = "org.apache.servicemix.eip.sequence.number";
    
    private static final String IN = "in";
    
    private String sequenceNumberKey;
    
    private boolean sequenceNumberAsString;
    
    public DefaultComparator() {
        sequenceNumberKey = SEQUENCE_NUMBER_KEY;
        sequenceNumberAsString = false;
    }
    
    public String getSequenceNumberKey() {
        return sequenceNumberKey;
    }

    public void setSequenceNumberKey(String sequenceNumberPropertyName) {
        this.sequenceNumberKey = sequenceNumberPropertyName;
    }

    public boolean isSequenceNumberAsString() {
        return sequenceNumberAsString;
    }

    public void setSequenceNumberAsString(boolean sequenceNumberAsString) {
        this.sequenceNumberAsString = sequenceNumberAsString;
    }

    public boolean predecessor(MessageExchange o1, MessageExchange o2) {
        long n1 = getSequenceNumber(o1).longValue();
        long n2 = getSequenceNumber(o2).longValue();
        return n1 == (n2 - 1L);
    }

    public boolean successor(MessageExchange o1, MessageExchange o2) {
        long n1 = getSequenceNumber(o1).longValue();
        long n2 = getSequenceNumber(o2).longValue();
        return n2 == (n1 - 1L);
    }

    public int compare(MessageExchange o1, MessageExchange o2) {
        Long n1 = getSequenceNumber(o1);
        Long n2 = getSequenceNumber(o2);
        return n1.compareTo(n2);
    }

    private Long getSequenceNumber(MessageExchange exchange) {
        Object number = exchange.getMessage(IN).getProperty(sequenceNumberKey);
        if (sequenceNumberAsString) {
            return new Long((String)number);
        } else {
            return (Long)number;
        }
    }
    
}
