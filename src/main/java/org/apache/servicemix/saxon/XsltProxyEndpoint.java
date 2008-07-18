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
package org.apache.servicemix.saxon;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.jbi.jaxp.BytesSource;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.apache.servicemix.saxon.support.ExchangeTarget;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.StoreFactory;
import org.apache.servicemix.store.memory.MemoryStoreFactory;
import org.apache.servicemix.JbiConstants;
import org.springframework.core.io.Resource;
import net.sf.saxon.TransformerFactoryImpl;

/**
 * @org.apache.xbean.XBean element="proxy"
 */
public class XsltProxyEndpoint extends SaxonEndpoint {

    public static final int IN = 0;
    public static final int OUT = 1;
    public static final int FAULT = 2;

    private TransformerFactory transformerFactory;
    private Resource outResource;
    private Resource faultResource;
    private Source[] xsltSource = new Source[3];
    private Templates[] templates = new Templates[3];
    private boolean useDomSourceForXslt = true;
    private Boolean useDomSourceForContent;
    private ExchangeTarget target;
    /**
     * The store to keep pending exchanges
     */
    protected Store store;
    /**
     * The store factory.
     */
    protected StoreFactory storeFactory;
    /**
     * The correlation property used by this component
     */
    private String correlation;

    public TransformerFactory getTransformerFactory() {
        if (transformerFactory == null) {
            transformerFactory = createTransformerFactory();
        }
        return transformerFactory;
    }

    public void setTransformerFactory(TransformerFactory transformerFactory) {
        this.transformerFactory = transformerFactory;
    }

    public Resource getOutResource() {
        return outResource;
    }

    public void setOutResource(Resource outResource) {
        this.outResource = outResource;
    }

    public Resource getFaultResource() {
        return faultResource;
    }

    public void setFaultResource(Resource faultResource) {
        this.faultResource = faultResource;
    }

    public boolean isUseDomSourceForXslt() {
        return useDomSourceForXslt;
    }

    public void setUseDomSourceForXslt(boolean useDomSourceForXslt) {
        this.useDomSourceForXslt = useDomSourceForXslt;
    }

    public Boolean getUseDomSourceForContent() {
        return useDomSourceForContent;
    }

    public void setUseDomSourceForContent(Boolean useDomSourceForContent) {
        this.useDomSourceForContent = useDomSourceForContent;
    }

    public ExchangeTarget getTarget() {
        return target;
    }

    public void setTarget(ExchangeTarget target) {
        this.target = target;
    }

