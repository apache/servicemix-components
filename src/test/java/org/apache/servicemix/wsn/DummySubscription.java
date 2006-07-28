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
package org.apache.servicemix.wsn;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.servicemix.wsn.jaxws.PauseFailedFault;
import org.apache.servicemix.wsn.jaxws.ResumeFailedFault;
import org.apache.servicemix.wsn.jaxws.SubscribeCreationFailedFault;
import org.apache.servicemix.wsn.jaxws.UnacceptableTerminationTimeFault;

public class DummySubscription extends AbstractSubscription {

	public DummySubscription(String name) {
		super(name);
	}

	@Override
	protected void start() throws SubscribeCreationFailedFault {
	}

	@Override
	protected void pause() throws PauseFailedFault {
	}

	@Override
	protected void resume() throws ResumeFailedFault {
	}

	@Override
	protected void renew(XMLGregorianCalendar terminationTime) throws UnacceptableTerminationTimeFault {
	}
	
}