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
package org.apache.servicemix.exec;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.exec.marshaler.DefaultExecMarshaler;
import org.apache.servicemix.exec.marshaler.ExecMarshalerSupport;
import org.apache.servicemix.exec.utils.ExecUtils;
import org.apache.servicemix.exec.utils.ExecutionData;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.wsdl.WSDLUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Represents an exec endpoint.
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="endpoint"
 */
public class ExecEndpoint extends ProviderEndpoint {
    
    public final static String DEFAULT_WSDL = "servicemix-exec.wsdl"; // the default abstract endpoint WSDL

	private String command; // the command can be static (define in the descriptor) or provided in the incoming message
	private Resource wsdl; // the abstract WSDL describing the endpoint behavior
	private ExecMarshalerSupport marshaler = new DefaultExecMarshaler(); // the default exec marshaler

	public String getCommand() {
		return command;
	}

	/**
	 * <p>
	 * This attribute specifies the default command to use if no is provided in
	 * the incoming message.
	 * </p>
	 * <i>&nbsp;&nbsp;&nbsp;The default value is <code>null</code>.
	 * 
	 * @param command
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	public ExecMarshalerSupport getMarshaler() {
		return marshaler;
	}
	
	public Resource getWsdl() {
	    return this.wsdl;
	}
	
	/**
	 * <p>
	 * This attribute specifies the abstract WSDL describing the endpoint behavior.
	 * </p>
	 * 
	 * @param wsdl the WSDL <code>Resource</code>.
	 */
	public void setWsdl(Resource wsdl) {
	    this.wsdl = wsdl;
	}

	/**
	 * <p>
	 * With this method you can specify a marshaler class which provides the
	 * logic for converting a message into a execution command. This class has
	 * to implement the interface class <code>ExecMarshalerSupport</code>. If
	 * you don't specify a marshaler, the <code>DefaultExecMarshaler</code> will
	 * be used.
	 * </p>
	 * 
	 * @param marshaler
	 *            a <code>ExecMarshalerSupport</code> class representing the
	 *            marshaler.
	 */
	public void setMarshaler(ExecMarshalerSupport marshaler) {
		this.marshaler = marshaler;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#validate()
	 */
	@Override
	public void validate() throws DeploymentException {
	    try {
	        if (wsdl != null) {
	            // the user provides a WSDL
	            description = DomUtil.parse(wsdl.getInputStream());
	            definition = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
	        } else {
	            // load the default abstract WSDL
	            description = DomUtil.parse(new ClassPathResource(DEFAULT_WSDL).getInputStream());
	            definition = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
	        }
	    } catch (Exception e) {
	        throw new DeploymentException("Can't load the WSDL.", e);
	    }
	    
	    // TODO define the WSDL for the marshaler
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax
	 * .jbi.messaging.MessageExchange)
	 */
	@Override
	public void process(MessageExchange exchange) throws Exception {
		// The component acts as a provider, this means that another component
		// has requested our service
		// As this exchange is active, this is either an in or a fault (out are
		// sent by this component)
		if (exchange.getStatus() == ExchangeStatus.DONE) {
			// exchange is finished
			return;
		} else if (exchange.getStatus() == ExchangeStatus.ERROR) {
			// exchange has been aborted with an exception
			return;
		} else {
			// exchange is active
			this.handleProviderExchange(exchange);
		}
	}

	/**
	 * <p>
	 * Handles on the message exchange (provider role).
	 * </p>
	 * 
	 * @param exchange
	 *            the <code>MessageExchange</code>.
	 */
	protected void handleProviderExchange(MessageExchange exchange)
			throws Exception {
		// fault message
		if (exchange.getFault() != null) {
			done(exchange);
			return;
		} else {
			String exec = null;

			// try to extract the command from the in message content
			if (exchange.getMessage("in") != null) {
				// in message presents
				if (logger.isDebugEnabled()) {
					logger.debug("Received exchange: " + exchange);
				}
				// gets the in message
				NormalizedMessage in = exchange.getMessage("in");
				logger.debug("In message: " + in);
				logger.debug("In message content: " + in.getContent());
				// parses the in message and get the execution command
				SourceTransformer transformer = new SourceTransformer();
				exec = marshaler.constructExecCommand(transformer.toDOMDocument(in));
			}

			// fall back to static command if extracted is null or empty
			if (exec == null || exec.trim().length() < 1) {
				exec = command;
			}

			// if even the fall back is empty then we can't do anything
			if (exec == null || exec.trim().length() < 1) {
				throw new MessagingException("No command to execute.");
			}

			// execute the command
			ExecutionData resultData = ExecUtils.execute(exec);

			// prepare the output
			String result = marshaler.formatExecutionResult(resultData);

			if (exchange instanceof InOut) {
				// pushes the execution output in out message
				NormalizedMessage out = exchange.createMessage();
				out.setContent(new StringSource(result));
				exchange.setMessage(out, "out");
				send(exchange);
			} else {
				done(exchange);
			}
		}
	}
}
