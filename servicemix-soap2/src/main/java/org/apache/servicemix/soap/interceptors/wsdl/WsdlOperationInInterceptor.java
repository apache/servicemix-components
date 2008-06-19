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
package org.apache.servicemix.soap.interceptors.wsdl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.core.AbstractInterceptor;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class WsdlOperationInInterceptor extends AbstractInterceptor {

    public void handleMessage(Message message) {
        if (message.get(Operation.class) != null) {
            return;
        }
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);
        if (reader == null || reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            return;
        }
        Binding<?> binding = message.get(Binding.class);
        List<Operation> matching = new ArrayList<Operation>();
        QName name = reader.getName();
        for (Operation<?> oper : binding.getOperations()) {
            if (name.equals(oper.getInput().getElementName())) {
                matching.add(oper);
            }
        }
        if (matching.size() == 1) {
            Operation op = matching.get(0);
            message.put(Operation.class, op);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("No matching operation found for element: ");
            sb.append(name);
            sb.append("\n");
            sb.append("Expected one of: \n");
            for (Operation<?> oper : binding.getOperations()) {
                sb.append("  - ");
                sb.append(oper.getInput().getElementName());
                sb.append("\n");
            }
            throw new SoapFault(SoapFault.SENDER, sb.toString());
        }
    }

}
