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
package org.apache.servicemix.camel.converter;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessagingException;
import javax.xml.transform.Source;

import junit.framework.TestCase;

import org.apache.servicemix.camel.converter.JbiConverter;
import org.apache.servicemix.jbi.exception.FaultException;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link JbiConverter}
 */
public class JbiConverterTest extends TestCase {
    
    private static final Source FAULT_CONTENTS = new StringSource("<fault>serious problem</fault>");
    
    private JbiConverter converter;

    @Override
    protected void setUp() throws Exception {
        this.converter = new JbiConverter();
    }
    
    public void testConvertFaultExceptionToSource() throws MessagingException {
        MockMessageExchange exchange = new MockMessageExchange();
        Fault fault = exchange.createFault();
        fault.setContent(FAULT_CONTENTS);
        FaultException exception = new FaultException("A fault occurred", exchange, fault);
        
        assertEquals(FAULT_CONTENTS, converter.convertFaultExceptionToSource(exception));
    }

}
