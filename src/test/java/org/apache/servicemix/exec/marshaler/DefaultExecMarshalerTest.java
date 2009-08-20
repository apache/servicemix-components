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
package org.apache.servicemix.exec.marshaler;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeFactoryImpl;

/**
 * Unit tests on the default exec marshaler.
 * 
 * @author jbonofre
 */
public class DefaultExecMarshalerTest extends TestCase {
    
    private static final String COMMAND = "ls";
    private static final String FIRST_ARG = "-lt";
    private static final String SECOND_ARG = "/tmp";
    
    private static final String MSG_VALID = "<message>"
        + "<command>" + COMMAND + "</command>"
        + "<arguments>"
        + "<argument>" + FIRST_ARG + "</argument>"
        + "<argument>" + SECOND_ARG + "</argument>"
        + "</arguments>"
        + "</message>";
    
    private ExecMarshalerSupport marshaler;
    private MessageExchangeFactory factory;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        this.marshaler = new DefaultExecMarshaler();
        this.factory = new MessageExchangeFactoryImpl(new IdGenerator(), new AtomicBoolean(false));
    }
    
    public void testValidMessage() throws Exception {
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        message.setContent(new StringSource(MSG_VALID));
        exchange.setMessage(message, "in");
        SourceTransformer transformer = new SourceTransformer();
        //String execCommand = marshaler.constructExecCommand(transformer.toDOMDocument(message));
        
        //assertEquals("ls -lt /tmp", execCommand);
    }
    
    

}
