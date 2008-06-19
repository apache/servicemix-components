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
package org.apache.servicemix.soap.interceptors.mime;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.servicemix.jbi.util.ByteArrayDataSource;
import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.AbstractInterceptor;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class AttachmentsOutInterceptor extends AbstractInterceptor {

    public static final String PIPE_ATTACHMENT_STREAM = "PipeAttachmentStream";
    
    public static final String SOAP_PART_ID = "soap-request";
    
    public void handleMessage(final Message message) {
        if (message.getAttachments().size() == 0) {
            return;
        }
        OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            throw new NullPointerException("OutputStream content not found");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.setContent(OutputStream.class, baos);
            message.put(Message.CONTENT_TYPE, "multipart/related; type=\"text/xml\"; start=\"<" + SOAP_PART_ID + ">\"");
            InterceptorChain chain = message.get(InterceptorChain.class);
            chain.doIntercept(message);
            writeMultipartMessage(message, os, baos.toByteArray());
        } catch (MessagingException e) {
            throw new Fault(e);
        } catch (IOException e) {
            throw new Fault(e);
        }
    }
    
    protected boolean getBoolean(Message message, String name, boolean def) {
        Object b = message.get(name);
        if (b instanceof Boolean) {
            return ((Boolean) b).booleanValue();
        } else if (b != null) {
            return Boolean.parseBoolean(b.toString());
        } else {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private void writeMultipartMessage(Message message, OutputStream out, byte[] data) throws MessagingException, IOException {
        MimeMultipart parts = new MimeMultipart("related; type=\"text/xml\"; start=\"<" + SOAP_PART_ID + ">\"");
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage mime = new MimeMessage(session);
        // Add soap part
        MimeBodyPart soapPart = new MimeBodyPart();
        soapPart.setContentID("<" + SOAP_PART_ID + ">");
        soapPart.addHeader("Content-Transfer-Encoding", "8bit");
        soapPart.setDataHandler(new DataHandler(new ByteArrayDataSource(data, "text/xml")));
        parts.addBodyPart(soapPart);
        // Add attachments
        for (Iterator itr = message.getAttachments().entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = (Map.Entry) itr.next();
            String id = (String) entry.getKey();
            DataHandler dh = (DataHandler) entry.getValue();
            MimeBodyPart part = new MimeBodyPart();
            part.setDataHandler(dh);
            part.setContentID("<" + id + ">");
            part.addHeader("Content-Transfer-Encoding", "binary");
            parts.addBodyPart(part);
        }
        mime.setContent(parts);
        mime.setHeader(Message.CONTENT_TYPE, parts.getContentType());
        // We do not want headers, so 
        //  * retrieve all headers
        //  * skip first 2 bytes (CRLF)
        mime.saveChanges();
        Enumeration<Header> headersEnum = mime.getAllHeaders();
        List<String> headersList = new ArrayList<String>();
        while (headersEnum.hasMoreElements()) {
            headersList.add(headersEnum.nextElement().getName().toLowerCase());
        }
        String[] headers = headersList.toArray(new String[0]);
        // Skip first 2 bytes
        OutputStream os = new FilterOutputStream(out) {
            private int nb = 0;
            public void write(int b) throws IOException {
                if (++nb > 2) {
                    super.write(b);
                }
            }
        };
        // Write
        mime.writeTo(os, headers);
    }

}
