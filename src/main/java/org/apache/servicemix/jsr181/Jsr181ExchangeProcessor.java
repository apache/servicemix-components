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
package org.apache.servicemix.jsr181;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.jbi.jaxp.StAXSourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jsr181.xfire.JbiTransport;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.attachments.Attachment;
import org.codehaus.xfire.attachments.Attachments;
import org.codehaus.xfire.attachments.JavaMailAttachments;
import org.codehaus.xfire.attachments.SimpleAttachment;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.Transport;

public class Jsr181ExchangeProcessor implements ExchangeProcessor {

    public static final String SOAP_FAULT_CODE = "org.apache.servicemix.soap.fault.code";
    public static final String SOAP_FAULT_SUBCODE = "org.apache.servicemix.soap.fault.subcode";
    public static final String SOAP_FAULT_REASON = "org.apache.servicemix.soap.fault.reason";
    public static final String SOAP_FAULT_NODE = "org.apache.servicemix.soap.fault.node";
    public static final String SOAP_FAULT_ROLE = "org.apache.servicemix.soap.fault.role";
    
    protected DeliveryChannel channel;
    protected Jsr181Endpoint endpoint;
    protected StAXSourceTransformer transformer;
    
    public Jsr181ExchangeProcessor(Jsr181Endpoint endpoint) {
        this.endpoint = endpoint;
        this.transformer = new StAXSourceTransformer();
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }

        // TODO: clean this code
        XFire xfire = endpoint.getXFire();
        Service service = endpoint.getXFireService();
        Transport t = xfire.getTransportManager().getTransport(JbiTransport.JBI_BINDING);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Channel c = t.createChannel();
        MessageContext ctx = new MessageContext();
        ctx.setXFire(xfire);
        ctx.setService(service);
        ctx.setProperty(Channel.BACKCHANNEL_URI, out);
        ctx.setExchange(new org.codehaus.xfire.exchange.MessageExchange(ctx));
        InMessage msg = new InMessage();
        ctx.getExchange().setInMessage(msg);
        if (exchange.getOperation() != null) {
            OperationInfo op = service.getServiceInfo().getOperation(exchange.getOperation().getLocalPart());
            if (op != null) {
                ctx.getExchange().setOperation(op);
            }
        }
        ctx.setCurrentMessage(msg);
        NormalizedMessage in = exchange.getMessage("in");
        msg.setXMLStreamReader(getXMLStreamReader(in.getContent()));
        if (in.getAttachmentNames() != null && in.getAttachmentNames().size() > 0) {
            JavaMailAttachments attachments = new JavaMailAttachments();
            for (Iterator it = in.getAttachmentNames().iterator(); it.hasNext();) {
                String name = (String) it.next();
                DataHandler dh = in.getAttachment(name);
                attachments.addPart(new SimpleAttachment(name, dh));
            }
            msg.setAttachments(attachments);
        }
        JBIContext.setMessageExchange(exchange);
        if (isInAndOut(exchange)) {
            
        }
        try {
            c.receive(ctx, msg);
        } finally {
            JBIContext.setMessageExchange(null);
        }
        c.close();
        
        // Set response or DONE status
        if (isInAndOut(exchange)) {
            if (ctx.getExchange().hasFaultMessage() && ctx.getExchange().getFaultMessage().getBody() != null) {
                String charSet = ctx.getExchange().getFaultMessage().getEncoding();
                Fault fault = exchange.getFault();
                if (fault == null) {
                    fault = exchange.createFault();
                    exchange.setFault(fault);
                }
                fault.setContent(new StringSource(out.toString(charSet)));
                XFireFault xFault = (XFireFault) ctx.getExchange().getFaultMessage().getBody();
                fault.setProperty(SOAP_FAULT_CODE, xFault.getFaultCode());
                fault.setProperty(SOAP_FAULT_REASON, xFault.getReason());
                fault.setProperty(SOAP_FAULT_ROLE, xFault.getRole());
                fault.setProperty(SOAP_FAULT_SUBCODE, xFault.getSubCode());
            } else {
                String charSet = ctx.getOutMessage().getEncoding();
                NormalizedMessage outMsg = exchange.getMessage("out");
                if (outMsg == null) {
                    outMsg = exchange.createMessage();
                    exchange.setMessage(outMsg, "out");
                }
                Attachments attachments = ctx.getCurrentMessage().getAttachments();
                if (attachments != null) {
                    for (Iterator it = attachments.getParts(); it.hasNext();) {
                        Attachment att = (Attachment) it.next();
                        outMsg.addAttachment(att.getId(), att.getDataHandler());
                    }
                }
                outMsg.setContent(new StringSource(out.toString(charSet)));
            }
            if (exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC))) {
                channel.sendSync(exchange);
            } else {
                channel.send(exchange);
            }
        } else {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        }
    }

    public void start() throws Exception {
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
    }

    public void stop() throws Exception {
    }

    protected XMLStreamReader getXMLStreamReader(Source source) throws TransformerException, XMLStreamException {
        return transformer.toXMLStreamReader(source);
    }
    
    protected boolean isInAndOut(MessageExchange exchange) {
        return exchange instanceof InOut || exchange instanceof InOptionalOut;
    }

}
