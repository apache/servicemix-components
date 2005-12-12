/** 
 * 
 * Copyright 2005 Protique Ltd
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
package org.servicemix.http;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.http.HttpContext;
import org.servicemix.common.ExchangeProcessor;

public class ConsumerProcessor implements ExchangeProcessor, HttpProcessor {

    protected HttpEndpoint endpoint;
    protected HttpContext context;
    protected HttpMarshaler marshaler;
    protected DeliveryChannel channel;
    
    public ConsumerProcessor(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
        this.marshaler = new HttpMarshaler();
    }

    public void process(MessageExchange exchange) throws Exception {
        // TODO
        System.out.println(exchange);
    }

    public void start() throws Exception {
        String url = endpoint.getLocationURI();
        context = getServerManager().createContext(url, this);
        context.start();
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
    }

    public void stop() throws Exception {
        context.stop();
        getServerManager().remove(context);
        
    }
    
    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Create the exchange
        MessageExchange exchange = channel.createExchangeFactory().createExchange(endpoint.getDefaultMep());
        exchange.setService(endpoint.getService());
        exchange.setInterfaceName(endpoint.getInterfaceName());
        NormalizedMessage msg = exchange.createMessage();
        exchange.setMessage(msg, "in");
        marshaler.toNMS(exchange, msg, request);
        // Send exchange
        boolean result = channel.sendSync(exchange);
        // Check result
        if (result == false) {
            throw new Exception("Exchanged timed out");
        }
        if (exchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
            if (exchange.getError() != null) {
                throw new Exception(exchange.getError());
            } else if (exchange.getFault() != null) {
                // TODO: retrieve fault
                throw new Exception("Fault received");
            } else {
                throw new Exception("Unkown Error");
            }
        } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            NormalizedMessage outMsg = exchange.getMessage("out");
            if (outMsg != null) {
                marshaler.toResponse(exchange, outMsg, response);
            }
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        }
    }
    
    protected ServerManager getServerManager() {
        HttpLifeCycle lf =  (HttpLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
        return lf.getServer();
    }

}
