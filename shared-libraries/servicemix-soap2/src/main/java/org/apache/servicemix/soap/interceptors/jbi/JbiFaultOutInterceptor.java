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
package org.apache.servicemix.soap.interceptors.jbi;

import java.net.URI;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.core.AbstractInterceptor;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiFaultOutInterceptor extends AbstractInterceptor {

    public void handleMessage(Message message) {
        NormalizedMessage nm = message.getContent(NormalizedMessage.class);
        if (nm instanceof Fault) {
            SoapFault fault = createFault(nm);
            throw fault;
        }
    }

    private SoapFault createFault(NormalizedMessage nm) {
        Source src = nm.getContent();
        QName code = (QName) nm.getProperty(JbiConstants.SOAP_FAULT_CODE);
        QName subcode = (QName) nm.getProperty(JbiConstants.SOAP_FAULT_SUBCODE);
        String reason = (String) nm.getProperty(JbiConstants.SOAP_FAULT_REASON);
        URI node = (URI) nm.getProperty(JbiConstants.SOAP_FAULT_NODE);
        URI role = (URI) nm.getProperty(JbiConstants.SOAP_FAULT_ROLE);
        SoapFault fault = new SoapFault(code, subcode, reason, node, role, src);
        return fault;
    }

}
