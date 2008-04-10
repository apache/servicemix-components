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
package org.apache.servicemix.mail;

import java.io.File;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.InOnlyImpl;
import org.apache.servicemix.mail.marshaler.AbstractMailMarshaler;
import org.apache.servicemix.mail.marshaler.DefaultMailMarshaler;

/**
 * this is a collection of test which validate the marshaler conversion results
 * to make sure the conversion always delivers the expected results
 * 
 * @author lhein
 */
public class DefaultMailMarshalerTest extends TestCase {
    private static final String SUBJECT = "subject";
    private static final String FROM = "user@localhost.lan";
    private static final String TO = "anotheruser@localhost.lan";
    private static final String CC = "ccuser@localhost.lan";
    private static final String BCC = "bcc@localhost.lan";
    private static final String FILE = "smx-logo.png";
    private static final String TEXT = "This is a plain text test!";
    private static final String HTML = "<?xml version=\"1.0\"?><test>This is a html text test!</test>";
    private static final String PATH = "./src/test/resources/";

    private DefaultMailMarshaler marshaler;
    private SourceTransformer st;
    private Session session;

    /**
     * @throws java.lang.Exception
     */
    public void setUp() throws Exception {
        this.marshaler = new DefaultMailMarshaler();
        this.st = new SourceTransformer();
        this.session = Session.getDefaultInstance(System.getProperties(), null);
    }

    /**
     * @throws java.lang.Exception
     */
    public void tearDown() throws Exception {
        this.marshaler = null;
        this.st = null;
        this.session = null;
    }

    /**
     * Test method for
     * {@link net.compart.jbi.mail.marshaler.AbstractMailMarshaler#getDefaultSenderForOutgoingMails()}.
     */
    public void testGetDefaultSenderForOutgoingMails() {
        assertEquals("Marshaler's default sender is not correct!", marshaler
            .getDefaultSenderForOutgoingMails(), AbstractMailMarshaler.DEFAULT_SENDER);
    }

