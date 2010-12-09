package org.apache.servicemix.soap.ws.addressing;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Document;

import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.bindings.soap.SoapInterceptor;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.common.util.WSAddressingConstants;
import org.apache.servicemix.common.util.DOMUtil;

public abstract class AbstractWsAddressingInterceptor extends AbstractInterceptor implements SoapInterceptor {

    private final WsAddressingPolicy policy;
    private final boolean server;

    protected AbstractWsAddressingInterceptor(WsAddressingPolicy policy, boolean server) {
        this.policy = policy;
        this.server = server;
    }

    public WsAddressingPolicy getPolicy() {
        return policy;
    }

    public boolean isServer() {
        return server;
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
        Document doc = DomUtil.createDocument();
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
