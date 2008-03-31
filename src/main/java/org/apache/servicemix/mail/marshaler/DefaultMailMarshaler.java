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
package org.apache.servicemix.mail.marshaler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Enumeration;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.ByteArrayDataSource;
import org.apache.servicemix.jbi.util.FileUtil;

/**
 * this is the default marshaler for conversion between the normalized message
 * format and the mail message format
 * 
 * @author lhein
 */
public class DefaultMailMarshaler extends AbstractMailMarshaler {
    private static Log log = LogFactory.getLog(DefaultMailMarshaler.class);

    /*
     * (non-Javadoc)
     * 
     * @see net.compart.jbi.mail.AMailMarshallerSupport#convertMailToJBI(javax.jbi.messaging.MessageExchange,
     *      javax.jbi.messaging.NormalizedMessage, javax.mail.internet.MimeMessage)
     */
    @Override
    public void convertMailToJBI(MessageExchange exchange, NormalizedMessage nmsg, MimeMessage mailMsg)
        throws javax.mail.MessagingException {
        // copy headers
        copyHeaders(exchange, nmsg, mailMsg);

        // now copy the mail body and the attachments
        copyBodyAndAttachments(exchange, nmsg, mailMsg);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.mail.marshaler.AbstractMailMarshaler#
     * convertJBIToMail(javax.mail.internet.MimeMessage, javax.jbi.messaging.MessageExchange, 
     * javax.jbi.messaging.NormalizedMessage, java.lang.String, java.lang.String)
     */
    @Override
    public void convertJBIToMail(MimeMessage mimeMessage, MessageExchange exchange, 
                                 NormalizedMessage nmsg, String configuredSender, String configuredReceiver)
        throws javax.mail.MessagingException {
        try {
            // first fill the headers of the mail
            fillMailHeaders(mimeMessage, exchange, nmsg, configuredSender, configuredReceiver);

            // fill the body and attachments
            fillMailBodyAndAttachments(mimeMessage, exchange, nmsg);
        } catch (Exception ex) {
            throw new javax.mail.MessagingException(ex.getMessage(), ex);
        }
    }

    /**
     * fills all attachments from the normalized message into the mail message
     * 
     * @param mimeMessage the mail message to fill
     * @param exchange the received exchange
     * @param nmsg the normalized message received
     * @throws Exception on any errors
     */
    protected void fillMailBodyAndAttachments(MimeMessage mimeMessage, MessageExchange exchange,
                                              NormalizedMessage nmsg) throws Exception {
        // if there are attachments, then a multipart mime mail with
        // attachments will be sent
        if (nmsg.getAttachmentNames().size() > 0) {
            Iterator itAttNames = nmsg.getAttachmentNames().iterator();

            if (itAttNames.hasNext()) {
                // there is at least one attachment

                // Create a Multipart
                MimeMultipart multipart = new MimeMultipart();

                // fill the body with text, html or both
                fillMailBody(mimeMessage, exchange, nmsg, multipart);

                BodyPart messageBodyPart = null;

                // loop the existing attachments and put them to the mail
                while (itAttNames.hasNext()) {
                    String oneAttachmentName = (String)itAttNames.next();
                    
                    // For some strange reason the java mail tries to 
                    // read the input stream of the attachments twice.
                    // this is impossible if the used data source is of 
                    // type StreamDataSource (used for example in the
                    // binary file marshaler)
                    // ----------------------------------------------------
                    // As a not so nice workaround we do store the contents
                    // of the stream as a temporary file and then attach
                    // this file as FileDataSource to the mails attachment.
                    // This may cause some slight performance loss.
                    DataHandler dh = nmsg.getAttachment(oneAttachmentName);
                    File f = File.createTempFile("" + System.currentTimeMillis() + "-", dh.getDataSource().getName());
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
                    FileUtil.copyInputStream(dh.getInputStream(), bos);
                    bos.close();
                    
                    log.debug("Saved temp file: " + f.getName() + " with length: " + f.length());
                    
                    // add the file to the temporary resources list
                    addTemporaryResource(f);                    
                    // Create another body part
                    messageBodyPart = new MimeBodyPart();
                    // Set the data handler to the attachment
                    messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(f)));
                    // Set the filename
                    messageBodyPart.setFileName(dh.getDataSource().getName());
                    // Set Disposition
                    messageBodyPart.setDisposition(Part.ATTACHMENT);
                    // Add part to multipart
                    multipart.addBodyPart(messageBodyPart);
                }
                // Put parts in message
                mimeMessage.setContent(multipart);
            }
        } else {
            // fill the body with text, html or both
            fillMailBody(mimeMessage, exchange, nmsg, null);
        }
    }

    /**
     * fills the body of the mail
     * 
     * @param mimeMessage the mail message
     * @param exchange the jbi exchange
     * @param nmsg the normalized message
     * @param content the content of a multipart to use or null
     * @throws Exception on errors
     */
    protected void fillMailBody(MimeMessage mimeMessage, MessageExchange exchange, NormalizedMessage nmsg,
                                MimeMultipart content) throws Exception {
        // check if we are going to send a plain text mail or html or both
        boolean isPlainTextMessage = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT) != null;
        boolean isHtmlMessage = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML) != null;

        if (isPlainTextMessage && !isHtmlMessage) {
            // a plain text mail will be sent 
            if (content != null) {
                content.setSubType("mixed");
                MimeBodyPart textBodyPart = new MimeBodyPart();
                textBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT).toString(),
                                        "text/plain");
                content.addBodyPart(textBodyPart);
            } else {
                mimeMessage.setText(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT).toString());
            }
        } else if (isHtmlMessage && !isPlainTextMessage) {
            // a html message will be sent 
            if (content != null) {
                content.setSubType("mixed");
                MimeBodyPart htmlBodyPart = new MimeBodyPart();
                htmlBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML).toString(),
                                        "text/html");
                content.addBodyPart(htmlBodyPart);
            } else {
                content = new MimeMultipart("mixed");
                MimeBodyPart htmlBodyPart = new MimeBodyPart();
                htmlBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML).toString(),
                                        "text/html");
                content.addBodyPart(htmlBodyPart);
                // Put parts in message
                mimeMessage.setContent(content);
            }
        } else if (isHtmlMessage && isPlainTextMessage) {
            // both parts will be sent 
            if (content != null) {
                content.setSubType("mixed");
                MimeBodyPart textBodyPart = new MimeBodyPart();
                MimeBodyPart htmlBodyPart = new MimeBodyPart();
                textBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT).toString(),
                                        "text/plain");
                htmlBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML).toString(),
                                        "text/html");
                content.addBodyPart(textBodyPart);
                content.addBodyPart(htmlBodyPart);
            } else {
                content = new MimeMultipart("mixed");
                MimeBodyPart textBodyPart = new MimeBodyPart();
                MimeBodyPart htmlBodyPart = new MimeBodyPart();
                textBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT).toString(),
                                        "text/plain");
                htmlBodyPart.setContent(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML).toString(),
                                        "text/html");
                content.addBodyPart(textBodyPart);
                content.addBodyPart(htmlBodyPart);
                // Put parts in message
                mimeMessage.setContent(content);
            }
        } else {
            if (nmsg.getContent() != null) {
                // use the content of the message
                SourceTransformer st = new SourceTransformer();
                try {
                    st.toDOMDocument(nmsg);
    
                    // a html message will be sent
                    if (content != null) {
                        content.setSubType("mixed");
                        MimeBodyPart htmlBodyPart = new MimeBodyPart();
                        htmlBodyPart.setContent(st.contentToString(nmsg), "text/html");
                        content.addBodyPart(htmlBodyPart);
                    } else {
                        content = new MimeMultipart("mixed");
                        MimeBodyPart htmlBodyPart = new MimeBodyPart();
                        htmlBodyPart.setContent(st.contentToString(nmsg), "text/html");
                        content.addBodyPart(htmlBodyPart);
                        // Put parts in message
                        mimeMessage.setContent(content);
                    }
                } catch (Exception ex) {
                    // no xml document - plain text used now
                    if (content != null) {
                        content.setSubType("mixed");
                        MimeBodyPart textBodyPart = new MimeBodyPart();
                        textBodyPart.setContent(st.contentToString(nmsg), "text/plain");
                        content.addBodyPart(textBodyPart);
                    } else {
                        // Put text in message
                        mimeMessage.setText(st.contentToString(nmsg));
                    }
                }
            } else {
                // a plain text mail will be sent
                if (content != null) {
                    content.setSubType("mixed");
                    MimeBodyPart textBodyPart = new MimeBodyPart();
                    textBodyPart.setContent(AbstractMailMarshaler.DUMMY_CONTENT, "text/plain");
                    content.addBodyPart(textBodyPart);
                } else {
                    mimeMessage.setText(AbstractMailMarshaler.DUMMY_CONTENT);
                }
            }
        }
    }

    /**
     * fills the mail headers according to the normalized message headers
     * 
     * @param mimeMessage the mail message to fill
     * @param exchange the exchange received
     * @param nmsg the normalized message received
     * @param configuredSender the configured sender from xbean
     * @param configuredReceiver the configured receiver from xbean
     * @throws Exception on errors
     */
    protected void fillMailHeaders(MimeMessage mimeMessage, MessageExchange exchange, 
                                   NormalizedMessage nmsg, String configuredSender, String configuredReceiver)
        throws Exception {
        // fill the "To" field of the mail
        // if there is a TO property, this overrides the standard receiver
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TO) != null) {
            Address[] to = InternetAddress.parse(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TO)
                .toString());
            if (to != null) {
                mimeMessage.setRecipients(Message.RecipientType.TO, to);
            } 
        // there is no TO property, so try the configured receiver
        } else {
            if (configuredReceiver != null) {
                Address[] to = InternetAddress.parse(configuredReceiver);
                if (to != null) {
                    mimeMessage.setRecipients(Message.RecipientType.TO, to);
                } 
            }
        }

        // fill the "Cc" field of the mail
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_CC) != null) {
            Address[] cc = InternetAddress.parse(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_CC)
                .toString());
            if (cc != null) {
                mimeMessage.setRecipients(Message.RecipientType.CC, cc);
            }
        }

        // fill the "Bcc" field of the mail
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_BCC) != null) {
            Address[] bcc = InternetAddress.parse(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_BCC)
                .toString());
            if (bcc != null) {
                mimeMessage.setRecipients(Message.RecipientType.BCC, bcc);
            }
        }

        // fill the "From" field of the mail
        if (configuredSender != null && configuredSender.trim().length() > 0) {
            // if sender is configured through xbean then use it
            Address from = InternetAddress.parse(configuredSender)[0];
            if (from != null) {
                mimeMessage.setFrom(from);
            }
        } else if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_FROM) != null) {
            // use the delivered From field from the message
            Address from = InternetAddress.parse(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_FROM)
                .toString())[0];
            if (from != null) {
                mimeMessage.setFrom(from);
            }
        } else {
            // there was no From field delivered, so use the default sender
            Address from = InternetAddress.parse(getDefaultSenderForOutgoingMails())[0];
            if (from != null) {
                mimeMessage.setFrom(from);
            }
        }

        // fill the "Subject" field of the mail
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT) != null) {
            String subject = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT).toString();
            if (subject != null) {
                mimeMessage.setSubject(subject);
            } else {
                mimeMessage.setSubject(AbstractMailMarshaler.DUMMY_SUBJECT);
            }
        } else {
            mimeMessage.setSubject(AbstractMailMarshaler.DUMMY_SUBJECT);
        }        

        // fill the "Date" field of the mail
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_SENTDATE) != null) {
            String sentDate = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_SENTDATE).toString();
            if (sentDate != null) {
                mimeMessage.setSentDate(DateFormat.getInstance().parse(sentDate));
            }
        }

        // fill the "Reply-To" field of the mail
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO) != null) {
            Address[] replyTo = InternetAddress.parse(nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO)
                .toString());
            if (replyTo != null) {
                mimeMessage.setReplyTo(replyTo);
            }
        }
    }

    /**
     * copy the headers of the mail message into the normalized message headers
     * 
     * @param exchange the exchange to use
     * @param nmsg the message to use
     * @param mailMsg the mail message
     * @throws javax.mail.MessagingException on any errors
     */
    protected void copyHeaders(MessageExchange exchange, NormalizedMessage nmsg, MimeMessage mailMsg)
        throws javax.mail.MessagingException {
        // first convert the headers of the mail to properties of the message
        Enumeration headers = mailMsg.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header header = (Header)headers.nextElement();
            if (nmsg.getProperty(header.getName()) != null) {
                // this is a multi line header - add it at the end
                nmsg.setProperty(header.getName(), nmsg.getProperty(header.getName() + ";"
                                                                    + header.getValue()));
            } else {
                // add it to the message properties
                nmsg.setProperty(header.getName(), header.getValue());
            }
            log.debug("Setting property: " + header.getName() + " = " + header.getValue());
        }

        // now fill some predefined properties to the message
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_BCC) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_BCC));
        }
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_CC) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_CC));
        }
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_FROM) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_FROM));
        }
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_REPLYTO) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_REPLYTO));
        }
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_SENTDATE) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SENTDATE, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_SENTDATE));
        }
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_SUBJECT) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_SUBJECT));
        }
        if (nmsg.getProperty(AbstractMailMarshaler.MAIL_TAG_TO) != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, nmsg
                .getProperty(AbstractMailMarshaler.MAIL_TAG_TO));
        }
    }

    /**
     * copies the mail body and attachments to the normalized message
     * 
     * @param exchange the exchange to use
     * @param nmsg the message to use
     * @param mailMsg the mail to use
     * @throws javax.mail.MessagingException on any errors
     */
    protected void copyBodyAndAttachments(MessageExchange exchange, NormalizedMessage nmsg,
                                          MimeMessage mailMsg) throws javax.mail.MessagingException {
        // now convert the mail body and attachments and put it to the msg
        Multipart mp;
        Object content;
        Multipart subMP;
        String text = null;
        String html = null;

        try {
            content = mailMsg.getContent();

            if (content instanceof String) {
                // simple mail
                text = asString(content);
            } else if (content instanceof Multipart) {
                // mail with attachment
                mp = (Multipart)content;
                int nbMP = mp.getCount();
                log.debug("MultiPart count: " + nbMP);
                for (int i = 0; i < nbMP; i++) {
                    Part part = mp.getBodyPart(i);
                    String disposition = part.getDisposition();

                    log.debug("MultiPart " + i + ": " + part);
                    log.debug("Disposition: " + disposition);

                    if (disposition != null
                        && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition
                            .equalsIgnoreCase(Part.INLINE))) {
                        // only add named attachments
                        if (part.getFileName() != null) {
                            // Parts marked with a disposition of Part.ATTACHMENT
                            // from part.getDisposition() are clearly attachments
                            DataHandler att = part.getDataHandler();
                            // this is clearly a attachment
                            nmsg.addAttachment(att.getName(), att);
                        } else {
                            // inline part without name?
                            text = part.getContent() != null ? part.getContent().toString() : "null";
                        }
                    } else if (disposition == null) {
                        // Check if plain
                        MimeBodyPart mbp = (MimeBodyPart)part;
                        if (mbp.isMimeType("text/plain")) {
                            // Handle plain text
                            text = (String)mbp.getContent();
                        } else if (mbp.isMimeType("text/html")) {
                            // Handle html contents
                            html = (String)mbp.getContent();
                        } else {
                            // Special non-attachment cases (image/gif, ...)
                            if (mbp.getContent() instanceof MimeMultipart) {
                                subMP = (MimeMultipart)mbp.getContent();
                                int nbsubMP = subMP.getCount();
                                for (int j = 0; j < nbsubMP; j++) {
                                    MimeBodyPart subMBP = (MimeBodyPart)subMP.getBodyPart(j);

                                    if (subMBP.getContent() instanceof InputStream) {
                                        // parse the part
                                        parsePart(subMBP, nmsg);
                                    } else if (subMBP.isMimeType("text/plain")
                                               && (text == null || text.length() <= 0)) {
                                        // Handle plain text
                                        text = (String)subMBP.getContent();
                                    } else if (subMBP.isMimeType("text/html")
                                               && (html == null || html.length() <= 0)) {
                                        // Handle plain text
                                        html = (String)subMBP.getContent();
                                    } else {
                                        // add as property into the normalize message
                                        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_ALTERNATIVE_CONTENT
                                                         + j, subMBP.getContent());
                                    }
                                }
                            } else {
                                // parse the part
                                parsePart(mbp, nmsg);
                            }
                        }
                    }
                }
            } else { // strange mail structure...log a warning
                log.warn("The content of the mail message is not supported by this component. ("
                         + content.getClass().getName() + ")");
            }
        } catch (MessagingException e) {
            throw new javax.mail.MessagingException("Error while setting content on normalized message", e);
        } catch (IOException e) {
            throw new javax.mail.MessagingException("Error while fetching content", e);
        }

        String msgContent = null;

        if (text == null && html != null) {
            // html mail body
            msgContent = html;
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, html);
        } else if (text != null && html != null) {
            // both text and html
            msgContent = text;
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, html);
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, text);
        } else {
            // text mail body
            if (text == null) {
                // text and html content is null
                log.debug("No content found! \n" + nmsg.toString());
            }

            msgContent = text;
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, text);
        }

        try {
            // now set the converted content for the normalized message
            nmsg.setContent(new StringSource(msgContent != null ? msgContent : "No mail content found!"));
        } catch (javax.jbi.messaging.MessagingException e) {
            throw new javax.mail.MessagingException("Error while setting message content", e);
        }
    }

    /**
     * extracts and parses the attachments found in the mail bodies and puts 
     * them to the normalized message attachments
     * 
     * @param mbp           the mime body part to parse
     * @param nmsg          the normalized message to fill
     * @throws MessagingException
     * @throws javax.mail.MessagingException
     * @throws IOException
     */
    protected void parsePart(MimeBodyPart mbp, NormalizedMessage nmsg) throws MessagingException,
        javax.mail.MessagingException, IOException {
        Object subContent = mbp.getContent();

        log.debug("Parsing: " + subContent.getClass().getName());

        if (subContent instanceof InputStream) {
            String altName = mbp.getContentID() + "."
                             + mbp.getContentType().substring(mbp.getContentType().lastIndexOf('/') + 1);
            altName = altName.replaceAll("<", "").replaceAll(">", "").toLowerCase();

            log.debug("Adding special attachment: "
                      + (mbp.getFileName() != null ? mbp.getFileName() : altName));

            // read the stream into a byte array
            byte[] data = new byte[mbp.getSize()];
            InputStream stream = (InputStream)subContent;
            stream.read(data);

            // create a byte array data source for use in data handler
            ByteArrayDataSource bads = new ByteArrayDataSource(data, mbp.getContentType());

            // remember the name of the attachment
            bads.setName(mbp.getFileName() != null ? mbp.getFileName() : altName);

            // add the attachment to the message
            nmsg.addAttachment(bads.getName(), new DataHandler(bads));
        }
    }
}
