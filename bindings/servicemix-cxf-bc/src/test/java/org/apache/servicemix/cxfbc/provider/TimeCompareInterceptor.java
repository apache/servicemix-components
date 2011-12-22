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

package org.apache.servicemix.cxfbc.provider;

import org.apache.servicemix.cxf.binding.jbi.JBIFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class TimeCompareInterceptor extends AbstractPhaseInterceptor {
   
	private long currentThreadId;
	private long before;
	private boolean firstInvocation = true;
	
	public TimeCompareInterceptor() {
		super(Phase.PRE_STREAM);
	}




	
	
	public void handleMessage(Message message) throws Fault {
		if (firstInvocation) {
			firstInvocation = false;
			currentThreadId = Thread.currentThread().getId();
			before = System.currentTimeMillis();
		} else {
			if (Thread.currentThread().getId() != currentThreadId) {
				//ensure only one thread is used for the cxf bc provider
				throw new JBIFault("not invoked by the same thread");
			}
			if (!message.getExchange().isSynchronous()) {
				if (System.currentTimeMillis() - before > 10000) {
					//it's asynchronous way, so should use nonblcok invocation
					throw new JBIFault("second invocation shouldn't wait the first invocation return");
					
				}
			} else {
				if (System.currentTimeMillis() - before < 8000) {
					//it's synchronous way, so should use blcok invocation
					throw new JBIFault("second invocation should wait until the first invocation return");
					
				}
			}
		}
	}

}
