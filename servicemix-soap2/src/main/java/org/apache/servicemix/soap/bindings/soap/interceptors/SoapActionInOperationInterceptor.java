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
package org.apache.servicemix.soap.bindings.soap.interceptors;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapConstants;
import org.apache.servicemix.soap.bindings.soap.model.SoapBinding;
import org.apache.servicemix.soap.bindings.soap.model.SoapOperation;
import org.apache.servicemix.soap.core.AbstractInterceptor;

public class SoapActionInOperationInterceptor extends AbstractInterceptor {

    public void handleMessage(Message message) {
        if (message.get(Operation.class) != null) {
            return;
        }
        String soapAction = message.getTransportHeaders().get(SoapConstants.SOAP_ACTION_HEADER);
        if (soapAction != null) {
            soapAction = soapAction.trim();
            if (soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
                soapAction = soapAction.substring(1, soapAction.length() - 1);
            }
            SoapBinding<?> binding = (SoapBinding<?>) message.get(Binding.class);
            List<SoapOperation<?>> matching = new ArrayList<SoapOperation<?>>();
            for (SoapOperation<?> operation : binding.getOperations()) {
                if (soapAction.equals(operation.getSoapAction())) {
                    matching.add(operation);
                }
            }
            if (matching.size() == 1) {
                message.put(Operation.class, matching.get(0));
            }
        }
    }

}
