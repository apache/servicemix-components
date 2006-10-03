package org.apache.servicemix.script;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

import org.apache.servicemix.common.ExchangeProcessor;

public abstract class ScriptExchangeProcessor implements ExchangeProcessor {

	private ScriptExchangeProcessorEndpoint endpoint;

	protected void setScriptExchangeProcessorEndpoint(
			ScriptExchangeProcessorEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public void fail(MessageExchange exchange, String error)
			throws MessagingException {
		endpoint.fail(exchange, new Exception(error));
	}

	public void fail(MessageExchange exchange, Exception exception)
			throws MessagingException {
		endpoint.fail(exchange, exception);
	}

	public void done(MessageExchange exchange) throws MessagingException {
		endpoint.done(exchange);
	}
	
	public void send(MessageExchange exchange) throws MessagingException {
		endpoint.send(exchange);
	}
}