    /**
     * @return Returns the store.
     */
    public Store getStore() {
        return store;
    }
    /**
     * @param store The store to set.
     */
    public void setStore(Store store) {
        this.store = store;
    }
    /**
     * @return Returns the storeFactory.
     */
    public StoreFactory getStoreFactory() {
        return storeFactory;
    }
    /**
     * @param storeFactory The storeFactory to set.
     */
    public void setStoreFactory(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public void validate() throws DeploymentException {
        if (target == null) {
            throw new DeploymentException("target must be specified");
        }
        // Create correlation property
        correlation = "XsltProxy.Correlation." + getService() + "." + getEndpoint();
    }

    public void start() throws Exception {
        super.start();
        // Create store
        if (store == null) {
            if (storeFactory == null) {
                storeFactory = new MemoryStoreFactory();
            }
            store = storeFactory.open(getService().toString() + getEndpoint());
        }
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getRole() == MessageExchange.Role.PROVIDER && exchange.getProperty(correlation) == null) {
            // Create exchange for target
            MessageExchange tme = getExchangeFactory().createExchange(exchange.getPattern());
            if (store.hasFeature(Store.CLUSTERED)) {
                exchange.setProperty(JbiConstants.STATELESS_PROVIDER, Boolean.TRUE);
                tme.setProperty(JbiConstants.STATELESS_CONSUMER, Boolean.TRUE);
            }
            target.configureTarget(tme, getContext());
            // Set correlations
            exchange.setProperty(correlation, tme.getExchangeId());
            tme.setProperty(correlation, exchange.getExchangeId());
            // Put exchange to store
            store.store(exchange.getExchangeId(), exchange);
            // Transform
            tme.setMessage(tme.createMessage(), "in");
            transform(exchange, exchange.getMessage("in"), tme, tme.getMessage("in"), IN);
            // Send to target
            if (tme.getOperation() == null) {
                tme.setOperation(exchange.getOperation());
            }
            send(tme);
        // Mimic the exchange on the other side and send to needed listener
        } else {
            String id = (String) exchange.getProperty(correlation);
            if (id == null) {
                if (exchange.getRole() == MessageExchange.Role.CONSUMER
                    && exchange.getStatus() != ExchangeStatus.ACTIVE) {
                    // This must be a listener status, so ignore
                    return;
                }
                throw new IllegalStateException(correlation + " property not found");
            }
            MessageExchange org = (MessageExchange) store.load(id);
            if (org == null) {
                throw new IllegalStateException("Could not load original exchange with id " + id);
            }
            // Reproduce DONE status to the other side
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                done(org);
            // Reproduce ERROR status to the other side
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                fail(org, exchange.getError());
            // Reproduce faults to the other side and listeners
            } else if (exchange.getFault() != null) {
                store.store(exchange.getExchangeId(), exchange);
                // Transform
                org.setFault(org.createFault());
                transform(exchange, exchange.getFault(), org, org.getFault(), FAULT);
                // Send to target
                send(org);
            // Reproduce answers to the other side
            } else if (exchange.getMessage("out") != null) {
                store.store(exchange.getExchangeId(), exchange);
                // Transform
                org.setMessage(org.createMessage(), "out");
                transform(exchange, exchange.getMessage("out"), org, org.getMessage("out"), OUT);
                // Send to target
                send(org);
            } else {
                throw new IllegalStateException("Exchange status is " + ExchangeStatus.ACTIVE
                        + " but has no Out nor Fault message");
            }
        }
    }

    protected void transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected void transform(MessageExchange inExchange, NormalizedMessage inMessage, MessageExchange outExchange, NormalizedMessage outMessage, int type) throws Exception {
        Transformer transformer = createTransformer(inExchange, inMessage, type);
        if (transformer != null) {
            configureTransformer(transformer, inExchange, inMessage, outExchange, outMessage);
            transformContent(transformer, inExchange, inMessage, outMessage);
            copyPropertiesAndAttachments(inMessage, outMessage);
        } else {
            MessageUtil.transfer(inMessage, outMessage);
        }
    }

    protected void transformContent(Transformer transformer, MessageExchange exchange,
                                    NormalizedMessage in, NormalizedMessage out) throws Exception {
        Source src = in.getContent();
        if (useDomSourceForContent != null && useDomSourceForContent.booleanValue()) {
            src = new DOMSource(getSourceTransformer().toDOMDocument(src));
        } else if (useDomSourceForContent != null && !useDomSourceForContent.booleanValue()) {
            src = getSourceTransformer().toStreamSource(src);
        } else {
            if (src instanceof DOMSource) {
                src = new DOMSource(getSourceTransformer().toDOMDocument(src));
            }
        }
        if (RESULT_BYTES.equalsIgnoreCase(getResult())) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Result result = new StreamResult(buffer);
            transformer.transform(src, result);
            out.setContent(new BytesSource(buffer.toByteArray()));
        } else if (RESULT_STRING.equalsIgnoreCase(getResult())) {
            StringWriter buffer = new StringWriter();
            Result result = new StreamResult(buffer);
            transformer.transform(src, result);
            out.setContent(new StringSource(buffer.toString()));
        } else {
            DOMResult result = new DOMResult();
            transformer.transform(src, result);
            out.setContent(new DOMSource(result.getNode()));
        }
    }

    protected Source getXsltSource(int type) throws Exception {
        if (xsltSource[type] == null) {
            xsltSource[type] = createXsltSource(getResource(type));
        }
        return xsltSource[type];
    }

    protected Resource getResource(int type) {
        switch (type) {
            case IN: return getResource();
            case OUT: return getOutResource();
            case FAULT: return getFaultResource();
            default: throw new IllegalStateException();
        }
    }

    protected Source createXsltSource(Resource res) throws Exception {
        String url = null;
        try {
            url = res.getURL().toURI().toString();
        } catch (Exception e) {
            // Ignore
        }
        if (useDomSourceForXslt) {
            return new DOMSource(parse(res), url);
        } else {
            return new StreamSource(res.getInputStream(), url);
        }
    }

    public synchronized Templates getTemplates(int type) throws Exception {
        if (templates[type] == null) {
            templates[type] = createTemplates(type);
        }
        return templates[type];
    }

    /**
     * Factory method to create a new transformer instance
     */
    protected Templates createTemplates(int type) throws Exception {
        Source source = getXsltSource(type);
        return getTransformerFactory().newTemplates(source);
    }

    /**
     * Factory method to create a new transformer instance
     */
    protected Transformer createTransformer(MessageExchange exchange, NormalizedMessage msg, int type) throws Exception {
        // Use dynamic stylesheet selection
        if (getExpression() != null) {
            Resource r = getDynamicResource(exchange, msg);
            if (r == null) {
                return getTransformerFactory().newTransformer();
            } else {
                Source source = createXsltSource(r);
                return getTransformerFactory().newTransformer(source);
            }
        // Use static stylesheet
        } else if (getResource(type) != null) {
            return getTemplates(type).newTransformer();
        } else {
            return null;
        }
    }

    protected TransformerFactory createTransformerFactory() {
        if (getConfiguration() != null) {
            return new TransformerFactoryImpl(getConfiguration());
        } else {
            return new TransformerFactoryImpl();
        }
    }

    /**
     * A hook to allow the transformer to be configured from the current
     * exchange and inbound message
     */
    protected void configureTransformer(Transformer transformer,
                                        MessageExchange inExchange, NormalizedMessage inMessage,
                                        MessageExchange outExchange, NormalizedMessage outMessage) {
        for (Iterator iter = inExchange.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = inExchange.getProperty(name);
            transformer.setParameter(name, value);
        }
        for (Iterator iter = inMessage.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = inMessage.getProperty(name);
            transformer.setParameter(name, value);
        }
        Map parameters = getParameters();
        if (parameters != null) {
            for (Iterator iter = parameters.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                Object value = parameters.get(name);
                transformer.setParameter(name, value);
            }
        }
        transformer.setParameter("inExchange", inExchange);
        transformer.setParameter("inMessage", inMessage);
        transformer.setParameter("outExchange", outExchange);
        transformer.setParameter("outMessage", outMessage);
        transformer.setParameter("component", this);
    }

}
