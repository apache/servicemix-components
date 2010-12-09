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
package org.apache.servicemix.soap.ws.security;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.SoapInterceptor;
import org.apache.servicemix.soap.core.AbstractInterceptor;
//import org.apache.ws.security.WSConstants;

public class WsSecurityInInterceptor extends AbstractInterceptor implements SoapInterceptor {

    public static QName[] HEADERS = {
    /*
        new QName(WSConstants.WSSE_NS, WSConstants.WSSE_LN),  
        new QName(WSConstants.WSSE11_NS, WSConstants.WSSE_LN),
    */  
    };
    
    private final WsSecurityPolicy policy;
    
    public WsSecurityInInterceptor(WsSecurityPolicy policy) {
        this.policy = policy;
    }
    
    public void handleMessage(Message message) {
        boolean required = policy.isRequired();
        // TODO
    }

    public Collection<QName> getUnderstoodHeaders() {
        return Arrays.asList(HEADERS);
    }

    public Collection<URI> getRoles() {
        return Collections.emptyList();
    }
    
}
