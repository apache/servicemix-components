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
package org.apache.servicemix.wsn.client;

import javax.jbi.JBIException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.component.ComponentContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import org.apache.servicemix.common.util.DOMUtil;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.wsn.jbi.JbiWrapperHelper;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsrf.rp_2.GetResourcePropertyResponse;

public abstract class AbstractWSAClient {

    private static JAXBContext jaxbContext;

    private W3CEndpointReference endpoint;

    private ComponentContext context;

    private ServiceEndpoint serviceEndpoint;

    private boolean jbiWrapped;

    private SourceTransformer transformer;

    public AbstractWSAClient() {
    }

    public AbstractWSAClient(ComponentContext context, W3CEndpointReference endpoint) {
        this.context = context;
        this.endpoint = endpoint;
    }

    public boolean isJbiWrapped() {
        return jbiWrapped;
    }

    public void setJbiWrapped(boolean jbiWrapped) {
        this.jbiWrapped = jbiWrapped;
    }

    public static W3CEndpointReference createWSA(String address) {
        Source src = new StringSource("<EndpointReference xmlns='http://www.w3.org/2005/08/addressing'><Address>"
                                        + address + "</Address></EndpointReference>");
        return new W3CEndpointReference(src);
    }

    public static String getWSAAddress(W3CEndpointReference ref) {
        try {
            Element element = JbiWrapperHelper.createDocument().createElement("elem");
            ref.writeTo(new DOMResult(element));
            NodeList nl = element.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "Address");
            if (nl != null && nl.getLength() > 0) {
                Element e = (Element) nl.item(0);
                return DOMUtil.getElementText(e).trim();
            }
        } catch (ParserConfigurationException e) {
            // Ignore
        }
        return null;
    }

    public static ServiceEndpoint resolveWSA(W3CEndpointReference ref, ComponentContext context) {
        String[] parts = URIResolver.split3(getWSAAddress(ref));
        return context.getEndpoint(new QName(parts[0], parts[1]), parts[2]);
    }

    public W3CEndpointReference getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(W3CEndpointReference endpoint) {
        this.endpoint = endpoint;
    }

    public ServiceEndpoint getServiceEndpoint() {
        if (serviceEndpoint == null && endpoint != null) {
            serviceEndpoint = resolveWSA(endpoint, context);
        }
        return serviceEndpoint;
    }

    public void setServiceEndpoint(ServiceEndpoint serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public ComponentContext getContext() {
        return context;
    }

    public void setContext(ComponentContext context) {
        this.context = context;
    }

    protected Object request(Object request) throws JBIException {
        return request(null, request);
    }

    protected Object request(QName operation, Object request) throws JBIException {
        try {
            InOut exchange = getContext().getDeliveryChannel().createExchangeFactory().createInOutExchange();
            exchange.setEndpoint(getServiceEndpoint());
            exchange.setOperation(operation);
            NormalizedMessage in = exchange.createMessage();
            exchange.setInMessage(in);
            if (isJbiWrapped()) {
                Document doc = JbiWrapperHelper.createDocument();
                getJAXBContext().createMarshaller().marshal(request, doc);
                JbiWrapperHelper.wrap(doc);
                in.setContent(new DOMSource(doc));
            } else {
                in.setContent(new JAXBSource(getJAXBContext(), request));
            }
            getContext().getDeliveryChannel().sendSync(exchange);
            if (exchange.getStatus() == ExchangeStatus.ERROR) {
                throw new JBIException(exchange.getError());
            } else if (exchange.getFault() != null) {
                if (transformer == null) {
                    transformer = new SourceTransformer();
                }
                String fault = transformer.contentToString(exchange.getFault());
                exchange.setStatus(ExchangeStatus.DONE);
                getContext().getDeliveryChannel().send(exchange);
                throw new JBIException(fault);
            } else {
                NormalizedMessage out = exchange.getOutMessage();
                Source source = out.getContent();
                if (isJbiWrapped()) {
                    source = JbiWrapperHelper.unwrap(source);
                }
                Object result = getJAXBContext().createUnmarshaller().unmarshal(source);
                exchange.setStatus(ExchangeStatus.DONE);
                getContext().getDeliveryChannel().send(exchange);
                return result;
            }
        } catch (Exception e) {
            throw new JBIException(e);
        }
    }

    protected void send(Object request) throws JBIException {
        send(null, request);
    }

    protected void send(QName operation, Object request) throws JBIException {
        try {
            InOnly exchange = getContext().getDeliveryChannel().createExchangeFactory().createInOnlyExchange();
            exchange.setEndpoint(getServiceEndpoint());
            exchange.setOperation(operation);
            NormalizedMessage in = exchange.createMessage();
            exchange.setInMessage(in);
            in.setContent(new JAXBSource(getJAXBContext(), request));
            getContext().getDeliveryChannel().sendSync(exchange);
            if (exchange.getStatus() == ExchangeStatus.ERROR) {
                throw new JBIException(exchange.getError());
            }
        } catch (JAXBException e) {
            throw new JBIException(e);
        }
    }

    private synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class, GetResourcePropertyResponse.class);
        }
        return jaxbContext;
    }

}
