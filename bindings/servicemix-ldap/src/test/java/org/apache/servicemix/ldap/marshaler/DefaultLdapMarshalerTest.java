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
package org.apache.servicemix.ldap.marshaler;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.messaging.MessageExchangeFactoryImpl;

import junit.framework.TestCase;

/**
 * <p>
 * Unit tests on the default LDAP marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public class DefaultLdapMarshalerTest extends TestCase {
    
    private LdapMarshalerSupport marshaler;
    private MessageExchangeFactory messageExchangeFactory;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        this.marshaler = new DefaultLdapMarshaler();
        this.messageExchangeFactory = new MessageExchangeFactoryImpl(new IdGenerator(), new AtomicBoolean(false));
    }
    
    /**
     * <p>
     * Test the marshaling of a naming enumeration.
     * </p>
     * 
     * @throws Exception in case of marshaling failure.
     */
    public void testMarshaling() throws Exception {
        // create a testing naming enumeration
        TestNamingEnumeration namingEnumeration = new TestNamingEnumeration();
        
        // create a InOnly exchange
        InOnly exchange = messageExchangeFactory.createInOnlyExchange();
        // create the in message
        NormalizedMessage in = exchange.createMessage();
        
        // marshal the naming enumeration into the message
        marshaler.marshal(in, namingEnumeration);
        
        // check the message content
        assertEquals("<entries><entry><dn>test</dn><attributes><attribute><name>second</name><values></values></attribute><attribute><name>first</name><values></values></attribute></attributes></entry></entries>",
                new SourceTransformer().toString(in.getContent()));
    }

}

class TestNamingEnumeration implements NamingEnumeration {
    
    private SearchResult searchResult;
    private boolean iterate = true;
    
    public TestNamingEnumeration() {
        Attributes attributes = new BasicAttributes();
        Attribute first = new BasicAttribute("first");
        attributes.put(first);
        Attribute second = new BasicAttribute("second");
        attributes.put(second);
        this.searchResult = new SearchResult("test", null, attributes);
    }
    
    public void close() { }
    
    public boolean hasMore() { 
        return iterate;
    }
    
    public SearchResult next() {
        if (iterate) {
            iterate = false;
            return searchResult;
        }
        return null;
    }
    
    public boolean hasMoreElements() { 
        return iterate; 
    }
    
    public SearchResult nextElement() { 
        if (iterate) {
            iterate = false;
            return searchResult;
        }
        return null;
    }
    
    
}
