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
package org.apache.servicemix.wsn.component;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.wsn.ComponentContextAware;
import org.oasis_open.docs.wsrf.bf_2.BaseFaultType;

public class WSNEndpoint extends ProviderEndpoint implements ExchangeProcessor {

    protected String address;

    protected Object pojo;

    protected JAXBContext jaxbContext;

    protected Class endpointInterface;

    public WSNEndpoint(String address, Object pojo) {
        this.address = address;
        this.pojo = pojo;
        String[] parts = URIResolver.split3(address);
        service = new QName(parts[0], parts[1]);
        endpoint = parts[2];
    }

    @Override
    public void start() throws Exception {
        if (pojo instanceof ComponentContextAware) {
            ((ComponentContextAware) pojo).setContext(getContext());
        }
        logger = this.serviceUnit.getComponent().getLogger();
        WebService ws = getWebServiceAnnotation(pojo.getClass());
        if (ws == null) {
            throw new IllegalStateException("Unable to find WebService annotation");
        }
        endpointInterface = Class.forName(ws.endpointInterface());
        jaxbContext = createJAXBContext(endpointInterface);
        ws = getWebServiceAnnotation(endpointInterface);
        if (ws != null) {
            interfaceName = new QName(ws.targetNamespace(), ws.name());
        }
        super.start();
    }

    public static JAXBContext createJAXBContext(Class interfaceClass) throws JAXBException {
        List<Class> classes = new ArrayList<Class>();
        classes.add(JbiFault.class);
        for (Method mth : interfaceClass.getMethods()) {
            WebMethod wm = (WebMethod) mth.getAnnotation(WebMethod.class);
            if (wm != null) {
                classes.add(mth.getReturnType());
                classes.addAll(Arrays.asList(mth.getParameterTypes()));
            }
        }
        return JAXBContext.newInstance(classes.toArray(new Class[classes.size()]));
    }

    @SuppressWarnings("unchecked")
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }
        Object input = jaxbContext.createUnmarshaller().unmarshal(exchange.getMessage("in").getContent());
        Method webMethod = null;
        for (Method mth : endpointInterface.getMethods()) {
            Class[] params = mth.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(input.getClass())) {
                webMethod = mth;
                break;
            }
        }
        if (webMethod == null) {
            throw new IllegalStateException("Could not determine invoked web method");
        }
        boolean oneWay = webMethod.getAnnotation(Oneway.class) != null;
        Object output;
        try {
            output = webMethod.invoke(pojo, new Object[] {input });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                WebFault fa = (WebFault) e.getCause().getClass().getAnnotation(WebFault.class);
                if (!(exchange instanceof InOnly) && fa != null) {
                    BaseFaultType info = (BaseFaultType) e.getCause().getClass().getMethod("getFaultInfo").invoke(
                            e.getCause());
                    Fault fault = exchange.createFault();
                    exchange.setFault(fault);
                    exchange.setError((Exception) e.getCause());
                    StringWriter writer = new StringWriter();
                    jaxbContext.createMarshaller().marshal(new JbiFault(info), writer);
                    fault.setContent(new StringSource(writer.toString()));
                    send(exchange);
                    return;
                } else {
                    throw (Exception) e.getCause();
                }
            } else if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
        if (oneWay) {
            exchange.setStatus(ExchangeStatus.DONE);
            send(exchange);
        } else {
            NormalizedMessage msg = exchange.createMessage();
            exchange.setMessage(msg, "out");
            StringWriter writer = new StringWriter();
            jaxbContext.createMarshaller().marshal(output, writer);
            msg.setContent(new StringSource(writer.toString()));
            send(exchange);
        }
    }

    @XmlRootElement(name = "Fault")
    public static class JbiFault {
        private BaseFaultType info;

        public JbiFault() {
        }

        public JbiFault(BaseFaultType info) {
            this.info = info;
        }

        public BaseFaultType getInfo() {
            return info;
        }

        public void setInfo(BaseFaultType info) {
            this.info = info;
        }
    }

    protected Method getWebServiceMethod(QName interfaceName, QName operation) throws Exception {
        WebService ws = getWebServiceAnnotation(pojo.getClass());
        if (ws == null) {
            throw new IllegalStateException("Unable to find WebService annotation");
        }
        Class itf = Class.forName(ws.endpointInterface());
        for (Method mth : itf.getMethods()) {
            WebMethod wm = (WebMethod) mth.getAnnotation(WebMethod.class);
            if (wm != null) {
                // TODO: get name ?
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected WebService getWebServiceAnnotation(Class clazz) {
        for (Class cl = clazz; cl != null; cl = cl.getSuperclass()) {
            WebService ws = (WebService) cl.getAnnotation(WebService.class);
            if (ws != null) {
                return ws;
            }
        }
        return null;
    }

}
