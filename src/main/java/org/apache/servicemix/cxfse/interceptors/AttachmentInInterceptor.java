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
package org.apache.servicemix.cxfse.interceptors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessageExchange;

import org.apache.cxf.attachment.AttachmentImpl;

import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.servicemix.jbi.messaging.NormalizedMessageImpl;

public class AttachmentInInterceptor extends AbstractPhaseInterceptor<Message> {
     
    
    public AttachmentInInterceptor() {
        super(Phase.RECEIVE);
    }
    
    public void handleMessage(Message message) {
        List<Attachment> attachmentList = new ArrayList<Attachment>();
        MessageExchange exchange = message.get(MessageExchange.class);
        NormalizedMessageImpl norMessage = 
            (NormalizedMessageImpl) exchange.getMessage("in");
        Iterator<String> iter = norMessage.listAttachments();
        while (iter.hasNext()) {
            String id = iter.next();
            DataHandler dh = norMessage.getAttachment(id);
            attachmentList.add(new AttachmentImpl(id, dh));
        }
        
        message.setAttachments(attachmentList);
    }

    
}
