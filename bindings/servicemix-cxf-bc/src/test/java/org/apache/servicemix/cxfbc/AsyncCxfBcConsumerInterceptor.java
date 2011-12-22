/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.cxfbc;

import org.apache.servicemix.cxf.binding.jbi.JBIFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.servicemix.cxfbc.CxfBcConsumer.JbiPostInvokerInterceptor;

public class AsyncCxfBcConsumerInterceptor extends AbstractPhaseInterceptor {
   
	private long currentThreadId;
	private boolean firstInvocation = true;
	
	public AsyncCxfBcConsumerInterceptor() {
		super(Phase.POST_INVOKE);
		addAfter(JbiPostInvokerInterceptor.class.getName());
	}

	public void handleMessage(Message message) throws Fault {
		if (firstInvocation) {
			firstInvocation = false;
			currentThreadId = Thread.currentThread().getId();
		} else {
			if (Thread.currentThread().getId() != currentThreadId) {
				//ensure only one thread is used for the cxf bc consumer
				throw new JBIFault("not invoked by the same thread");
			}
		}
	}

}