    /**
     * validates the commons fields expected in normalized message
     * 
     * @param nmsg the normalized message
     */
    private void validateCommonsInJBI(NormalizedMessage nmsg) {
        assertEquals("The SUBJECT is wrong!", SUBJECT, nmsg
            .getProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT));
        assertEquals("The FROM is wrong!", FROM, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_FROM));
        assertEquals("The TO is wrong!", TO, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TO));
        assertEquals("The CC is wrong!", CC, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_CC));
    }

    /**
     * test the conversion of plain text mails to normalized messages
     */
    public void testPlainTextToJBI() throws Exception {
        // prepare the mail
        MimeMessage mail = prepareTextMail(false);

        // prepare the jbi objects
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // do conversion
        marshaler.convertMailToJBI(exchange, nmsg, mail);

        // test the result
        validateCommonsInJBI(nmsg);
        assertEquals("The TEXT content is wrong!", TEXT, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT));
        assertNotNull("The message content was null!", nmsg.getContent());

        String result = st.contentToString(nmsg);
        assertEquals("The content is wrong!", TEXT, result);
    }

    /**
     * test the conversion of html mails to normalized messages
     */
    public void testHTMLToJBI() throws Exception {
        // prepare the mail
        MimeMessage mail = prepareHTMLMail(false);

        // prepare the jbi objects
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // do conversion
        marshaler.convertMailToJBI(exchange, nmsg, mail);

        // test the result
        validateCommonsInJBI(nmsg);
        assertEquals("The HTML content is wrong!", HTML, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML));
        assertNotNull("The message content was null!", nmsg.getContent());

        String result = st.contentToString(nmsg);
        assertEquals("The content is wrong!", HTML, result);
    }

    /**
     * test the conversion of plain text + html mails to normalized messages
     */
    public void testBothToJBI() throws Exception {
        // prepare the mail
        MimeMessage mail = prepareBothMail(false);

        // prepare the jbi objects
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // do conversion
        marshaler.convertMailToJBI(exchange, nmsg, mail);

        // test the result
        validateCommonsInJBI(nmsg);
        assertEquals("The HTML content is wrong!", HTML, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML));
        assertEquals("The TEXT content is wrong!", TEXT, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT));
        assertNotNull("The message content was null!", nmsg.getContent());

        String result = st.contentToString(nmsg);
        assertEquals("The content is wrong!", TEXT, result);
    }

    /**
     * test the conversion of plain text mails to normalized messages with
     * attachments
     */
    public void testPlainTextWithAttachmentsToJBI() throws Exception {
        // prepare the mail
        MimeMessage mail = prepareTextMail(true);

        // prepare the jbi objects
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // do conversion
        marshaler.convertMailToJBI(exchange, nmsg, mail);

        // test the result
        validateCommonsInJBI(nmsg);
        assertEquals("The TEXT content is wrong!", TEXT, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT));
        assertNotNull("The message content was null!", nmsg.getContent());

        String result = st.contentToString(nmsg);
        assertEquals("The content is wrong!", TEXT, result);

        if (nmsg.getAttachmentNames().size() != 1) {
            fail("The attachments are invalid. Expected: 1  Found: " + nmsg.getAttachmentNames().size());
        }

        Iterator it = nmsg.getAttachmentNames().iterator();
        if (it.hasNext()) {
            String key = (String)it.next();
            DataHandler dh = nmsg.getAttachment(key);
            assertNotNull("The attachment is missing!", dh);
            assertEquals("The name of the attachment is invalid!", FILE, key);
            assertEquals("The name of the attachments handler is invalid!", FILE, dh.getName());
        }
    }

    /**
     * test the conversion of html mails to normalized messages with attachments
     */
    public void testHtmlWithAttachmentsToJBI() throws Exception {
        // prepare the mail
        MimeMessage mail = prepareHTMLMail(true);

        // prepare the jbi objects
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // do conversion
        marshaler.convertMailToJBI(exchange, nmsg, mail);

        // test the result
        validateCommonsInJBI(nmsg);
        assertEquals("The HTML content is wrong!", HTML, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML));
        assertNotNull("The message content was null!", nmsg.getContent());

        String result = st.contentToString(nmsg);
        assertEquals("The content is wrong!", HTML, result);

        if (nmsg.getAttachmentNames().size() != 1) {
            fail("The attachments are invalid. Expected: 1  Found: " + nmsg.getAttachmentNames().size());
        }

        Iterator it = nmsg.getAttachmentNames().iterator();
        if (it.hasNext()) {
            String key = (String)it.next();
            DataHandler dh = nmsg.getAttachment(key);
            assertNotNull("The attachment is missing!", dh);
            assertEquals("The name of the attachment is invalid!", FILE, key);
            assertEquals("The name of the attachments handler is invalid!", FILE, dh.getName());
        }
    }

    /**
     * test the conversion of plain text + html mails to normalized messages
     * with attachments
     */
    public void testBothWithAttachmentsToJBI() throws Exception {
        // prepare the mail
        MimeMessage mail = prepareBothMail(true);

        // prepare the jbi objects
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // do conversion
        marshaler.convertMailToJBI(exchange, nmsg, mail);

        // test the result
        validateCommonsInJBI(nmsg);
        assertEquals("The HTML content is wrong!", HTML, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_HTML));
        assertEquals("The TEXT content is wrong!", TEXT, nmsg.getProperty(AbstractMailMarshaler.MSG_TAG_TEXT));
        assertNotNull("The message content was null!", nmsg.getContent());

        String result = st.contentToString(nmsg);
        assertEquals("The content is wrong!", TEXT, result);

        if (nmsg.getAttachmentNames().size() != 1) {
            fail("The attachments are invalid. Expected: 1  Found: " + nmsg.getAttachmentNames().size());
        }

        Iterator it = nmsg.getAttachmentNames().iterator();
        if (it.hasNext()) {
            String key = (String)it.next();
            DataHandler dh = nmsg.getAttachment(key);
            assertNotNull("The attachment is missing!", dh);
            assertEquals("The name of the attachment is invalid!", FILE, key);
            assertEquals("The name of the attachments handler is invalid!", FILE, dh.getName());
        }
    }

    /**
     * test the conversion of normalized messages to plain text mails
     */
    public void testJbiToPlainText() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, TEXT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(TEXT));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The TEXT is invalid!", TEXT, mail.getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());
    }

    /**
     * test the conversion of normalized messages to html mails
     */
    public void testJbiToHTML() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, HTML);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(HTML));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The HTML is invalid!", HTML, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());
    }

    /**
     * test the conversion of normalized messages to html + plain text mails
     */
    public void testJbiToBoth() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, TEXT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, HTML);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(TEXT));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The TEXT is invalid!", TEXT, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The HTML is invalid!", HTML, ((MimeMultipart)mail.getContent()).getBodyPart(1)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());
    }

    /**
     * test the conversion of normalized messages without needed headers to
     * mails
     */
    public void testRawTextToMail() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(TEXT));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The TEXT is invalid!", TEXT, mail.getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());
    }

    /**
     * test the conversion of normalized messages without needed headers to
     * mails
     */
    public void testRawHTMLToMail() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(HTML));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The HTML is invalid!", HTML, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());
    }

    /**
     * test the conversion of normalized messages to plain text mails
     */
    public void testJbiWithAttachmentsToPlainText() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, TEXT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(TEXT));
        // prepare attachment
        nmsg.addAttachment(FILE, new DataHandler(new FileDataSource(new File(PATH, FILE))));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The TEXT is invalid!", TEXT, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());

        // check attachment
        assertNotNull("No attachment part found!", ((MimeMultipart)mail.getContent()).getBodyPart(1));
        BodyPart att = ((MimeMultipart)mail.getContent()).getBodyPart(1);
        assertEquals("Attachment file name is invalid!", FILE, att.getFileName());
        if (att.getDataHandler().getInputStream().available() != new File(PATH, FILE).length()) {
            fail("Attachment size wrong. Expected: " + new File(PATH, FILE).length() + "  Found: "
                 + att.getDataHandler().getInputStream().available());
        }
    }

    /**
     * test the conversion of normalized messages to html mails
     */
    public void testJbiWithAttachmentsToHTML() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, HTML);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(HTML));
        // prepare attachment
        nmsg.addAttachment(FILE, new DataHandler(new FileDataSource(new File(PATH, FILE))));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The HTML is invalid!", HTML, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());

        // check attachment
        assertNotNull("No attachment part found!", ((MimeMultipart)mail.getContent()).getBodyPart(1));
        BodyPart att = ((MimeMultipart)mail.getContent()).getBodyPart(1);
        assertEquals("Attachment file name is invalid!", FILE, att.getFileName());
        if (att.getDataHandler().getInputStream().available() != new File(PATH, FILE).length()) {
            fail("Attachment size wrong. Expected: " + new File(PATH, FILE).length() + "  Found: "
                 + att.getDataHandler().getInputStream().available());
        }
    }

    /**
     * test the conversion of normalized messages to html + plain text mails
     */
    public void testJbiWithAttachmentsToBoth() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TEXT, TEXT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_HTML, HTML);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(TEXT));
        // prepare attachment
        nmsg.addAttachment(FILE, new DataHandler(new FileDataSource(new File(PATH, FILE))));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The TEXT is invalid!", TEXT, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The HTML is invalid!", HTML, ((MimeMultipart)mail.getContent()).getBodyPart(1)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());

        // check attachment
        assertNotNull("No attachment part found!", ((MimeMultipart)mail.getContent()).getBodyPart(2));
        BodyPart att = ((MimeMultipart)mail.getContent()).getBodyPart(2);
        assertEquals("Attachment file name is invalid!", FILE, att.getFileName());
        if (att.getDataHandler().getInputStream().available() != new File(PATH, FILE).length()) {
            fail("Attachment size wrong. Expected: " + new File(PATH, FILE).length() + "  Found: "
                 + att.getDataHandler().getInputStream().available());
        }
    }

    /**
     * test the conversion of normalized messages without needed headers to
     * mails
     */
    public void testRawTextWithAttachmentsToMail() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(TEXT));
        // prepare attachment
        nmsg.addAttachment(FILE, new DataHandler(new FileDataSource(new File(PATH, FILE))));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The TEXT is invalid!", TEXT, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());

        // check attachment
        assertNotNull("No attachment part found!", ((MimeMultipart)mail.getContent()).getBodyPart(1));
        BodyPart att = ((MimeMultipart)mail.getContent()).getBodyPart(1);
        assertEquals("Attachment file name is invalid!", FILE, att.getFileName());
        if (att.getDataHandler().getInputStream().available() != new File(PATH, FILE).length()) {
            fail("Attachment size wrong. Expected: " + new File(PATH, FILE).length() + "  Found: "
                 + att.getDataHandler().getInputStream().available());
        }
    }

    /**
     * test the conversion of normalized messages without needed headers to
     * mails
     */
    public void testRawHTMLWithAttachmentsToMail() throws Exception {
        MimeMessage mail = new MimeMessage(session);
        InOnly exchange = new InOnlyImpl();
        NormalizedMessage nmsg = exchange.createMessage();

        // prepare headers
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_FROM, FROM);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_TO, TO);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_CC, CC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_BCC, BCC);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_SUBJECT, SUBJECT);
        nmsg.setProperty(AbstractMailMarshaler.MSG_TAG_REPLYTO, FROM);
        // prepare content
        nmsg.setContent(new StringSource(HTML));
        // prepare attachment
        nmsg.addAttachment(FILE, new DataHandler(new FileDataSource(new File(PATH, FILE))));

        // convert
        marshaler.convertJBIToMail(mail, exchange, nmsg, null, null);

        // test the result
        assertEquals("The FROM is invalid!", FROM, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_FROM)[0]
            .toString());
        assertEquals("The TO is invalid!", TO, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_TO)[0]
            .toString());
        assertEquals("The CC is invalid!", CC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_CC)[0]
            .toString());
        assertEquals("The BCC is invalid!", BCC, mail.getHeader(AbstractMailMarshaler.MAIL_TAG_BCC)[0]
            .toString());
        assertEquals("The SUBJECT is invalid!", SUBJECT, mail
            .getHeader(AbstractMailMarshaler.MAIL_TAG_SUBJECT)[0].toString());
        assertEquals("The HTML is invalid!", HTML, ((MimeMultipart)mail.getContent()).getBodyPart(0)
            .getContent());
        assertEquals("The REPLY-TO is invalid!", FROM,
                     mail.getHeader(AbstractMailMarshaler.MAIL_TAG_REPLYTO)[0].toString());

        // check attachment
        assertNotNull("No attachment part found!", ((MimeMultipart)mail.getContent()).getBodyPart(1));
        BodyPart att = ((MimeMultipart)mail.getContent()).getBodyPart(1);
        assertEquals("Attachment file name is invalid!", FILE, att.getFileName());
        if (att.getDataHandler().getInputStream().available() != new File(PATH, FILE).length()) {
            fail("Attachment size wrong. Expected: " + new File(PATH, FILE).length() + "  Found: "
                 + att.getDataHandler().getInputStream().available());
        }
    }

    /**
     * creates a mail with or without attachments in plain text
     * 
     * @param withAttachments flag if attachments should be used
     * @return
     */
    private MimeMessage prepareTextMail(boolean withAttachments) throws MessagingException {
        // Create the message
        MimeMessage message = new MimeMessage(session);

        // Fill its headers
        message.setSubject(SUBJECT);
        message.setFrom(new InternetAddress(FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        message.addRecipient(Message.RecipientType.CC, new InternetAddress(CC));
        message.addRecipient(Message.RecipientType.BCC, new InternetAddress(BCC));

        if (withAttachments) {
            // Create your new message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(TEXT, "text/plain");

            // Create a related multi-part to combine the parts
            MimeMultipart multipart = new MimeMultipart("related");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart);

            // Create part for the image
            messageBodyPart = new MimeBodyPart();

            // Fetch the image and associate to part
            FileDataSource fds = new FileDataSource(new File(PATH, FILE));
            messageBodyPart.setDataHandler(new DataHandler(fds));
            messageBodyPart.setFileName(fds.getName());

            // Add part to multi-part
            multipart.addBodyPart(messageBodyPart);

            // Associate multi-part with message
            message.setContent(multipart);
        } else {
            // no attachments, so set text directly
            message.setText(TEXT);
        }

        message.saveChanges();

        return message;
    }

    /**
     * creates a mail with or without attachments in html
     * 
     * @param withAttachments flag if attachments should be used
     * @return
     */
    private MimeMessage prepareHTMLMail(boolean withAttachments) throws MessagingException {
        // Create the message
        MimeMessage message = new MimeMessage(session);

        // Fill its headers
        message.setSubject(SUBJECT);
        message.setFrom(new InternetAddress(FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        message.addRecipient(Message.RecipientType.CC, new InternetAddress(CC));
        message.addRecipient(Message.RecipientType.BCC, new InternetAddress(BCC));

        if (withAttachments) {
            // Create a Multipart
            MimeMultipart multipart = new MimeMultipart();

            // Create your new message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(HTML, "text/html");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart);

            // Create part for the image
            messageBodyPart = new MimeBodyPart();

            // Fetch the image and associate to part
            FileDataSource fds = new FileDataSource(new File(PATH, FILE));
            messageBodyPart.setDataHandler(new DataHandler(fds));
            messageBodyPart.setFileName(fds.getName());

            // Add part to multi-part
            multipart.addBodyPart(messageBodyPart);

            // Associate multi-part with message
            message.setContent(multipart);
        } else {
            // Create a Multipart
            MimeMultipart multipart = new MimeMultipart("alternate");

            // Create your new message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(HTML, "text/html");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart, 0);

            message.setDisposition(null);
            message.setContent(multipart);
        }

        message.saveChanges();

        return message;
    }

    /**
     * creates a mail with or without attachments in html
     * 
     * @param withAttachments flag if attachments should be used
     * @return
     */
    private MimeMessage prepareBothMail(boolean withAttachments) throws MessagingException {
        // Create the message
        MimeMessage message = new MimeMessage(session);

        // Fill its headers
        message.setSubject(SUBJECT);
        message.setFrom(new InternetAddress(FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        message.addRecipient(Message.RecipientType.CC, new InternetAddress(CC));
        message.addRecipient(Message.RecipientType.BCC, new InternetAddress(BCC));

        if (withAttachments) {
            // Create a Multipart
            MimeMultipart multipart = new MimeMultipart();

            // Create your new message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(TEXT, "text/plain");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart);

            messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(HTML, "text/html");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart);

            // Create part for the image
            messageBodyPart = new MimeBodyPart();

            // Fetch the image and associate to part
            FileDataSource fds = new FileDataSource(new File(PATH, FILE));
            messageBodyPart.setDataHandler(new DataHandler(fds));
            messageBodyPart.setFileName(fds.getName());

            // Add part to multi-part
            multipart.addBodyPart(messageBodyPart);

            // Associate multi-part with message
            message.setContent(multipart);
        } else {
            // Create a Multipart
            MimeMultipart multipart = new MimeMultipart("alternate");

            // Create your new message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(TEXT, "text/plain");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart);

            messageBodyPart = new MimeBodyPart();

            // Set the content of the body part
            messageBodyPart.setContent(HTML, "text/html");

            // Add body part to multipart
            multipart.addBodyPart(messageBodyPart);

            message.setDisposition(null);
            message.setContent(multipart);
        }

        message.saveChanges();

        return message;
    }
}
