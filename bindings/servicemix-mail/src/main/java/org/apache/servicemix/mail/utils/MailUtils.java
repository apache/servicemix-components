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
package org.apache.servicemix.mail.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.mail.marshaler.AbstractMailMarshaler;
import org.apache.servicemix.mail.security.CustomSSLSocketFactory;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * utility class
 * 
 * @author lhein
 */
public final class MailUtils {
    private static final Log log = LogFactory.getLog(MailUtils.class);
    
    public static final String KEY_BODY_TEXT = "text";
    public static final String KEY_BODY_HTML = "html";
    
    public static final String SSL_FACTORY = "org.apache.servicemix.mail.security.CustomSSLSocketFactory"; // javax.net.ssl.SSLSocketFactory

    public static final int DEFAULT_PORT_SMTP = 25;
    public static final int DEFAULT_PORT_SMTPS = 465;
    public static final int DEFAULT_PORT_POP3 = 110;
    public static final int DEFAULT_PORT_POP3S = 995;
    public static final int DEFAULT_PORT_NNTP = 119;
    public static final int DEFAULT_PORT_IMAP = 143;
    public static final int DEFAULT_PORT_IMAPS = 993;

    public static final String PROTOCOL_SMTP = "smtp";
    public static final String PROTOCOL_SMTPS = "smtps";
    public static final String PROTOCOL_POP = "pop";
    public static final String PROTOCOL_POP3 = "pop3";
    public static final String PROTOCOL_POP3S = "pop3s";
    public static final String PROTOCOL_NNTP = "nntp";
    public static final String PROTOCOL_IMAP = "imap";
    public static final String PROTOCOL_IMAPS = "imaps";

    private static final String MAIL_PREFIX = "mail.";
    private static final String DEFAULT_INBOX = "INBOX";
    
    private static final String URI_PASSWORD_PART = "password=";
    private static final String URI_USER_PART = "user=";
    
    public static final String CONNECTION_TIMEOUT = "10000";

    /**
     * default constructor
     */
    private MailUtils() {
    }
    
