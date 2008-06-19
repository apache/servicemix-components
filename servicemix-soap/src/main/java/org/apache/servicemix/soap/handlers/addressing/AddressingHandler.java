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
package org.apache.servicemix.soap.handlers.addressing;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.jbi.util.WSAddressingConstants;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.handlers.AbstractHandler;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * 
 * @author Guillaume Nodet
 * @version $Revision: 1.5 $
 * @since 3.0
 * 
 * @org.apache.xbean.XBean element="ws-addressing"
 */
public class AddressingHandler extends AbstractHandler {

    protected final SourceTransformer sourceTransformer = new SourceTransformer();
    protected final IdGenerator idGenerator = new IdGenerator();
    
	public void onReceive(Context context) throws Exception {
		SoapMessage message = context.getInMessage();
    	String action = null;
    	String to = null;
    	String nsUri = null;
    	Map headers = message.getHeaders();
    	if (headers != null) {
	    	for (Iterator it = headers.keySet().iterator(); it.hasNext();) {
	    		QName qname = (QName) it.next();
	    		Object value = headers.get(qname);
                if (isWSANamespace(qname.getNamespaceURI())) {
	    			if (nsUri == null) {
	    				nsUri = qname.getNamespaceURI();
	    			} else if (!nsUri.equals(qname.getNamespaceURI())) {
	    				throw new SoapFault(SoapFault.SENDER, "Inconsistent use of wsa namespaces");
	    			}
		    		if (WSAddressingConstants.EL_ACTION.equals(qname.getLocalPart())) {
		    			action = getHeaderText(value);
		        		String[] parts = URIResolver.split3(action);
		        		context.setProperty(Context.INTERFACE, new QName(parts[0], parts[1]));
		        		context.setProperty(Context.OPERATION, new QName(parts[0], parts[2]));
		    		} else if (WSAddressingConstants.EL_TO.equals(qname.getLocalPart())) {
                        to = getHeaderText(value);
		        		String[] parts = URIResolver.split3(to);
		        		context.setProperty(Context.SERVICE, new QName(parts[0], parts[1]));
		        		context.setProperty(Context.ENDPOINT, parts[2]);
		    		} else {
		    			// TODO: what ?
		    		}
	    		}
	    	}
    	}
	}
    
    public void onReply(Context context) throws Exception {
        SoapMessage in = context.getInMessage();
        SoapMessage out = context.getOutMessage();
        Map headers = in.getHeaders();
        if (headers != null) {
            for (Iterator it = headers.keySet().iterator(); it.hasNext();) {
                QName qname = (QName) it.next();
                Object value = headers.get(qname);
                if (isWSANamespace(qname.getNamespaceURI())) {
                    if (WSAddressingConstants.EL_MESSAGE_ID.equals(qname.getLocalPart())) {
                        QName name = new QName(qname.getNamespaceURI(), WSAddressingConstants.EL_MESSAGE_ID, qname.getPrefix() != null ? qname.getPrefix() : WSAddressingConstants.WSA_PREFIX);
                        DocumentFragment df = createHeader(name, idGenerator.generateSanitizedId());
                        out.addHeader(name, df);
                        name = new QName(qname.getNamespaceURI(), WSAddressingConstants.EL_RELATES_TO, qname.getPrefix() != null ? qname.getPrefix() : WSAddressingConstants.WSA_PREFIX);
                        df = createHeader(name, getHeaderText(value));
                        out.addHeader(name, df);
                    }
                }
            }
        }
    }
    
    protected boolean isWSANamespace(String ns) {
        return WSAddressingConstants.WSA_NAMESPACE_200303.equals(ns) ||
               WSAddressingConstants.WSA_NAMESPACE_200403.equals(ns) ||
               WSAddressingConstants.WSA_NAMESPACE_200408.equals(ns) ||
               WSAddressingConstants.WSA_NAMESPACE_200508.equals(ns);
    }
    
    protected String getHeaderText(Object header) {
        Element el = (Element) ((DocumentFragment) header).getFirstChild();
        return DOMUtil.getElementText(el);
    }
    
    protected DocumentFragment createHeader(QName name, String value) throws Exception {
        Document doc = new SourceTransformer().createDocument();
        DocumentFragment df = doc.createDocumentFragment();
        Element el = doc.createElementNS(name.getNamespaceURI(), getQualifiedName(name));
        el.appendChild(doc.createTextNode(value));
        df.appendChild(el);
        return df;
    }
    
    /**
     * Gets the QName prefix.  If the QName has no set prefix, the specified default prefix will be used.
     */    
    protected String getPrefix(QName qname, String defaultPrefix) {
    	String prefix = qname.getPrefix();
    	if(null == prefix || "".equals(prefix)) {
    		prefix = defaultPrefix;
    	}
    	
    	return prefix;
    }
    
    protected String getQualifiedName(QName qname) {
    	String name = qname.getLocalPart();
    	
    	String prefix = qname.getPrefix();
    	if(null != prefix && (!"".equals(prefix))) {
    		name = prefix + ":" + name;
    	}
    	
    	return name;
    }
  
}
