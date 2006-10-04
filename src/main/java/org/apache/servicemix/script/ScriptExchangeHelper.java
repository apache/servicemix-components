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
package org.apache.servicemix.script;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * This helper object can be injected into your scripts to allow to quick access
 * to basic JBI operations
 * 
 * @org.apache.xbean.XBean element="exchangeHelper" description="ServiceMix
 *                         Scripting Helper"
 */
public class ScriptExchangeHelper {

	private ScriptExchangeProcessorEndpoint endpoint;

	public ScriptExchangeHelper(ScriptExchangeProcessorEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public void done(MessageExchange exchange) throws MessagingException {
		endpoint.done(exchange);
	}

	public void fail(MessageExchange exchange, Exception exception)
			throws MessagingException {
		endpoint.fail(exchange, exception);
	}

	public void send(MessageExchange exchange) throws MessagingException {
		endpoint.send(exchange);
	}
}
