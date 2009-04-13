package org.apache.servicemix.common;

import java.io.StringReader;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class ResolveEndpointTest extends TestCase {
	
	public void testResolveEndpoint_noWhitespace() throws Exception {
        DocumentFragment fragment = createFragment();
        ServiceEndpoint endpoint = ResolvedEndpoint.resolveEndpoint(fragment, new QName("urn:test", "elementName"), new QName("urn:test", "serviceName"), "http:");
		assertNotNull("failed to parse wsa endpoint", endpoint);
	}
	
	public void testResolveEndpoint_hasWhitespace() throws Exception {
        DocumentFragment fragment = createFragment();
        // add some whitespace
        Node node = fragment.getOwnerDocument().createTextNode("\n");
        fragment.insertBefore(node, fragment.getFirstChild());
        ServiceEndpoint endpoint = ResolvedEndpoint.resolveEndpoint(fragment, new QName("urn:test", "elementName"), new QName("urn:test", "serviceName"), "http:");
		assertNotNull("failed to resolve an endpoint with whitespace", endpoint);
	}
	
	public void testResolveEndpoint_multipleElements() throws Exception {
        DocumentFragment fragment = createFragment();
        // add multiple elements
        Node clone = fragment.getFirstChild().cloneNode(true);
        fragment.appendChild(clone);
        ServiceEndpoint endpoint = ResolvedEndpoint.resolveEndpoint(fragment, new QName("urn:test", "elementName"), new QName("urn:test", "serviceName"), "http:");
		assertNull("should have rejected fragment since it had multiple elements", endpoint);
	}

	private DocumentFragment createFragment() throws Exception {
		String xml = "<wsa:EndpointReference xmlns:wsa='http://www.w3.org/2005/08/addressing'>" +
						"<wsa:Address>http://example.org/service/MyService</wsa:Address>" +
					 "</wsa:EndpointReference>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document epr = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        DocumentFragment fragment = epr.createDocumentFragment();
        fragment.appendChild(epr.getFirstChild());
		return fragment;
	}
}
