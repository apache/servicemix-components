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
package org.apache.servicemix.drools;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.drools.model.JbiHelper;
import org.drools.RuleBase;
import org.drools.WorkingMemory;
import org.drools.compiler.RuleBaseLoader;
import org.springframework.core.io.Resource;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="endpoint"
 */
public class DroolsEndpoint extends ProviderEndpoint {

    private RuleBase ruleBase;
    private Resource ruleBaseResource;
    private URL ruleBaseURL;
    private NamespaceContext namespaceContext;
    private QName defaultTargetService;
    private String defaultTargetURI;
    private Map<String, Object> globals;
    private List<Object> assertedObjects;

    public DroolsEndpoint() {
        super();
    }

    public DroolsEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public DroolsEndpoint(ServiceUnit su, QName service, String endpoint) {
        super(su, service, endpoint);
    }

    /**
     * @return the ruleBase
     */
    public RuleBase getRuleBase() {
        return ruleBase;
    }

    /**
     * @param ruleBase the ruleBase to set
     */
    public void setRuleBase(RuleBase ruleBase) {
        this.ruleBase = ruleBase;
    }

    /**
     * @return the ruleBaseResource
     */
    public Resource getRuleBaseResource() {
        return ruleBaseResource;
    }

    /**
     * @param ruleBaseResource the ruleBaseResource to set
     */
    public void setRuleBaseResource(Resource ruleBaseResource) {
        this.ruleBaseResource = ruleBaseResource;
    }

    /**
     * @return the ruleBaseURL
     */
    public URL getRuleBaseURL() {
        return ruleBaseURL;
    }

    /**
     * @param ruleBaseURL the ruleBaseURL to set
     */
    public void setRuleBaseURL(URL ruleBaseURL) {
        this.ruleBaseURL = ruleBaseURL;
    }

    /**
     * @return the namespaceContext
     */
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * @param namespaceContext the namespaceContext to set
     */
    public void setNamespaceContext(NamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    /**
     * @return the variables
     */
    public Map<String, Object> getGlobals() {
        return globals;
    }

    /**
     * @param variables the variables to set
     */
    public void setGlobals(Map<String, Object> variables) {
        this.globals = variables;
    }

    public void validate() throws DeploymentException {
        super.validate();
        if (ruleBase == null && ruleBaseResource == null && ruleBaseURL == null) {
            throw new DeploymentException("Property ruleBase, ruleBaseResource or ruleBaseURL must be set");
        }
    }
    
    public void start() throws Exception {
        super.start();
        if (ruleBase == null) {
            InputStream is = null;
            try {
                if (ruleBaseResource != null) {
                    is = ruleBaseResource.getInputStream();
                } else if (ruleBaseURL != null) {
                    is = ruleBaseURL.openStream();
                } else {
                    throw new IllegalArgumentException("Property ruleBase, ruleBaseResource "
                            + "or ruleBaseURL must be set");
                }
                RuleBaseLoader loader = RuleBaseLoader.getInstance();
                ruleBase = loader.loadFromReader(new InputStreamReader(is));
            } catch (Exception e) {
                throw new JBIException(e);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(
     *      javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage)
     */
    public void process(MessageExchange exchange) throws Exception {
        drools(exchange);
    }
    
    protected void drools(MessageExchange exchange) throws Exception {
        WorkingMemory memory = createWorkingMemory(exchange);
        populateWorkingMemory(memory, exchange);
        memory.fireAllRules();
        postProcess(exchange, memory);
    }

    protected void postProcess(MessageExchange exchange, WorkingMemory memory) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            String uri = getDefaultRouteURI();
            if (uri != null) {
                JbiHelper helper = (JbiHelper) memory.getGlobal("jbi");
                helper.route(uri);
            }
        }
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            fail(exchange, new Exception("No rules have handled the exchange. Check your rule base."));
        }
    }
    
    protected WorkingMemory createWorkingMemory(MessageExchange exchange) throws Exception {
        return ruleBase.newWorkingMemory();
    }

    protected void populateWorkingMemory(WorkingMemory memory, MessageExchange exchange) throws Exception {
        memory.setGlobal("jbi", new JbiHelper(this, exchange, memory));
        if (assertedObjects != null) {
            for (Object o : assertedObjects) {
                memory.assertObject(o);
            }
        }
        if (globals != null) {
            for (Map.Entry<String, Object> e : globals.entrySet()) {
                memory.setGlobal(e.getKey(), e.getValue());
            }
        }
    }

    public QName getDefaultTargetService() {
        return defaultTargetService;
    }

    public void setDefaultTargetService(QName defaultTargetService) {
        this.defaultTargetService = defaultTargetService;
    }

    public String getDefaultTargetURI() {
        return defaultTargetURI;
    }

    public void setDefaultTargetURI(String defaultTargetURI) {
        this.defaultTargetURI = defaultTargetURI;
    }

    public List<Object> getAssertedObjects() {
        return assertedObjects;
    }

    public void setAssertedObjects(List<Object> assertedObjects) {
        this.assertedObjects = assertedObjects;
    }

    public String getDefaultRouteURI() {
        if (defaultTargetURI != null) {
            return defaultTargetURI;
        } else if (defaultTargetService != null) {
            String nsURI = defaultTargetService.getNamespaceURI();
            String sep = (nsURI.indexOf("/") > 0) ? "/" : ":";
            return "service:" + nsURI + sep + defaultTargetService.getLocalPart();
        } else {
            return null;
        }
    }
}
