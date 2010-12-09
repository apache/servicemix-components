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
package org.apache.servicemix.common.util;

import java.net.URL;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.security.auth.Subject;
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.mock.MockMessageExchange;

public class MessageUtilTest extends TestCase {
	
    public void testNormalizedMessageImpl() throws Exception {    	
        Subject subject = new Subject();        
        
        MessageUtil.NormalizedMessageImpl src = new MessageUtil.NormalizedMessageImpl();
        MessageUtil.NormalizedMessageImpl dest = new MessageUtil.NormalizedMessageImpl();
        src.setContent(new StringSource("<hello>world</hello>"));
        src.setProperty("name", "edell");
        src.setProperty("surname", "nolan");
        DataHandler param = new DataHandler(new ByteArrayDataSource("foo".getBytes(), 
            "application/octet-stream"));
        src.addAttachment("fooId", param);
        DataHandler param2 = new DataHandler(new ByteArrayDataSource("bar".getBytes(), 
        "application/octet-stream"));
        src.addAttachment("barId", param);
        Object credential = new String("test-credential");
        subject.getPublicCredentials().add(credential);
        src.setSecuritySubject(subject);
        
        MessageUtil.transfer(src,dest);
        
        String txt = new SourceTransformer().toString(dest.getContent());
        assertEquals("<hello>world</hello>", txt);        
        assertEquals("edell", dest.getProperty("name").toString());
        Set s = dest.getPropertyNames();       
        assertTrue(s.contains("name"));
        assertTrue(s.contains("surname"));        
        
        param = null;
        param = dest.getAttachment("fooId");        
        assertEquals("application/octet-stream", param.getDataSource().getContentType());
        
        Set attachementSet = dest.getAttachmentNames();
        assertTrue(attachementSet.contains("fooId"));
        assertTrue(attachementSet.contains("barId"));
        
        dest.removeAttachment("barId");
        assertFalse(attachementSet.contains("barId"));
        
        Subject sub = dest.getSecuritySubject();
        assertTrue(sub.getPublicCredentials().contains(credential));
        
        MessageUtil.NormalizedMessageImpl nmsg = new MessageUtil.NormalizedMessageImpl(src);        
        assertEquals("StringSource[<hello>world</hello>]", nmsg.getContent().toString());
        
        NormalizedMessage newMsg = MessageUtil.copy(src);
        assertEquals("StringSource[<hello>world</hello>]", newMsg.getContent().toString());
        
        src.setContent(null);
        MessageUtil.NormalizedMessageImpl nmsg2 = new MessageUtil.NormalizedMessageImpl(src);
        assertNull(nmsg2.getContent());
        
        URL url = new URL("http://schemas.xmlsoap.org/soap/http");
        DataHandler urlDataHandler = new DataHandler(url);
        src.addAttachment("urlId", urlDataHandler);
        MessageUtil.NormalizedMessageImpl nmsg3 = new MessageUtil.NormalizedMessageImpl(src);   
        DataHandler dh = nmsg3.getAttachment("urlId");
        assertTrue(dh.getDataSource().getContentType().startsWith("text/html"));                
    }
    
