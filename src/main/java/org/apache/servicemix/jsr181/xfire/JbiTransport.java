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
package org.apache.servicemix.jsr181.xfire;

import javax.jbi.component.ComponentContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.handler.LocateBindingHandler;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.soap.SoapTransport;
import org.codehaus.xfire.soap.handler.SoapBodyHandler;
import org.codehaus.xfire.transport.AbstractTransport;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.DefaultEndpoint;
import org.codehaus.xfire.wsdl11.WSDL11Transport;

/**
 * Simple jbi transport, similar to local transport,
 * but without soap encoding. 
 * 
 */
public class JbiTransport extends AbstractTransport implements WSDL11Transport, SoapTransport {

    public static final String JBI_BINDING = "http://java.sun.com/xml/ns/jbi/binding/service+engine";
    
    private static final Log LOG = LogFactory.getLog(JbiTransport.class);
    
    private static final String URI_PREFIX = "urn:xfire:transport:jbi:";

    private ComponentContext context;
    
    public JbiTransport(ComponentContext context) {
        addInHandler(new LocateBindingHandler());
        addInHandler(new SoapBodyHandler());
        this.context = context;
    }
    
    public String getName() {
        return "JBI";
    }
    
    public String getServiceURL(Service service) {
        return "jbi://" + service.getName();
    }

    protected Channel createNewChannel(String uri) {
        LOG.debug("Creating new channel for uri: " + uri);
        JbiChannel c = new JbiChannel(uri, this);
        c.setEndpoint(new DefaultEndpoint());
        return c;
    }

    protected String getUriPrefix() {
        return URI_PREFIX;
    }

    public String[] getSupportedBindings() {
        return new String[] {JBI_BINDING };
    }

    public String[] getKnownUriSchemes() {
        return new String[] {"jbi://" };
    }

    public ComponentContext getContext() {
        return context;
    }
    
    public boolean equals(Object o) {
        return o instanceof JbiTransport;
    }

    public int hashCode() {
        return JbiTransport.class.hashCode();
    }
}

