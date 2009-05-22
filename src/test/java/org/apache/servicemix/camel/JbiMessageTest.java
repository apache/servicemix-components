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
package org.apache.servicemix.camel;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.camel.Message;
import org.apache.servicemix.tck.mock.MockNormalizedMessage;

/**
 * Test cases for {@link JbiMessage}
 */
public class JbiMessageTest extends TestCase {
    
    private static final String ATTACHMENT_ID = "attachment.png";
    private static final String ANOTHER_ATTACHMENT_ID = "another_attachment.png"; 
    private static final DataHandler ATTACHMENT = new DataHandler(new FileDataSource("attachment.png"));
    private static final DataHandler ANOTHER_ATTACHMENT = new DataHandler(new FileDataSource("attachment.png"));
    
    public void testAttachmentsWithNormalizedMessage() throws Exception {
        NormalizedMessage jbiMessage = new MockNormalizedMessage();
        jbiMessage.addAttachment(ATTACHMENT_ID, ATTACHMENT);
        
        // make sure the Camel Message also has the attachment
        JbiMessage camelMessage = new JbiMessage(jbiMessage);
        assertSame(ATTACHMENT, camelMessage.getAttachment(ATTACHMENT_ID));
        
        // and ensure that attachments are propagated back to the underlying NormalizedMessage
        camelMessage.addAttachment(ANOTHER_ATTACHMENT_ID, ANOTHER_ATTACHMENT);
        assertSame(ANOTHER_ATTACHMENT, jbiMessage.getAttachment(ANOTHER_ATTACHMENT_ID));
        
        // and they should also be on the Camel Message itself
        camelMessage.setNormalizedMessage(null);
        assertSame(ANOTHER_ATTACHMENT, camelMessage.getAttachment(ANOTHER_ATTACHMENT_ID));
    }
    
    public void testCopyMessage() throws Exception {
        JbiMessage message = new JbiMessage();
        Message copy = message.copy();
        assertTrue(copy instanceof JbiMessage);
    }

}
