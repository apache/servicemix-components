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
package org.apache.servicemix.cxfbc.ws.addressing;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.JAXWSAConstants;

/**
 * @author vladislav.chernyak
 * 
 */
public class AddressingToVerifier extends AbstractPhaseInterceptor<Message> {

    String expectedTo;

    public AddressingToVerifier() {
        super(Phase.POST_LOGICAL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    public void handleMessage(Message message) throws Fault {
        AddressingPropertiesImpl maps = (AddressingPropertiesImpl) message
                .get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND);

        if (!maps.getTo().getValue().equals(expectedTo)) {
            throw new Fault(new Exception("Addressing To header \""
                    + maps.getTo().getValue() + "\" does not match expected \""
                    + expectedTo + "\"."));
        }
    }

    /**
     * @return the expectedTo
     */
    public String getExpectedTo() {
        return expectedTo;
    }

    /**
     * @param expectedTo
     *            the expectedTo to set
     */
    public void setExpectedTo(String expectedTo) {
        this.expectedTo = expectedTo;
    }
}
