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

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.mail.utils.MailContentType;
import org.apache.servicemix.mail.utils.MailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.internet.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

/**
 * this is the default marshaler for conversion between the normalized message
 * format and the mail message format
 * 
 * @author lhein
 */
public class DefaultMailMarshaler extends AbstractMailMarshaler {

    private final Logger logger = LoggerFactory.getLogger(DefaultMailMarshaler.class);

    /*
     * (non-Javadoc)
     * 
     * @see net.compart.jbi.mail.AMailMarshallerSupport#convertMailToJBI(javax.jbi.messaging.MessageExchange,
     *      javax.jbi.messaging.NormalizedMessage, javax.mail.internet.MimeMessage)
     */
    @Override
    public void convertMailToJBI(MessageExchange exchange, NormalizedMessage nmsg, MimeMessage mailMsg)
        throws javax.mail.MessagingException {
        // extract the headers from the mail message
        MailUtils.extractHeadersFromMail(nmsg, mailMsg);

        // extract the body
        try {
            MailUtils.extractBodyFromMail(nmsg, mailMsg);
            if (nmsg.getContent() == null) {
                nmsg.setContent(new StringSource(AbstractMailMarshaler.DUMMY_CONTENT));
            }
        } catch (Exception e) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, AbstractMailMarshaler.DUMMY_CONTENT);
            logger.error("Unable to extract the mail content: " + MailUtils.extractBodyFromMail(mailMsg), e);
        }        
        
        // extract the attachments
        MailUtils.extractAttachmentsFromMail(nmsg, mailMsg);
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
            fillMailHeaders(mimeMessage, nmsg, configuredSender, configuredReceiver);

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
        
        String contentType = null;
        
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE) != null) {
            contentType = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE).toString();
        }
        
        boolean isText = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT) != null;
        boolean isHtml = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML) != null || !isText || nmsg.getContent() != null;
                
        if (contentType == null) {
            // we need to guess what is best to use here
            if (isHtml) {
                // we got attachments or a html content...so it will be some multipart/* message
                prepareMixedMail(mimeMessage, exchange, nmsg);
            } else {
                // we will use a text/plain message
                preparePlainTextMail(mimeMessage, nmsg);
            }
        } else {
            MailContentType mct = MailContentType.getEnumForValue(contentType);
            switch (mct) {
                case TEXT_PLAIN:                preparePlainTextMail(mimeMessage, nmsg);
                                                break;
                case TEXT_HTML:
                case TEXT_XML:
                case MULTIPART:         
                case MULTIPART_MIXED:           prepareMixedMail(mimeMessage, exchange, nmsg);
                                                break;
                case MULTIPART_ALTERNATIVE:     prepareAlternativeMail(mimeMessage, exchange, nmsg);
                                                break;
                default:                        if (isHtml && isText) {
                                                    prepareAlternativeMail(mimeMessage, exchange, nmsg);
                                                } else if (isText && nmsg.getAttachmentNames().size() == 0) {
                                                    preparePlainTextMail(mimeMessage, nmsg);
                                                } else {
                                                    prepareMixedMail(mimeMessage, exchange, nmsg);
                                                }
                }
            }
        }            

    /**
     * prepares a plain text mail message
     * 
     * @param mimeMessage       the mail message
     * @param nmsg              the normalized message
     * @throws Exception        on errors
     */
    protected void preparePlainTextMail(MimeMessage mimeMessage, NormalizedMessage nmsg) throws Exception {
        
        Object content = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT);
        Object charset = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CHARSET);
        
        if (content != null) {
            if (charset != null) {
                mimeMessage.setText(content.toString(), charset.toString(), "plain");
            } else {
                mimeMessage.setText(content.toString(), MimeUtility.getDefaultJavaCharset(), "plain");
            }
        } else {
            // no message content...throw exception
            throw new MessagingException("Unable to get mail content for text/plain message from exchange.");
        }
    }
    
    /**
     * prepares a multipart mixed mail message 
     * 
     * @param mimeMessage       the mail message
     * @param exchange          the message exchange
     * @param nmsg              the normalized message
     * @throws Exception        on errors
     */
    protected void prepareMixedMail(MimeMessage mimeMessage, MessageExchange exchange,
                                        NormalizedMessage nmsg) throws Exception {
        
        boolean isText = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT) != null;
        boolean isHtml = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML) != null;
        boolean useInline = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_USE_INLINE_ATTACHMENTS) != null &&
                (Boolean) nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_USE_INLINE_ATTACHMENTS);
        
        Object content_text = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT);
        Object content_html = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML);
        Object charset = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CHARSET);
        
        // Create a Multipart
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("mixed");

        // fill the body with text if existing
        if (isText && content_text != null) {
            MimeBodyPart textBodyPart = new MimeBodyPart();
            if (charset != null) {
                textBodyPart.setText(content_text.toString(), charset.toString(), "plain");    
            } else {
                textBodyPart.setText(content_text.toString(), MimeUtility.getDefaultJavaCharset(), "plain");
            }
            multipart.addBodyPart(textBodyPart);
        }
        
        // fill the body with html if existing
        if (isHtml && content_html != null) {
            MimeBodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(content_html.toString(), MailContentType.TEXT_HTML.getMimeType());
            multipart.addBodyPart(htmlBodyPart);
        }
        
        // put attachments in place
        appendAttachments(exchange, nmsg, multipart, useInline ? Part.INLINE : Part.ATTACHMENT);
        
        // Put parts in message
        mimeMessage.setContent(multipart);
    }
    
    /**
     * prepares a multipart alternative mail message (both html and text)
     * 
     * @param mimeMessage       the mail message
     * @param exchange          the message exchange
     * @param nmsg              the normalized message
     * @throws Exception        on errors
     */
    protected void prepareAlternativeMail(MimeMessage mimeMessage, MessageExchange exchange,
                                        NormalizedMessage nmsg) throws Exception {
    
        boolean isText = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT) != null;
        boolean isHtml = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML) != null;
        boolean useInline = true; // defaults to true
        
        // only use attachments if it's definitive set so
        if (nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_USE_INLINE_ATTACHMENTS) != null && 
            !(Boolean)nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_USE_INLINE_ATTACHMENTS)) {
            useInline = false;
        }
        
        Object content_text = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT);
        Object content_html = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML);
        Object charset = nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CHARSET);
        
        // Create a Multipart
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("alternative");

        // fill the body with text if existing
        if (isText && content_text != null) {
            MimeBodyPart textBodyPart = new MimeBodyPart();
            if (charset != null) {
                textBodyPart.setText(content_text.toString(), charset.toString(), "plain");    
            } else {
                textBodyPart.setText(content_text.toString(), MimeUtility.getDefaultJavaCharset(), "plain");
            }
            multipart.addBodyPart(textBodyPart);
        }

        if (nmsg.getAttachmentNames().size() < 1) {
            // no attachments
            // fill the body with html if existing
            if (isHtml && content_html != null) {
                MimeBodyPart htmlBodyPart = new MimeBodyPart();
                htmlBodyPart.setContent(content_html.toString(), MailContentType.TEXT_HTML.getMimeType());
                multipart.addBodyPart(htmlBodyPart);
            }
        } else {
            // found attachments
            if (!useInline) {
                // no inline attachments
                BodyPart htmlpart = new MimeBodyPart();
                MimeMultipart mmp = new MimeMultipart();
                mmp.setSubType("mixed");
                
                // fill the body with html if existing
                if (isHtml && content_html != null) {
                    MimeBodyPart htmlBodyPart = new MimeBodyPart();
                    htmlBodyPart.setContent(content_html.toString(), MailContentType.TEXT_HTML.getMimeType());
                    mmp.addBodyPart(htmlBodyPart);
                }
                htmlpart.setContent(mmp);
                multipart.addBodyPart(htmlpart);

                // put attachments in place
                appendAttachments(exchange, nmsg, mmp, Part.ATTACHMENT);
            } else {
                // use inline attachments
                MimeMultipart multipartRelated = new MimeMultipart("related");
                BodyPart related = new MimeBodyPart();
                related.setContent(multipartRelated);
                multipart.addBodyPart(related);

                // fill the body with html if existing
                if (isHtml && content_html != null) {
                    MimeBodyPart htmlBodyPart = new MimeBodyPart();
                    htmlBodyPart.setContent(content_html.toString(), MailContentType.TEXT_HTML.getMimeType());
                    multipartRelated.addBodyPart(htmlBodyPart);
                }
                // put attachments in place
                appendAttachments(exchange, nmsg, multipartRelated, Part.INLINE);
            }
        }
        
        // Put parts in message
        mimeMessage.setContent(multipart);        
    }

    /**
     * appends all attachments to the mime message
     * 
     * @param exchange          the message exchange
     * @param nmsg              the normalized message  
     * @param multipart         the mime multipart
     * @param partType          the part type (INLINE / ATTACHMENT)
     * @throws Exception        on any errors
     */
    protected void appendAttachments(MessageExchange exchange, NormalizedMessage nmsg, 
                                     MimeMultipart multipart, String partType) throws Exception {
        // if there are attachments, then a multipart mime mail with
        // attachments will be sent
        if (nmsg.getAttachmentNames().size() > 0) {
            Iterator itAttNames = nmsg.getAttachmentNames().iterator();

            if (itAttNames.hasNext()) {
                // there is at least one attachment
                MimeBodyPart messageBodyPart;

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
                    dh.writeTo(bos);
                    bos.close();
                    
                    logger.debug("Saved temp file: " + f.getName() + " with length: " + f.length());
                    
                    // add the file to the temporary resources list
                    addTemporaryResource(exchange.getExchangeId(), f);                    
                    // Create another body part
                    messageBodyPart = new MimeBodyPart();
                    // Set the data handler to the attachment
                    messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(f)));
                    // Set the filename
                    messageBodyPart.setFileName(dh.getDataSource().getName());
                    // Set Disposition
                    messageBodyPart.setDisposition(partType);
                    // add a Content-ID header to the attachment                    
                    if (oneAttachmentName.toLowerCase().startsWith("cid:")) {
                        messageBodyPart.setContentID(oneAttachmentName.substring(4));
                    }
                    // Add part to multipart
                    multipart.addBodyPart(messageBodyPart);
                }
            }
        }
    }
    
    /**
     * fills the mail headers according to the normalized message headers
     * 
     * @param mimeMessage the mail message to fill
     * @param nmsg the normalized message received
     * @param configuredSender the configured sender from xbean
     * @param configuredReceiver the configured receiver from xbean
     * @throws Exception on errors
     */
    protected void fillMailHeaders(MimeMessage mimeMessage, NormalizedMessage nmsg, String configuredSender, String configuredReceiver)
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
            	try {
            		mimeMessage.setSentDate(DateFormat.getInstance().parse(sentDate));
            	} catch (ParseException ex) {
            		// unparsable date
            		mimeMessage.setSentDate(new Date());
            	}
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
}
