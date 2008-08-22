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
package org.apache.servicemix.cxfbc.interceptors;

import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class ParseContentTypeInterceptor extends AbstractPhaseInterceptor<Message> {

    public ParseContentTypeInterceptor() {
        super(Phase.RECEIVE);
        addBefore(AttachmentInInterceptor.class.getName());
    }
    
    public void handleMessage(Message message) throws Fault {
        Object headers = message.get(Message.PROTOCOL_HEADERS);
        if (headers != null) {
            String ct = headers.toString();
            //parse content-type from protocal headers
            if (ct.indexOf("content-type") != -1) {
                ct = ct.substring(ct.indexOf("content-type") + 14, 
                        ct.indexOf("]", ct.indexOf("content-type")));
            }
            message.put(Message.CONTENT_TYPE, ct);
        }
        
    }
    

}
