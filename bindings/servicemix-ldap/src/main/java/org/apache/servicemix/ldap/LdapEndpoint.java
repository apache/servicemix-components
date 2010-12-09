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
package org.apache.servicemix.ldap;

import javax.jbi.messaging.MessageExchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * <p>
 * This generic endpoint is a listener which is waiting for incoming normalized message
 * and perform the action on the LDAP directory (depending of the operation): search, add, delete, modify.
 * </p>
 * 
 * @author jbonofre
 */
public class LdapEndpoint extends ProviderEndpoint implements LdapEndpointType {
    
    // logging facility
    private final static transient Log LOG = LogFactory.getLog(LdapEndpoint.class);
    
    /**
     * <p>
     * Generic LDAP endpoint that waiting for incoming SOAP message and
     * process the corresponding operation (search, add, delete, modify).
     * </p>
     */
    public LdapEndpoint() {
        super();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#activate()
     */
    public void activate() {
        this.activate();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#start()
     */
    public void start() {
        this.start();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange exchange) {
        /*
         * Description of this endpoint behavior:
         * The endpoint expects a incoming message containing a payload with a SOAP envelope.
         * This SOAP envelop can have the following operations:
         *   * search
         *   * add
         *   * delete
         *   * modify
         *  Depending of the operation, the marshaled request can have different form.
         *  The endpoint could work with the following MEP: InOut (returning the found/added/deleted/modified LDAP entries)
         *  InOnly (no message returned), RobustInOnly (only a ACK message is sent back). InOptionalOut will not be supported.  
         */
    }

}
