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
package org.apache.servicemix.cxfbc.ws.rm;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.RetransmissionInterceptor;

public class OutMessageRecorder extends AbstractPhaseInterceptor {
    
    private static final Logger LOG = Logger.getLogger(OutMessageRecorder.class.getName());
    private List<byte[]> outbound;

    public OutMessageRecorder() {
        super(Phase.PRE_STREAM);
        outbound = new ArrayList<byte[]>();
        addAfter(RetransmissionInterceptor.class.getName());
        addBefore(StaxOutInterceptor.class.getName());
    }
    
    public void handleMessage(Message message) throws Fault {
        OutputStream os = message.getContent(OutputStream.class);
        if (null == os) {
            return;
        }

        WriteOnCloseOutputStream stream = RMUtils.createCachedStream(message, os);
        stream.registerCallback(new RecorderCallback());
    }
    
    public List<byte[]> getOutboundMessages() {
        return outbound;
    } 

    class RecorderCallback implements CachedOutputStreamCallback {

        public void onFlush(CachedOutputStream cos) {  

        }
        
        public void onClose(CachedOutputStream cos) {
            // bytes were already copied after flush
            OutputStream os = cos.getOut();
            if (os instanceof ByteArrayOutputStream) {
                ByteArrayOutputStream bos = (ByteArrayOutputStream)os;
                outbound.add(bos.toByteArray());
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("outbound: " + bos.toString());
                }
                LOG.info("***outbound: " + bos.toString());
            } else {
                LOG.fine("Can't record message from output stream class: " + os.getClass().getName());
            }
        }
        
    }

}

