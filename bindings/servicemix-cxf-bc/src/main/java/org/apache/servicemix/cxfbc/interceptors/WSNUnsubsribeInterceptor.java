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
package org.apache.servicemix.cxfbc.interceptors;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.servicemix.cxfbc.CxfBcComponent;
import org.apache.servicemix.cxfbc.CxfBcConsumer;
import org.apache.servicemix.soap.util.stax.StaxSource;

public class WSNUnsubsribeInterceptor extends AbstractSoapInterceptor {

    public WSNUnsubsribeInterceptor() {
        super(Phase.PRE_INVOKE);
        addAfter(JbiInWsdl1Interceptor.class.getName());
        addBefore(JbiInInterceptor.class.getName());
    }
    
    public void handleMessage(SoapMessage message) throws Fault {
        String address = null;
        address = getAddress(message);
        address = address.substring("http://servicemix.org/wsnotification/Subscription/".length());
        System.out.println("the address is =============" + address);
        message.setContextualProperty(CxfBcConsumer.WSN_UNSUBSCRIBE_ADDRESS, address);
    }
    
    private String getAddress(SoapMessage message) throws Fault {
        try {
            //as we need read the incoming message to determine the target endpoint
            //ensure the Source is re-readable;
            Node node = XMLUtils.fromSource(message.getContent(Source.class));
            XMLUtils.printDOM("======the node before address is", node);
            message.setContent(Source.class, new DOMSource(node));
            String ret = DOMUtils.findAllElementsByTagNameNS(
                                 (Element)node.getFirstChild(), "http://docs.oasis-open.org/wsrf/bf-2", "Address").get(0).getTextContent();
            XMLUtils.printDOM("======the node before address is", node);
            return ret;
           
        } catch (Exception e) {
            throw new Fault(e);
        }
    }


}
