/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.jsr181;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.RobustInOutExchange;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.Transport;
import org.servicemix.common.ExchangeProcessor;
import org.servicemix.jbi.jaxp.BytesSource;
import org.servicemix.jsr181.xfire.JbiTransport;

public class Jsr181ExchangeProcessor implements ExchangeProcessor {

    protected DeliveryChannel channel;
    protected Jsr181Endpoint endpoint;
    
    public Jsr181ExchangeProcessor(Jsr181Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
            return;
        }

        // TODO: fault should not be serialized as soap
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
        ctx.setExchange(new RobustInOutExchange(ctx));
        InMessage msg = new InMessage();
        ctx.getExchange().setInMessage(msg);
        NormalizedMessage in = exchange.getMessage("in");
        msg.setXMLStreamReader(getXMLStreamReader(in.getContent()));
        c.receive(ctx, msg);
        
        // Set response or DONE status
        if (isInAndOut(exchange)) {
            if (ctx.getExchange().hasFaultMessage() && ctx.getExchange().getFaultMessage().getBody() != null) {
                Fault fault = exchange.createFault();
                fault.setContent(new BytesSource(out.toByteArray()));
                exchange.setFault(fault);
                exchange.setStatus(ExchangeStatus.ERROR);
            } else {
                NormalizedMessage outMsg = exchange.createMessage();
                outMsg.setContent(new BytesSource(out.toByteArray()));
                exchange.setMessage(outMsg, "out");
            }
        } else {
            exchange.setStatus(ExchangeStatus.DONE);
        }
        channel.send(exchange);
    }

    public void start() throws Exception {
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
    }

    public void stop() throws Exception {
    }

    protected XMLStreamReader getXMLStreamReader(Source source) throws TransformerException, XMLStreamException {
        try {
            return XMLInputFactory.newInstance().createXMLStreamReader(source);
        } catch (Exception e) {
            // ignore, as this method is not mandatory in stax
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(source, new StreamResult(buffer));
        return XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(buffer.toByteArray()));
    }
    
    protected boolean isInAndOut(MessageExchange exchange) {
        return exchange instanceof InOut || exchange instanceof InOptionalOut;
    }

}
