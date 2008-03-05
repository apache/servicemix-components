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
import org.apache.servicemix.common.ExchangeProcessor;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.script.ScriptExchangeHelper;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

class GroovyExchangeProcessor implements ExchangeProcessor {

    @Property ScriptExchangeHelper exchangeHelper;

	def void start() {
	    println "Starting GroovyConsumer";
	}

    def void process(MessageExchange exchange) {
        if (exchange.getRole() == Role.PROVIDER) {
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                println "Receiving provider / done";
                MessageExchange me = exchange.getProperty("originalExchange");
                me.setStatus(ExchangeStatus.DONE);
                exchangeHelper.getChannel().send(me);
            } else {
                println "Receiving provider / active";
                MessageExchange me = exchangeHelper.getExchangeFactory().createInOutExchange();
                me.setService(new QName("urn:test", "echo"));
                NormalizedMessage inMsg = me.createMessage();
                inMsg.setContent(exchange.getInMessage().getContent());
                me.setInMessage(inMsg);
                me.setProperty("originalExchange", exchange);
                exchangeHelper.getChannel().send(me);
            }
        } else {
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                println "Receiving consumer / done";
            } else {
                println "Receiving consumer / active";
                MessageExchange me = exchange.getProperty("originalExchange");
                NormalizedMessage outMsg = me.createMessage();
                outMsg.setContent(exchange.getOutMessage().getContent());
                me.setOutMessage(outMsg);
                me.setProperty("originalExchange", exchange);
                exchangeHelper.getChannel().send(me);
            }
        }
    }
    
    def void stop() {
    	println "Stopping GroovyConsumer";
    }
}