    public void testTransfers() throws Exception {      	
        MessageExchange meSrc = new MockMessageExchange();
        MessageExchange meDest = new MockMessageExchange();
        
        MessageUtil.NormalizedMessageImpl srcMsg = new MessageUtil.NormalizedMessageImpl();
        srcMsg.setContent(new StringSource("<hello>world</hello>"));
        srcMsg.setProperty("name", "edell");
        DataHandler param = new DataHandler(new ByteArrayDataSource("foo".getBytes(), 
        "application/octet-stream"));
        srcMsg.addAttachment("fooId", param);
        Object credential = new String("test-credential");
        Subject subject = new Subject();
        subject.getPublicCredentials().add(credential);
        srcMsg.setSecuritySubject(subject);
        
        // Tests transferTo
        meSrc.setMessage(srcMsg, "in");
        MessageUtil.transferTo(meSrc, meDest, "in");
        NormalizedMessage nm = meDest.getMessage("in");        
        assertEquals("StringSource[<hello>world</hello>]", nm.getContent().toString());
        
        // Tests transferInToIn
        meSrc.setMessage(srcMsg, "in");
        MessageUtil.transferInToIn(meSrc, meDest);
        nm = meDest.getMessage("in");        
        assertEquals("StringSource[<hello>world</hello>]", nm.getContent().toString());
   
        // Tests transferOutToIn
        meSrc.setMessage(srcMsg, "out");
        MessageUtil.transferOutToIn(meSrc, meDest);
        nm = meDest.getMessage("in");        
        assertEquals("StringSource[<hello>world</hello>]", nm.getContent().toString());
        
        // Tests transferOutToOut
        meSrc.setMessage(srcMsg, "out");
        MessageUtil.transferOutToOut(meSrc, meDest);
        nm = meDest.getMessage("out");        
        assertEquals("StringSource[<hello>world</hello>]", nm.getContent().toString());
        
        // Tests transferInToOut
        meSrc.setMessage(srcMsg, "in");
        MessageUtil.transferInToOut(meSrc, meDest);
        nm = meDest.getMessage("out");        
        assertEquals("StringSource[<hello>world</hello>]", nm.getContent().toString());
        
        // Tests tranfer just on exchanges
        MessageExchange meDest2 = new MockMessageExchange();
        MessageUtil.transferTo(meSrc, meDest2, "out");
        nm = meDest.getMessage("out");        
        assertEquals("StringSource[<hello>world</hello>]", nm.getContent().toString());
        
        // Tests copyIn
        NormalizedMessage copyIn = MessageUtil.copyIn(meSrc);
        assertEquals("StringSource[<hello>world</hello>]", copyIn.getContent().toString());
        
        // Tests copyOut
        NormalizedMessage copyOut = MessageUtil.copyOut(meSrc);
        assertEquals("StringSource[<hello>world</hello>]", copyOut.getContent().toString());
    }
    
    public void testEnableContentRereadability() throws Exception {
    	MessageUtil.NormalizedMessageImpl srcMsg = new MessageUtil.NormalizedMessageImpl();
        srcMsg.setContent(new StringSource("<hello>world</hello>"));
        srcMsg.setProperty("name", "edell");        
        MessageUtil.enableContentRereadability(srcMsg);
        assertEquals("StringSource[<hello>world</hello>]", srcMsg.getContent().toString());                
        
        try {
            srcMsg.setContent(new StreamSource("@@@@@@@@@@@@@"));            
            MessageUtil.enableContentRereadability(srcMsg);
            fail();
        } catch (javax.jbi.messaging.MessagingException ex) {
        	// MessagingException("Unable to convert message content into StringSource
        }                     
        
        srcMsg = new MessageUtil.NormalizedMessageImpl();
        srcMsg.setContent(new DOMSource());
        MessageUtil.enableContentRereadability(srcMsg);
        DOMSource domSrc = (DOMSource)srcMsg.getContent();
        assertNotNull(domSrc);                        
    }
    
    public void testFault() throws Exception {
    	MessageExchange exchangeSrc = new MockMessageExchange();
        MessageExchange exchangeDest = new MockMessageExchange();

    	MessageUtil.FaultImpl fault = new MessageUtil.FaultImpl();
    	fault.setContent(new StringSource("<fault>failure</fault>"));    	
    	exchangeSrc.setFault(fault);
    	    	    	    	
    	// Tests copyFault    	
        Fault copyFault = MessageUtil.copyFault(exchangeSrc);
        assertEquals("StringSource[<fault>failure</fault>]", copyFault.getContent().toString());
        
        NormalizedMessage nm = MessageUtil.copy(fault);
        assertEquals("StringSource[<fault>failure</fault>]", nm.getContent().toString());
        
    	// Tests transferFaultToFault
        MessageUtil.transferFaultToFault(exchangeSrc, exchangeDest);
        assertEquals("StringSource[<fault>failure</fault>]", exchangeDest.getFault().getContent().toString());
        
        MessageUtil.transferTo(exchangeSrc, exchangeDest, "fault");
        assertEquals("StringSource[<fault>failure</fault>]", exchangeDest.getFault().getContent().toString());

    }
    

}
