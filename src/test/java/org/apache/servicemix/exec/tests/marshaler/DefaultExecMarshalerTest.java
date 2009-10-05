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
package org.apache.servicemix.exec.tests.marshaler;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.servicemix.exec.marshaler.DefaultExecMarshaler;
import org.apache.servicemix.exec.marshaler.ExecMarshalerSupport;
import org.apache.servicemix.exec.marshaler.ExecRequest;
import org.apache.servicemix.exec.marshaler.ExecResponse;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeFactoryImpl;

/**
 * <p>
 * Unit tests on the default exec marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public class DefaultExecMarshalerTest extends TestCase {
    
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
    
    /**
     * <p>
     * Test the unmarshalling of a valid message content.
     * </p>
     * 
     * @throws Exception if the unmarshalling fails.
     */
    public void testUnmarshalling() throws Exception {
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        message.setContent(new StringSource(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<exec:execRequest xmlns:exec=\"http://servicemix.apache.org/exec\">" +
                "<command>ls</command>" +
                "<arguments>" +
                "<argument>-lt</argument>" +
                "<argument>/tmp</argument>" +
                "</arguments>" +
                "</exec:execRequest>"));
                
        exchange.setMessage(message, "in");
        
        ExecRequest execRequest = marshaler.unmarshal(message);
        
        assertEquals("ls", execRequest.getCommand());
        assertEquals("-lt", execRequest.getArguments().get(0));
        assertEquals("/tmp", execRequest.getArguments().get(1));
    }
    
    /**
     * <p>
     * Test the marshalling of a ExecResponse.
     * </p>
     * 
     * @throws Exception if the marshalling fails.
     */
    public void testMarshalling() throws Exception {
        // construct an ExecResponse
        ExecResponse execResponse = new ExecResponse();
        execResponse.setExitCode(0);
        execResponse.setStartTime(1000000);
        execResponse.setEndTime(1000000);
        execResponse.setExecutionDuration(1000000);
        execResponse.setErrorData(new StringBuffer("TEST"));
        execResponse.setOutputData(new StringBuffer("TEST"));
        
        // create an exchange/normalized message
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        
        // marshal the exec response
        marshaler.marshal(execResponse, message);
        
        // get the message content
        SourceTransformer transformer = new SourceTransformer();
        String content = transformer.contentToString(message);
        
        assertEquals(content, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns2:execResponse xmlns:ns2=\"http://servicemix.apache.org/exec\"><endTime>1000000</endTime><errorData/><executionDuration>1000000</executionDuration><exitCode>0</exitCode><outputData/><startTime>1000000</startTime></ns2:execResponse>");
    }
    

}
