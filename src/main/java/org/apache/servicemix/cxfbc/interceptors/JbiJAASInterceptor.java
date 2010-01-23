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
package org.apache.servicemix.cxfbc.interceptors;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.security.auth.Subject;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;


public class JbiJAASInterceptor extends AbstractWSS4JInterceptor {

    private String domain = "servicemix-domain";
    private AuthenticationService authenticationService;
    private ThreadLocal<Subject> currentSubject = new ThreadLocal<Subject>();
    private boolean x509;
    private boolean delegateToJaas;
    
    
    public JbiJAASInterceptor(AuthenticationService authenticationService, boolean x509, boolean delegateToJaas) {
        super();
        setPhase(Phase.PRE_PROTOCOL);
        getAfter().add(WSS4JInInterceptor.class.getName());
        this.authenticationService = authenticationService;
        this.x509 = x509;
        this.delegateToJaas = delegateToJaas;
    }
    
    
    public void handleMessage(SoapMessage message) throws Fault {
     
        try {
            if (!delegateToJaas) {
                return;
            }
            Subject subject = (Subject) currentSubject.get();
            
            if (subject == null) {
                subject = new Subject();
                currentSubject.set(subject);
            }
            List<Object> results = (Vector<Object>)message.get(WSHandlerConstants.RECV_RESULTS);
            if (results == null) {
                return;
            }
            for (Iterator iter = results.iterator(); iter.hasNext();) {
                WSHandlerResult hr = (WSHandlerResult) iter.next();
                if (hr == null || hr.getResults() == null) {
                    return;
                }
                boolean authenticated = false;
                
                //favor WSSE UsernameToken based authentication over X.509 certificate
                //based authentication. For that purpose we iterate twice over the 
                //WSHandler result list
                for (Iterator it = hr.getResults().iterator(); it.hasNext();) {
                    WSSecurityEngineResult er = (WSSecurityEngineResult) it.next();
                        
                    if (er != null && er.getPrincipal() instanceof WSUsernameTokenPrincipal) {
                        WSUsernameTokenPrincipal p = (WSUsernameTokenPrincipal)er.getPrincipal();
                        subject.getPrincipals().add(p);
                        this.authenticationService.authenticate(subject, domain, p.getName(), p.getPassword());
                        authenticated = true;
                    }
                }
                
                //Second iteration checking for X.509 certificate to run authentication on
                //but only if not already authenticated on WSSE UsernameToken
                if (!authenticated && x509) {
	                for (Iterator it = hr.getResults().iterator(); it.hasNext();) {
	                  WSSecurityEngineResult er = (WSSecurityEngineResult) it.next();
	
	                    if (er != null && er.getCertificate() instanceof X509Certificate) {
	                      X509Certificate cert = er.getCertificate();
	                      this.authenticationService.authenticate(subject, domain, cert.getIssuerX500Principal().getName(), cert);
	                  }
	                }
                }
            }
            
            message.put(Subject.class, subject);
        } catch (GeneralSecurityException e) {
            throw new Fault(e);
        } catch (java.lang.reflect.UndeclaredThrowableException e) {
            java.lang.Throwable undeclared = e.getUndeclaredThrowable();
            if (undeclared != null
                    && undeclared instanceof java.lang.reflect.InvocationTargetException) {
                throw new Fault(
                        ((java.lang.reflect.InvocationTargetException) undeclared)
                                .getTargetException());
            }

        } finally {
            currentSubject.set(null);
        }
    }

}
