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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.servicemix.common.security.KeystoreManager;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.Handler;
import org.apache.servicemix.soap.SoapFault;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandler;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.processor.Processor;
import org.apache.ws.security.processor.SignatureProcessor;
import org.apache.ws.security.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * WS-Security handler.
 * This handler is heavily based on xfire-ws-security project.
 * 
 * @org.apache.xbean.XBean element="ws-security"
 */
public class WSSecurityHandler extends WSHandler implements Handler {

    private Map properties = new HashMap();
    private String domain = "servicemix-domain";
    private AuthenticationService authenticationService;
    private boolean required;
    private String sendAction;
    private String receiveAction;
    private String actor;
    private String username;
    private String keystore;
    private Crypto crypto;
    private CallbackHandler handler = new DefaultHandler();
    
    private ThreadLocal currentSubject = new ThreadLocal();
    private static ThreadLocal currentHandler = new ThreadLocal();

    public WSSecurityHandler() {
        final WSSecurityEngine wssEng = new WSSecurityEngine();
        wssEng.setWssConfig(new ServiceMixWssConfig());
    }
    
    static WSSecurityHandler getCurrentHandler() {
        return (WSSecurityHandler) currentHandler.get();
    }
    
    /**
     * @return the authenticationService
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    /**
     * @param authenticationService the authenticationService to set
     */
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    private static class ServiceMixWssConfig extends WSSConfig {
        public Processor getProcessor(QName el) throws WSSecurityException {
            if (el.equals(WSSecurityEngine.SIGNATURE)) {
                return new SignatureProcessor();
            } else {
                return super.getProcessor(el);
            }
        }
    }
    
        
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the crypto
     */
    public Crypto getCrypto() {
        return crypto;
    }

    /**
     * @param crypto the crypto to set
     */
    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    /**
     * @return the actor
     */
    public String getActor() {
        return actor;
    }

    /**
     * @param actor the actor to set
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return the receiveAction
     */
    public String getReceiveAction() {
        return receiveAction;
    }

    /**
     * @param receiveAction the receiveAction to set
     */
    public void setReceiveAction(String receiveAction) {
        this.receiveAction = receiveAction;
    }

    /**
     * @return the action
     */
    public String getSendAction() {
        return sendAction;
    }

    /**
     * @param action the action to set
     */
    public void setSendAction(String action) {
        this.sendAction = action;
    }

    /**
     * @return the required
     */
    public boolean isRequired() {
        return required;
    }

    public boolean requireDOM() {
        return true;
    }

    /**
     * @param required the required to set
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    public Object getOption(String key) {
        return properties.get(key);
    }

    public void setOption(String key, Object value) {
        this.properties.put(key, value);
    }

    public String getPassword(Object msgContext) {
        return (String) ((Context) msgContext).getProperty("password");
    }

    public Object getProperty(Object msgContext, String key) {
        if (WSHandlerConstants.PW_CALLBACK_REF.equals(key)) {
            return handler;
        }
        return ((Context) msgContext).getProperty(key); 
    }
    
    public void setHandler(CallbackHandler handler) {
        this.handler = handler;
    }

    public void setPassword(Object msgContext, String password) {
        ((Context) msgContext).setProperty("password", password);
    }

    public void setProperty(Object msgContext, String key, Object value) {
        ((Context) msgContext).setProperty(key, value);
    }

    protected Crypto loadDecryptionCrypto(RequestData reqData) throws WSSecurityException {
        return crypto;
    }
    
    protected Crypto loadEncryptionCrypto(RequestData reqData) throws WSSecurityException {
        return crypto;
    }
    
    public Crypto loadSignatureCrypto(RequestData reqData) throws WSSecurityException {
        return crypto;
    }
    
    public void onReceive(Context context) throws Exception {
        RequestData reqData = new RequestData();
        init(context);
        try {
            reqData.setNoSerialization(true);
            reqData.setMsgContext(context);

            Vector actions = new Vector();
            String action = this.receiveAction;
            if (action == null) {
                throw new IllegalStateException("WSSecurityHandler: No receiveAction defined");
            }
            int doAction = WSSecurityUtil.decodeAction(action, actions);

            Document doc = context.getInMessage().getDocument();
            if (doc == null) {
                throw new IllegalStateException("WSSecurityHandler: The soap message has not been parsed using DOM");
            }

            /*
             * Get and check the Signature specific parameters first because
             * they may be used for encryption too.
             */
            doReceiverAction(doAction, reqData);

            List wsResult = null;

            try {
                wsResult = secEngine.processSecurityHeader(
                                doc, actor, handler, 
                                reqData.getSigCrypto(), 
                                reqData.getDecCrypto());
            } catch (WSSecurityException ex) {
                throw new SoapFault(ex);
            }

            if (wsResult == null) { // no security header found
                if (doAction == WSConstants.NO_SECURITY) {
                    return;
                } else {
                    throw new SoapFault(new WSSecurityException(
                                    "WSSecurityHandler: Request does not contain required Security header"));
                }
            }

            if (reqData.getWssConfig().isEnableSignatureConfirmation()) {
                checkSignatureConfirmation(reqData, wsResult);
            }

            
            /*
             * now check the security actions: do they match, in right order?
             */
            if (!checkReceiverResults(wsResult, actions)) {
                throw new SoapFault(new WSSecurityException(
                                "WSSecurityHandler: security processing failed (actions mismatch)"));

            }
            /*
             * All ok up to this point. Now construct and setup the security
             * result structure. The service may fetch this and check it.
             */
            Vector results = null;
            if ((results = (Vector) context.getProperty(WSHandlerConstants.RECV_RESULTS)) == null) {
                results = new Vector();
                context.setProperty(WSHandlerConstants.RECV_RESULTS, results);
            }
            WSHandlerResult rResult = new WSHandlerResult(actor, wsResult);
            results.add(0, rResult);

