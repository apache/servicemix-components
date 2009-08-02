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
package org.apache.servicemix.cxfse;


import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.mime.TestMtom;

@WebService(serviceName = "TestMtomService", 
        portName = "TestMtomPort", 
        targetNamespace = "http://cxf.apache.org/mime", 
        endpointInterface = "org.apache.cxf.mime.TestMtom",
            wsdlLocation = "testutils/mtom_xop.wsdl")
public class TestMtomImpl implements TestMtom {
    
    @Resource
    private WebServiceContext wsContext;
    
    public void testXop(Holder<String> name, Holder<DataHandler> attachinfo) {
        
        try {
            if ("runtime exception".equals(name.value)) {
                throw new RuntimeException("throw runtime exception");
            } else if ("property".equals(name.value)){
                MessageContext ctx = wsContext.getMessageContext();
                Message message = ((WrappedMessageContext) ctx).getWrappedMessage();
                String testProperty = (String) message.get("test-property");
                message.put("test-property", testProperty + "ffang");
                return;
            }
            InputStream bis = attachinfo.value.getDataSource().getInputStream();
            byte b[] = new byte[6];
            bis.read(b, 0, 6);
            String attachContent = new String(b);
            name.value = name.value + attachContent;
            
            ByteArrayDataSource source = 
                new ByteArrayDataSource(("test" + attachContent).getBytes(), "application/octet-stream");
            attachinfo.value = new DataHandler(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public org.apache.cxf.mime.types.XopStringType testXopString(
        org.apache.cxf.mime.types.XopStringType data) {
        return null;
    }

}
