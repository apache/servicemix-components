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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.servicemix.jbi.jaxp.StAXSourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jsr181.JBIContext;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFireException;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.MessageSerializer;
import org.codehaus.xfire.exchange.OutMessage;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.soap.AbstractSoapBinding;
import org.codehaus.xfire.transport.AbstractChannel;
import org.codehaus.xfire.transport.Channel;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;

/**
 * Jbi channel, only support local invocations. 
 */
public class JbiChannel extends AbstractChannel {

    public static final String JBI_INTERFACE_NAME = "jbi.interface";
    public static final String JBI_SERVICE_NAME = "jbi.service";
    public static final String JBI_ENDPOINT = "jbi.endpoint";
    public static final String JBI_SECURITY_PROPAGATATION = "jbi.security.propagation";

    private static ThreadLocal transformer = new ThreadLocal();

    public JbiChannel(String uri, JbiTransport transport) {
        setTransport(transport);
        setUri(uri);
    }

    protected static StAXSourceTransformer getTransformer() {
        StAXSourceTransformer t = (StAXSourceTransformer) transformer.get();
        if (t == null) {
            t = new StAXSourceTransformer();
            transformer.set(t);
        }
        return t;
    }

    public void open() throws Exception {
    }

    public void send(MessageContext context, OutMessage message) throws XFireException {
        if (Channel.BACKCHANNEL_URI.equals(message.getUri())) {
            final OutputStream out = (OutputStream) context.getProperty(Channel.BACKCHANNEL_URI);
            if (out != null) {
                try {
                    final XMLStreamWriter writer = getTransformer().getOutputFactory()
                        .createXMLStreamWriter(out, message.getEncoding());
                    message.getSerializer().writeMessage(message, writer, context);
                    writer.close();
                } catch (XMLStreamException e) {
                    throw new XFireException("Error closing output stream", e);
                }
                return;
            }
        } else {
            try {
                DeliveryChannel channel = ((JbiTransport) getTransport()).getContext().getDeliveryChannel();
                MessageExchangeFactory factory = channel.createExchangeFactory();
                if (context.getExchange().hasOutMessage()) {
                    InOut me = factory.createInOutExchange();
                    me.setInterfaceName((QName) context.getService().getProperty(JBI_INTERFACE_NAME));
                    me.setOperation(context.getExchange().getOperation().getQName());
                    me.setService((QName) context.getService().getProperty(JBI_SERVICE_NAME));
                    me.setEndpoint((ServiceEndpoint) context.getService().getProperty(JBI_ENDPOINT));
                    NormalizedMessage msg = me.createMessage();
                    me.setInMessage(msg);
                    if (Boolean.TRUE.equals(context.getService().getProperty(JBI_SECURITY_PROPAGATATION))) {
                        MessageExchange oldMe = JBIContext.getMessageExchange();
                        NormalizedMessage oldMsg = (oldMe != null) ? oldMe.getMessage("in") : null;
                        if (oldMsg != null) {
                            msg.setSecuritySubject(oldMsg.getSecuritySubject());
                        }
                    }
                    msg.setContent(getContent(context, message));
                    if (!channel.sendSync(me)) {
                        throw new XFireException("Unable to send jbi exchange: sendSync returned false");
                    }
                    if (me.getStatus() == ExchangeStatus.ERROR) {
                        me.setStatus(ExchangeStatus.DONE);
                        channel.send(me);
                        if (me.getError() != null) {
                            throw new XFireFault(me.getError(), XFireFault.RECEIVER);
                        } else {
                            throw new XFireFault("Unkown Error", XFireFault.RECEIVER);
                        }
                    } else if (me.getFault() != null) {
                        JDOMResult result = new JDOMResult();
                        String str = getTransformer().contentToString(me.getFault());
                        getTransformer().toResult(new StringSource(str), result);
                        Element e = result.getDocument().getRootElement();
                        e = (Element) e.clone();
                        XFireFault xfireFault = new XFireFault(str, XFireFault.RECEIVER);
                        xfireFault.getDetail().addContent(e);
                        throw xfireFault;
                    }
                    Source outSrc = me.getOutMessage().getContent();

                    InMessage inMessage = new InMessage(getTransformer().toXMLStreamReader(outSrc), getUri());
                    getEndpoint().onReceive(context, inMessage);

                    me.setStatus(ExchangeStatus.DONE);
                    channel.send(me);
                } else {
                    // TODO
                }
            } catch (XFireException e) {
                throw e;
            } catch (Exception e) {
                throw new XFireException("Error sending jbi exchange", e);
            }
        }
    }

    protected Source getContent(MessageContext context, 
                                OutMessage message) throws XMLStreamException, IOException, XFireException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        XMLStreamWriter writer = getTransformer().getOutputFactory()
            .createXMLStreamWriter(outStream, message.getEncoding());
        MessageSerializer serializer = context.getOutMessage().getSerializer();
        if (serializer == null) {
            AbstractSoapBinding binding = (AbstractSoapBinding) context.getBinding();
            if (binding == null) {
                throw new XFireException("Couldn't find the binding!");
            }
            serializer = AbstractSoapBinding.getSerializer(binding.getStyle(), binding.getUse());
        }
        serializer.writeMessage(message, writer, context);
        writer.close();
        outStream.close();
        return new StreamSource(new ByteArrayInputStream(outStream.toByteArray()));
    }
    
}
