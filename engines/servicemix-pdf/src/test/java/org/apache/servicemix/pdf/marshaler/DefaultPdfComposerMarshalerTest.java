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
package org.apache.servicemix.pdf.marshaler;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeFactoryImpl;
import org.apache.servicemix.pdf.marshaler.JaxbPdfComposerMarshaler;
import org.apache.servicemix.pdf.marshaler.PdfComposerDataField;
import org.apache.servicemix.pdf.marshaler.PdfComposerMarshalerSupport;
import org.apache.servicemix.pdf.marshaler.PdfComposerRequest;

import junit.framework.TestCase;

/**
 * <p>
 * Unit tests of the PdfComposer marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public class DefaultPdfComposerMarshalerTest extends TestCase {
    
    private PdfComposerMarshalerSupport marshaler;
    private MessageExchangeFactory factory;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() {
        this.marshaler = new JaxbPdfComposerMarshaler();
        this.factory = new MessageExchangeFactoryImpl(new IdGenerator(), new AtomicBoolean(false));
    }
    
    /**
     * <p>
     * Test the unmarshaling of a valid message content.
     * </p>
     * 
     * @throws Exception if the unmarshaling fails.
     */
    public void testUnmarshaling() throws Exception {
        MessageExchange exchange = factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        message.setContent(new StringSource(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<pdfcomposer:pdfComposerRequest xmlns:pdfcomposer=\"http://servicemix.apache.org/pdfcomposer\">" +
                "<template>test</template>" +
                "<data>" +
                "<field name=\"test\" value=\"test\"/>" +
                "</data>" +
                "</pdfcomposer:pdfComposerRequest>"
            ));
        
        exchange.setMessage(message, "in");
        
        PdfComposerRequest request = marshaler.unmarshal(message);
        
        assertEquals("test", request.getTemplate());
        assertEquals("test", ((PdfComposerDataField) request.getData().get(0)).getName());
        assertEquals("test", ((PdfComposerDataField) request.getData().get(0)).getValue());
    }

}