            // Add principals to the message
            for (Iterator iter = results.iterator(); iter.hasNext();) {
                WSHandlerResult hr = (WSHandlerResult) iter.next();
                for (Iterator it = hr.getResults().iterator(); it.hasNext();) {
                    WSSecurityEngineResult er = (WSSecurityEngineResult) it.next();
                    if (er.get(WSSecurityEngineResult.TAG_PRINCIPAL) != null) {
                        context.getInMessage().addPrincipal((Principal)er.get(WSSecurityEngineResult.TAG_PRINCIPAL));
                    }
                }
            }
            Subject s = (Subject) currentSubject.get();
            if (s != null) {
                for (Iterator iterator = s.getPrincipals().iterator(); iterator.hasNext();) {
                    Principal p = (Principal) iterator.next();
                    context.getInMessage().addPrincipal(p);
                }
            }

        } finally {
            reqData.clear();
            currentSubject.set(null);
            currentHandler.set(null);
        }
    }

    public void onReply(Context context) throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    public void onFault(Context context) throws Exception {
        // TODO Auto-generated method stub

    }

    public void onSend(Context context) throws Exception {
        RequestData reqData = new RequestData();
        reqData.setMsgContext(context);
        init(context);
        /*
         * The overall try, just to have a finally at the end to perform some
         * housekeeping.
         */
        try {
            /*
             * Get the action first.
             */
            Vector actions = new Vector();
            String action = this.sendAction;
            if (action == null) {
                throw new IllegalStateException("WSSecurityHandler: No sendAction defined");
            }
            
            int doAction = WSSecurityUtil.decodeAction(action, actions);
            if (doAction == WSConstants.NO_SECURITY) {
                return;
            }

            /*
             * For every action we need a username, so get this now. The
             * username defined in the deployment descriptor takes precedence.
             */
            reqData.setUsername((String) getOption(WSHandlerConstants.USER));
            if (reqData.getUsername() == null || reqData.getUsername().equals("")) {
                String username = (String) getProperty(reqData.getMsgContext(), WSHandlerConstants.USER);
                if (username != null) {
                    reqData.setUsername(username);
                } else {
                    reqData.setUsername(this.username);
                }
            }
            
            /*
             * Now we perform some set-up for UsernameToken and Signature
             * functions. No need to do it for encryption only. Check if
             * username is available and then get a passowrd.
             */
            if ((doAction & (WSConstants.SIGN | WSConstants.UT | WSConstants.UT_SIGN)) != 0) {
                /*
                 * We need a username - if none throw an XFireFault. For
                 * encryption there is a specific parameter to get a username.
                 */
                if (reqData.getUsername() == null || reqData.getUsername().equals("")) {
                    throw new IllegalStateException("WSSecurityHandler: Empty username for specified action");
                }
            }
            /*
             * Now get the SOAP part from the request message and convert it
             * into a Document.
             * 
             * Now we can perform our security operations on this request.
             */
            Document doc = context.getInMessage().getDocument();
            if (doc == null) {
                throw new IllegalStateException("WSSecurityHandler: The soap message has not been parsed using DOM");
            }
            
            doSenderAction(doAction, doc, reqData, actions, true);
        }
        catch (WSSecurityException e) {
            throw new SoapFault(e);
        }
        finally {
            reqData.clear();
            reqData = null;
            currentHandler.set(null);
        }
    }

    public void onAnswer(Context context) {
        // TODO Auto-generated method stub

    }
    
    protected void checkUser(String user, Object credentials) throws GeneralSecurityException {
        if (authenticationService == null) {
            throw new IllegalArgumentException("authenticationService is null");
        }
        Subject subject = (Subject) currentSubject.get();
        if (subject == null) {
            subject = new Subject();
            currentSubject.set(subject);
        }
        authenticationService.authenticate(subject, domain, user, credentials);
    }

    protected class DefaultHandler extends BaseSecurityCallbackHandler {

        protected void processSignature(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
            callback.setPassword("");
        }
        
        protected void processUsernameTokenUnkown(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
            try {
                checkUser(callback.getIdentifier(), callback.getPassword());
            } catch (GeneralSecurityException e) {
                throw new UnsupportedCallbackException(callback, "Unable to authenticate user");
            }
        }
        
        
    }

    /**
     * @return the keystore
     */
    public String getKeystore() {
        return keystore;
    }

    /**
     * @param keystore the keystore to set
     */
    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }
    
    protected void init(Context context) {
        currentSubject.set(null);
        currentHandler.set(this);
        if (context.getProperty(Context.AUTHENTICATION_SERVICE) != null) {
            setAuthenticationService((AuthenticationService) context.getProperty(Context.AUTHENTICATION_SERVICE));
        }
        if (crypto == null && context.getProperty(Context.KEYSTORE_MANAGER) != null && keystore != null) {
            KeystoreManager km = (KeystoreManager) context.getProperty(Context.KEYSTORE_MANAGER);
            setCrypto(new KeystoreInstanceCrypto(km, keystore));
        }
    }

}
