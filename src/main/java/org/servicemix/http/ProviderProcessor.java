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
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.PostMethod;
import org.servicemix.common.ExchangeProcessor;
import org.servicemix.components.http.InvalidStatusResponseException;

public class ProviderProcessor implements ExchangeProcessor {

    protected HttpEndpoint endpoint;
    protected HostConfiguration host;
    protected HttpClientMarshaler marshaler;
    protected DeliveryChannel channel;
    
    public ProviderProcessor(HttpEndpoint endpoint) throws Exception {
        this.endpoint = endpoint;
        this.host = new HostConfiguration();
        this.host.setHost(new URI(endpoint.getLocationURI(), false));
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
            return;
        }
        PostMethod method = new PostMethod(endpoint.getLocationURI());
        marshaler.fromNMS(method, exchange, exchange.getMessage("in"));
        int response = getClient().executeMethod(host, method);
        if (response != HttpStatus.SC_OK) {
            throw new InvalidStatusResponseException(response);
        }
        if (exchange instanceof InOut) {
            NormalizedMessage msg = exchange.createMessage();
            marshaler.toNMS(msg, method);
            ((InOut) exchange).setOutMessage(msg);
            channel.sendSync(exchange);
        } else if (exchange instanceof InOptionalOut) {
            if (method.getResponseContentLength() == 0) {
                exchange.setStatus(ExchangeStatus.DONE);
                channel.send(exchange);
            } else {
                NormalizedMessage msg = exchange.createMessage();
                marshaler.toNMS(msg, method);
                ((InOptionalOut) exchange).setOutMessage(msg);
                channel.sendSync(exchange);
            }
        } else {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        }
    }

    public void start() throws Exception {
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        HttpLifeCycle lf =  (HttpLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
        if (lf.getConfiguration().isStreamingEnabled()) {
            this.marshaler = new HttpStreamingClientMarshaler();
        } else {
            this.marshaler = new HttpClientMarshaler();
        }
    }

    public void stop() throws Exception {
    }

    protected HttpClient getClient() {
        HttpLifeCycle lf =  (HttpLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
        return lf.getClient();
    }

}
