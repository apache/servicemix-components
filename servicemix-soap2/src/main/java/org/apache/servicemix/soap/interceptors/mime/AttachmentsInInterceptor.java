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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.AbstractInterceptor;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class AttachmentsInInterceptor extends AbstractInterceptor {

    public static final String MULTIPART_CONTENT = "multipart/";
    
    public void handleMessage(Message message) {
        InputStream is = message.getContent(InputStream.class);
        if (is == null) {
            throw new NullPointerException("InputStream content not found");
        }
        String contentType = (String) message.get(Message.CONTENT_TYPE);
        if (contentType != null && contentType.toLowerCase().startsWith(MULTIPART_CONTENT)) {
            try {
                Session session = Session.getDefaultInstance(new Properties());
                is = new SequenceInputStream(new ByteArrayInputStream(new byte[] { 13, 10 }), is);
                MimeMessage mime = new MimeMessage(session, is);
                mime.setHeader(Message.CONTENT_TYPE, contentType);
                read(message, mime);
            } catch (IOException e) {
                throw new Fault(e);
            } catch (MessagingException e) {
                throw new Fault(e);
            }
        }
    }

    public void read(Message message, MimeMessage mime) throws MessagingException, IOException {
        final Object content = mime.getContent();
        if (content instanceof MimeMultipart == false) {
            throw new UnsupportedOperationException("Expected a javax.mail.internet.MimeMultipart object but found a " + content.getClass());
        }
        MimeMultipart multipart = (MimeMultipart) content;
        ContentType type = new ContentType(mime.getContentType());
        String contentId = type.getParameter("start");
        // Get request
        MimeBodyPart contentPart = null;
        if (contentId != null) {
            contentPart = (MimeBodyPart) multipart.getBodyPart(contentId);
        } else {
            for (int i = 0; i < multipart.getCount(); i++) {
              MimeBodyPart contentPart2 = (MimeBodyPart) multipart.getBodyPart(i);
              String contentType = contentPart2.getContentType();
              
              if (contentType.indexOf("xml") >= 0) {
                contentPart = contentPart2;
                break;
              }
            }
        }
        // Set new content
        message.setContent(InputStream.class, contentPart != null ? contentPart.getInputStream() : null);
        // Get attachments
        for (int i = 0; i < multipart.getCount(); i++) {
            MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
            if (part != contentPart) {
                String id = part.getContentID();
                if (id == null) {
                    id = "Part" + i;
                } else if (id.startsWith("<")) {
                    id = id.substring(1, id.length() - 1);
                }
                message.getAttachments().put(id, part.getDataHandler());
            }
        }
    }

}