    /**
     * returns the default port for a given protocol
     * 
     * @param protocol the protocol
     * @return the default port
     */
    public static int getDefaultPortForProtocol(final String protocol) {
        int port = DEFAULT_PORT_SMTP;
        
        if (protocol != null) {
            if (protocol.equalsIgnoreCase(PROTOCOL_IMAP)) {
                port = DEFAULT_PORT_IMAP;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_IMAPS)) {
                port = DEFAULT_PORT_IMAPS;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_NNTP)) {
                port = DEFAULT_PORT_NNTP;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_POP) || protocol.equalsIgnoreCase(PROTOCOL_POP3)) {
                port = DEFAULT_PORT_POP3;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_POP3S)) {
                port = DEFAULT_PORT_POP3S;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_SMTP)) {
                port = DEFAULT_PORT_SMTP;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_SMTPS)) {
                port = DEFAULT_PORT_SMTPS;
            } else {
                port = DEFAULT_PORT_SMTP;
            }
        }
        
        return port;
    }

    /**
     * parse the connection details via given uri
     * 
     * @param uriString the uri used for connection
     * @return a mail connection configuration object
     * @throws ParseException if the uri couldn't be parsed
     */
    public static MailConnectionConfiguration configure(String uriString) throws ParseException {
        MailConnectionConfiguration config = new MailConnectionConfiguration();

        if (uriString == null || uriString.length() <= 0) {
            throw new ParseException("The given connection uri (" + uriString + ") is invalid.");
        }

        URI uri = URI.create(uriString);

        String value = uri.getHost();
        if (value != null) {
            config.setHost(value);
        }

        String scheme = uri.getScheme();
        if (scheme != null) {
            config.setProtocol(scheme);
        } else {
            // set smtp as default
            config.setProtocol(PROTOCOL_SMTP);
        }

        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            config.setUsername(userInfo);
        }

        int port = uri.getPort();
        if (port >= 0) {
            config.setPort(port);
        } else {
            config.setPort(getDefaultPortForProtocol(config.getProtocol()));
        }

        if (uri.getPath() != null && uri.getPath().length() > 0) {
            if (uri.getPath().startsWith("/")) {
                config.setFolderName(uri.getPath().substring(1));
            } else {
                config.setFolderName(uri.getPath());
            }
        } else {
            config.setFolderName(DEFAULT_INBOX);
        }

        if (uri.getQuery() != null && uri.getQuery().indexOf(URI_PASSWORD_PART) != -1) {
            // extract the password from query
            int beginIndex = uri.getQuery().indexOf(URI_PASSWORD_PART) + URI_PASSWORD_PART.length();
            int endIndex = uri.getQuery().indexOf(';', beginIndex + 1) != -1 ? uri.getQuery()
                .indexOf(';', beginIndex + 1) : uri.getQuery().length();
            config.setPassword(uri.getQuery().substring(beginIndex, endIndex));
        } else {
            // maybe no password required
            config.setPassword("");
        }

        if (userInfo == null) { 
            // alternative way of specifying the user name
            if (uri.getQuery() != null && uri.getQuery().indexOf(URI_USER_PART) != -1) {
                // extract the password from query
                int beginIndex = uri.getQuery().indexOf(URI_USER_PART) + URI_USER_PART.length();
                int endIndex = uri.getQuery().indexOf(';', beginIndex + 1) != -1 ? uri.getQuery()
                    .indexOf(';', beginIndex + 1) : uri.getQuery().length();
                config.setUsername(uri.getQuery().substring(beginIndex, endIndex));
            } else {
                // maybe no password required
                config.setUsername("");
            }
        }

        return config;
    }

    /**
     * returns the connection properties for use in endpoints
     *
     * @param config the connection configuration
     * @param customTrustManagers the custom trust manager(s)
     * @return the connection properties
     */
    public static Properties getPropertiesForProtocol(final MailConnectionConfiguration config,
                                                      final String customTrustManagers) {
        // Get system properties clone
        Properties mailConnectionProperties = (Properties)System.getProperties().clone();

        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".connectiontimeout",
                                     CONNECTION_TIMEOUT);
        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".timeout", CONNECTION_TIMEOUT);
        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".host", config.getHost());
        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".port", "" + config.getPort());
        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".user", config.getUsername());
        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".rsetbeforequit", "true");
        mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".auth", "true");
        mailConnectionProperties.put(MAIL_PREFIX + "transport.protocol", config.getProtocol());
        mailConnectionProperties.put(MAIL_PREFIX + "store.protocol", config.getProtocol());
        mailConnectionProperties.put(MAIL_PREFIX + "host", config.getHost());
        mailConnectionProperties.put(MAIL_PREFIX + "user", config.getUsername());

        if (customTrustManagers != null && customTrustManagers.trim().length() > 0
            && config.isSecureProtocol()) {
            // set java mail properties
            mailConnectionProperties
                .put(MAIL_PREFIX + config.getProtocol() + ".socketFactory.class", SSL_FACTORY);
            mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".socketFactory.fallback", "false");
            mailConnectionProperties.put(MAIL_PREFIX + config.getProtocol() + ".socketFactory.port",
                                         "" + config.getPort());

            // set the properties for the custom ssl socket factory
            System.getProperties().put(CustomSSLSocketFactory.PROPERTY_TRUSTMANAGERS, customTrustManagers);
        }

        return mailConnectionProperties;
    }
    
    /**
     * Extracts the body from the Mail message
     *
     * @param message   the mail message
     * @return the mail body
     */
    public static Object extractBodyFromMail(Message message) {
        try {
            return message.getContent();
        } catch (UnsupportedEncodingException e) {
            return "Unable to decode the mail because charset is not supported: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract body due to: " + e.getMessage()
                + ". Message: " + message, e);
        }
    }
    
    /**
     * copy the headers of the mail message into the normalized message headers
     * 
     * @param nmsg the message to use
     * @param mailMsg the mail message
     * @throws javax.mail.MessagingException on any errors
     */
    @SuppressWarnings("unchecked")
    public static void extractHeadersFromMail(NormalizedMessage nmsg, MimeMessage mailMsg)
        throws javax.mail.MessagingException {
        // first convert the headers of the mail to properties of the message
        Enumeration headers = mailMsg.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header header = (Header)headers.nextElement();
            if (nmsg.getProperty(header.getName()) != null) {
                // this is a multi line header - add it at the end
                try {
                    nmsg.setProperty(header.getName(), nmsg.getProperty(header.getName() + ";"
                                                                    + MimeUtility.decodeText(header.getValue())));
                } catch (UnsupportedEncodingException e) {
                    nmsg.setProperty(header.getName(), nmsg.getProperty(header.getName() + ";"
                                                                        + header.getValue()));
                }
            } else {
                // add it to the message properties
                try {
                    nmsg.setProperty(header.getName(), MimeUtility.decodeText(header.getValue()));
                } catch (UnsupportedEncodingException e) {
                    nmsg.setProperty(header.getName(), header.getValue());
                }
            }
            try {
                log.debug("Setting property: " + header.getName() + " = " + MimeUtility.decodeText(header.getValue()));    
            } catch (UnsupportedEncodingException e) {
                log.debug("Setting property: " + header.getName() + " = " + header.getValue());
            }
        }

        // grab the mime type
        if (mailMsg.getContentType() != null) {
            if (mailMsg.isMimeType(MailContentType.TEXT_PLAIN.getMimeType())) {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.TEXT_PLAIN.getMimeType());
            } else if (mailMsg.isMimeType(MailContentType.TEXT_HTML.getMimeType())) {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.TEXT_HTML.getMimeType());
            } else if (mailMsg.isMimeType(MailContentType.TEXT_XML.getMimeType())) {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.TEXT_XML.getMimeType());
            } else if (mailMsg.isMimeType(MailContentType.MULTIPART_ALTERNATIVE.getMimeType())) {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.MULTIPART_ALTERNATIVE.getMimeType());
            } else if (mailMsg.isMimeType(MailContentType.MULTIPART_MIXED.getMimeType())) {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.MULTIPART_MIXED.getMimeType());
            } else if (mailMsg.isMimeType(MailContentType.MULTIPART.getMimeType())) {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.MULTIPART.getMimeType());
            } else {
                nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.UNKNOWN.getMimeType());
            }
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
     * extracts attachments from a multipart mail part
     * 
     * @param mp        the multipart
     * @param nmsg      the normalized message
     * @throws javax.mail.MessagingException    on mail errors
     * @throws MessagingException       on jbi messaging errors
     * @throws IOException      on io errors
     */
    public static void extractAttachmentsFromMultipart(Multipart mp, NormalizedMessage nmsg)
        throws javax.mail.MessagingException, MessagingException, IOException {

        for (int i = 0; i < mp.getCount(); i++) {
            MimeBodyPart part = (MimeBodyPart)mp.getBodyPart(i);
            if (part.isMimeType(MailContentType.MULTIPART.getMimeType())) {
                try {
                    Multipart mup = (Multipart)part.getContent();
                    extractAttachmentsFromMultipart(mup, nmsg);
                } catch (UnsupportedEncodingException e) {
                    log.error("Unable to decode the mail because charset is not supported.", e);
                }
            } else {
                if (i < 1) {
                    // skip first part always
                    continue;
                }
                String disposition = part.getDisposition();
                if (disposition != null && 
                    disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                    
                    // treat as attachment
                    String name = part.getFileName();
                    // only add named attachments
                    if (name != null) {
                        // Parts marked with a disposition of
                        // Part.ATTACHMENT
                        // are clearly attachments
                        try {
                            name = MimeUtility.decodeText(name);
                        } catch (UnsupportedEncodingException e) {
                            // ignore it
                        }
                        nmsg.addAttachment(name, part.getDataHandler());
                    } else if (part.getDataHandler() != null) {
                        // also add unnamed if there is a data handler
                        nmsg.addAttachment(disposition, part.getDataHandler());                            
                    }
                } else if (disposition != null && disposition.equalsIgnoreCase(Part.INLINE)) {
                    String contentID = part.getContentID();
                    if (contentID != null) {
                        contentID = contentID.replace('<', ' ');
                        contentID = contentID.replace('>', ' ');
                        contentID = contentID.trim();
                    } else {
                        contentID = part.getFileName();
                    }
                    contentID = "cid:" + contentID;
                    // treat as inline attachment
                    String name = part.getFileName();
                    // only add named attachments
                    if (name != null) {
                        // Parts marked with a disposition of
                        // Part.ATTACHMENT
                        // are clearly attachments
                        try {
                            name = MimeUtility.decodeText(name);
                        } catch (UnsupportedEncodingException e) {
                            // ignore it
                        }
                        nmsg.addAttachment(contentID, part.getDataHandler());
                        nmsg.setProperty(contentID, name);
                    } else if (part.getDataHandler() != null) {
                        // also add unnamed if there is a data handler
                        nmsg.addAttachment(contentID, part.getDataHandler());
                        nmsg.setProperty(contentID, contentID);
                    }                    
                }
            }
        }
    }
    
    /**
     * returns the body of the mail 
     * 
     * @param mp        the multipart 
     * @param nmsg      the normalized message
     * @return  a map of mail bodies
     * @throws javax.mail.MessagingException on mail message errors
     * @throws IOException on io errors
     */
    public static Map<String, String> extractBodyFromMultipart(Multipart mp, NormalizedMessage nmsg) 
        throws javax.mail.MessagingException, IOException {
        Map<String, String> content = new HashMap<String, String>();
        
        for (int i = 0; i < mp.getCount(); i++) {
            Part part = mp.getBodyPart(i);
            String disposition = part.getDisposition();

            if (disposition == null) {
                // Check if plain
                if (part.isMimeType(MailContentType.MULTIPART.getMimeType())) {
                    try {
                        Multipart mup = (Multipart)part.getContent();
                        Map<String, String> res = extractBodyFromMultipart(mup, nmsg);
                        for (String key : res.keySet()) {
                            if (content.containsKey(key)) {
                                content.put(key, content.get(key) + '\n' + res.get(key));
                            } else {
                                content.put(key, res.get(key));
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        log.error("Unable to decode the mail because charset is not supported.", e);
                    }
                } else if (part.isMimeType(MailContentType.TEXT_PLAIN.getMimeType())) {
                    try {
                        content.put(KEY_BODY_TEXT, (String)part.getContent());
                    } catch (UnsupportedEncodingException e) {
                        content.put(KEY_BODY_TEXT, AbstractMailMarshaler.DUMMY_CONTENT);
                        log.error("Unable to decode the mail because charset is not supported.", e);
                    }
                } else if (part.isMimeType(MailContentType.TEXT_HTML.getMimeType()) ||
                           part.isMimeType(MailContentType.TEXT_XML.getMimeType()) ) {
                    try {
                        content.put(KEY_BODY_HTML, (String)part.getContent());
                    } catch (UnsupportedEncodingException e) {
                        log.error("Unable to decode the mail because charset is not supported.", e);
                    }
                } else {
                    // can't parse that...skipping
                }
            }
        }
        
        // do a final check if this is a multipart/alternative and if yes, then
        // switch the content type according
        if (content.size() == 2 && !nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE).toString().equalsIgnoreCase(MailContentType.MULTIPART_ALTERNATIVE.getMimeType())) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_MAIL_CONTENT_TYPE, MailContentType.MULTIPART_ALTERNATIVE.getKey());
        }
        
        return content;
    }
    
    /**
     * extracts and parses the attachments found in the mail bodies and puts 
     * them to the normalized message attachments
     * 
     * @param mbp           the mime body part to parse
     * @param nmsg          the normalized message to fill
     * @throws MessagingException   on messaging errors
     * @throws javax.mail.MessagingException on mail message errors
     * @throws IOException on io errors
     */
    public static void parsePart(MimeBodyPart mbp, NormalizedMessage nmsg) throws MessagingException,
        javax.mail.MessagingException, IOException {
        Object subContent = mbp.getContent();

        log.debug("Parsing: " + subContent.getClass().getName());

        if (subContent instanceof InputStream) {
            String cid = mbp.getContentID();
            if (cid != null) {
                cid = cid.replaceAll("<", "").replaceAll(">", "").toLowerCase();
            }

            log.debug("Adding special attachment: "
                      + (mbp.getFileName() != null ? mbp.getFileName() : cid));

            // read the stream into a byte array
            byte[] data = new byte[mbp.getSize()];
            InputStream stream = (InputStream)subContent;
            stream.read(data);

            // create a byte array data source for use in data handler
            ByteArrayDataSource bads = new ByteArrayDataSource(data, mbp.getContentType());
            
            // remember the name of the attachment
            bads.setName(mbp.getFileName() != null ? mbp.getFileName() : cid);

            // add the attachment to the message
            nmsg.addAttachment(bads.getName(), new DataHandler(bads));
            // add the cid2attachment mapping to properties
            if (cid != null) {
                nmsg.setProperty(cid, mbp.getFileName());
            }
        }
    }
    
    /**
     * extracts the body from the mail
     * 
     * @param nmsg      the normalized message
     * @param mailMsg   the mail message
     * @throws javax.jbi.messaging.MessagingException on messaging errors
     * @throws javax.mail.MessagingException on mail message errors
     */
    public static void extractBodyFromMail(NormalizedMessage nmsg, MimeMessage mailMsg)
        throws javax.mail.MessagingException, MessagingException {
        
        Object content = MailUtils.extractBodyFromMail(mailMsg);
        String text = null;
        String html = null;
        
        if (content == null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, AbstractMailMarshaler.DUMMY_CONTENT);
            return;
        }
        
        if (mailMsg.isMimeType(MailContentType.TEXT_PLAIN.getMimeType())) {
            // simple mail
            text = (String)content;
        } else if (mailMsg.isMimeType(MailContentType.TEXT_HTML.getMimeType()) ||
                   mailMsg.isMimeType(MailContentType.TEXT_XML.getMimeType())) {
            html = (String)content;
        } else if (mailMsg.isMimeType(MailContentType.MULTIPART.getMimeType())) {
            // multipart - scan for body 
            Multipart mp = (Multipart)content;
            try {
                Map<String, String> parts = extractBodyFromMultipart(mp, nmsg);
                
                // check for text
                if (parts.containsKey(KEY_BODY_TEXT)) {
                    text = parts.get(KEY_BODY_TEXT);
                } 
                // check for html
                if (parts.containsKey(KEY_BODY_HTML)) {
                    html = parts.get(KEY_BODY_HTML);
                }
            } catch (Exception e) {
                log.error("Error extracting body from message " + mailMsg, e);
            }
        }
        
        if ((html == null || html.trim().length() < 1) &&
            (text == null || text.trim().length() < 1)) {
            text = AbstractMailMarshaler.DUMMY_CONTENT;
        }
        
        if (text != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, text);
        }
        
        if (html != null) {
            nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, html);
            nmsg.setContent(new StringSource(html));
        }
    }
    
    /**
     * extracts the attachments from the mail 
     * 
     * @param nmsg      the normalized message
     * @param mailMsg   the mail message
     */
    public static void extractAttachmentsFromMail(NormalizedMessage nmsg, MimeMessage mailMsg) {
        Object content = MailUtils.extractBodyFromMail(mailMsg);
        if (content != null && content instanceof Multipart) {
            // mail with attachment
            Multipart mp = (Multipart)content;
            try {
                MailUtils.extractAttachmentsFromMultipart(mp, nmsg);
                log.debug("Attachments found: " + nmsg.getAttachmentNames().size());
            } catch (Exception e) {
                log.error("Error extracting attachments from message " + mailMsg, e);
            }
        }
    }
}
