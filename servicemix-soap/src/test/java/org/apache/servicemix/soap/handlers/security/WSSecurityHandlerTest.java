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
package org.apache.servicemix.soap.handlers.security;

import java.io.File;
import java.net.URL;
import java.security.Principal;
import java.util.List;

import org.w3c.dom.Document;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.jaxp.W3CDOMStreamWriter;
import org.apache.servicemix.jbi.security.auth.impl.JAASAuthenticationService;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.marshalers.SoapMarshaler;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapReader;
import org.apache.servicemix.soap.marshalers.SoapWriter;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.springframework.core.io.ClassPathResource;
import sun.security.x509.X500Name;

public class WSSecurityHandlerTest extends TestCase {

    private static transient Log log = LogFactory.getLog(WSSecurityHandlerTest.class);

    static {
        String path = System.getProperty("java.security.auth.login.config");
        if (path == null) {
            URL resource = WSSecurityHandlerTest.class.getClassLoader().getResource("login.properties");
            if (resource != null) {
                path = new File(resource.getFile()).getAbsolutePath();
                System.setProperty("java.security.auth.login.config", path);
            }
        }
        log.info("Path to login config: " + path);
    }

    public void testUserNameToken() throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, true);
        SoapReader reader = marshaler.createReader();
        SoapMessage msg = reader.read(getClass().getResourceAsStream("sample-wsse-request.xml"));
        Context ctx = new Context();
        ctx.setInMessage(msg);
        
        WSSecurityHandler handler = new WSSecurityHandler();
        handler.setAuthenticationService(new JAASAuthenticationService());
        handler.setReceiveAction(WSHandlerConstants.USERNAME_TOKEN);
        handler.onReceive(ctx);
        List l = (List) ctx.getProperty(WSHandlerConstants.RECV_RESULTS);
        assertNotNull(l);
        assertEquals(1, l.size());
        WSHandlerResult result = (WSHandlerResult) l.get(0);
        assertNotNull(result);
        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
        WSSecurityEngineResult engResult = (WSSecurityEngineResult) result.getResults().get(0);
        assertNotNull(engResult);
        Principal principal = engResult.getPrincipal();
        assertNotNull(principal);
        assertTrue(principal instanceof WSUsernameTokenPrincipal);
        assertEquals("first", ((WSUsernameTokenPrincipal) principal).getName());
        assertEquals("secret", ((WSUsernameTokenPrincipal) principal).getPassword());
        assertNotNull(ctx.getInMessage().getSubject());
        assertNotNull(ctx.getInMessage().getSubject().getPrincipals());
        assertTrue(ctx.getInMessage().getSubject().getPrincipals().size() > 0);
    }
    
    public void testSignatureRoundtrip() throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, true);
        SoapMessage msg = new SoapMessage();
        Context ctx = new Context();
        ctx.setInMessage(msg);
        msg.setSource(new StringSource("<hello>world</hello>"));
        SoapWriter writer = marshaler.createWriter(ctx.getInMessage());
        W3CDOMStreamWriter domWriter = new W3CDOMStreamWriter(); 
        writer.writeSoapEnvelope(domWriter);
        ctx.getInMessage().setDocument(domWriter.getDocument());
        
        StandaloneCrypto crypto = new StandaloneCrypto();
        crypto.setKeyStoreUrl(new ClassPathResource("privatestore.jks"));
        crypto.setKeyStorePassword("keyStorePassword");
        WSSecurityHandler handler = new WSSecurityHandler();
        handler.setAuthenticationService(new JAASAuthenticationService());
        handler.setCrypto(crypto);
        handler.setUsername("myalias");
        crypto.setKeyPassword("myAliasPassword");
        handler.setSendAction(WSHandlerConstants.SIGNATURE);
        handler.onSend(ctx);
        
        Document doc = ctx.getInMessage().getDocument();
        log.info(DOMUtil.asXML(doc));
        
        handler.setReceiveAction(WSHandlerConstants.SIGNATURE);
        handler.onReceive(ctx);
        List l = (List) ctx.getProperty(WSHandlerConstants.RECV_RESULTS);
        assertNotNull(l);
        assertEquals(1, l.size());
        WSHandlerResult result = (WSHandlerResult) l.get(0);
        assertNotNull(result);
        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
        WSSecurityEngineResult engResult = (WSSecurityEngineResult) result.getResults().get(0);
        assertNotNull(engResult);
        Principal principal = engResult.getPrincipal();
        assertNotNull(principal);
        assertTrue(principal instanceof X500Name);
        assertEquals("CN=myAlias", ((X500Name) principal).getName());
        assertNotNull(ctx.getInMessage().getSubject());
        assertNotNull(ctx.getInMessage().getSubject().getPrincipals());
        assertTrue(ctx.getInMessage().getSubject().getPrincipals().size() > 0);
    }
    
    public void testSignatureServer() throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, true);
        SoapReader reader = marshaler.createReader();
        SoapMessage msg = reader.read(getClass().getResourceAsStream("signed.xml"));
        Context ctx = new Context();
        ctx.setInMessage(msg);
        
        StandaloneCrypto crypto = new StandaloneCrypto();
        crypto.setKeyStoreUrl(new ClassPathResource("privatestore.jks"));
        crypto.setKeyStorePassword("keyStorePassword");
        WSSecurityHandler handler = new WSSecurityHandler();
        handler.setAuthenticationService(new JAASAuthenticationService());
        handler.setCrypto(crypto);
        handler.setUsername("myalias");
        crypto.setKeyPassword("myAliasPassword");
        handler.setReceiveAction(WSHandlerConstants.SIGNATURE);
        handler.onReceive(ctx);
        List l = (List) ctx.getProperty(WSHandlerConstants.RECV_RESULTS);
        assertNotNull(l);
        assertEquals(1, l.size());
        WSHandlerResult result = (WSHandlerResult) l.get(0);
        assertNotNull(result);
        assertNotNull(result.getResults());
        assertEquals(1, result.getResults().size());
        WSSecurityEngineResult engResult = (WSSecurityEngineResult) result.getResults().get(0);
        assertNotNull(engResult);
        Principal principal = engResult.getPrincipal();
        assertNotNull(principal);
        assertTrue(principal instanceof X500Name);
        assertEquals("CN=myAlias", ((X500Name) principal).getName());
        assertNotNull(ctx.getInMessage().getSubject());
        assertNotNull(ctx.getInMessage().getSubject().getPrincipals());
        assertTrue(ctx.getInMessage().getSubject().getPrincipals().size() > 0);
    }
    
    public void testBadSignatureServer() throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, true);
        SoapReader reader = marshaler.createReader();
        SoapMessage msg = reader.read(getClass().getResourceAsStream("signed-bad.xml"));
        Context ctx = new Context();
        ctx.setInMessage(msg);
        
        StandaloneCrypto crypto = new StandaloneCrypto();
        crypto.setKeyStoreUrl(new ClassPathResource("privatestore.jks"));
        crypto.setKeyStorePassword("keyStorePassword");
        WSSecurityHandler handler = new WSSecurityHandler();
        handler.setCrypto(crypto);
        handler.setUsername("myalias");
        crypto.setKeyPassword("myAliasPassword");
        handler.setReceiveAction(WSHandlerConstants.SIGNATURE);
        try {
            handler.onReceive(ctx);
            fail("Signature verification should have failed");
        } catch (SoapFault f) {
            // ok
        }
    }
    
}
