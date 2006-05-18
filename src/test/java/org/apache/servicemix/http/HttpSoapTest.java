/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.http;

import java.net.URI;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.components.util.TransformComponentSupport;
import org.apache.servicemix.jbi.FaultException;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.soap.marshalers.SoapMarshaler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.wsdl.util.xml.DOMUtils;

public class HttpSoapTest extends TestCase {

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testFaultOnParse() throws Exception {
        HttpSpringComponent http = new HttpSpringComponent();
        HttpEndpoint ep = new HttpEndpoint();
        ep.setService(new QName("urn:test", "echo"));
        ep.setEndpoint("echo");
        ep.setLocationURI("http://localhost:8192/ep1/");
        ep.setRoleAsString("consumer");
        ep.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep.setSoap(true);
        http.setEndpoints(new HttpEndpoint[] { ep });
        container.activateComponent(http, "http");
        container.start();
        
        PostMethod method = new PostMethod("http://localhost:8192/ep1/");
        method.setRequestEntity(new StringRequestEntity("<hello>world</hello>"));
        int state = new HttpClient().executeMethod(method);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, state);
        FileUtil.copyInputStream(method.getResponseBodyAsStream(), System.out);
    }

    public void testSoap() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        container.activateComponent(echo, "echo");
        
        HttpSpringComponent http = new HttpSpringComponent();
        
        HttpEndpoint ep1 = new HttpEndpoint();
        ep1.setService(new QName("urn:test", "echo"));
        ep1.setEndpoint("echo");
        ep1.setLocationURI("http://localhost:8192/ep1/");
        ep1.setRoleAsString("consumer");
        ep1.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep1.setSoap(true);
        
        HttpEndpoint ep2 = new HttpEndpoint();
        ep2.setService(new QName("urn:test", "s2"));
        ep2.setEndpoint("ep2");
        ep2.setLocationURI("http://localhost:8192/ep1/");
        ep2.setRoleAsString("provider");
        ep2.setSoap(true);
        
        http.setEndpoints(new HttpEndpoint[] { ep1, ep2 });
        
        container.activateComponent(http, "http");
        
        container.start();
        
        ServiceMixClient client = new DefaultServiceMixClient(container);
        client.request(new ServiceNameEndpointResolver(new QName("urn:test", "s2")), null, null, 
                       new StreamSource(getClass().getResourceAsStream("soap-request.xml")));
        
    }

    public void testSoapFault12() throws Exception {
        TransformComponentSupport echo = new TransformComponentSupport() {
            protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws MessagingException {
                Fault f = exchange.createFault();
                f.setContent(new StringSource("<hello xmlns='myuri'>this is a fault</hello>"));
                throw new FaultException(null, exchange, f);
            }
        };
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        container.activateComponent(echo, "echo");
        
        HttpEndpoint ep1 = new HttpEndpoint();
        ep1.setService(new QName("urn:test", "http"));
        ep1.setEndpoint("ep1");
        ep1.setTargetService(new QName("urn:test", "echo"));
        ep1.setTargetEndpoint("echo");
        ep1.setLocationURI("http://localhost:8193/ep1/");
        ep1.setRoleAsString("consumer");
        ep1.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep1.setSoap(true);
        
        HttpEndpoint ep2 = new HttpEndpoint();
        ep2.setService(new QName("urn:test", "http"));
        ep2.setEndpoint("ep2");
        ep2.setTargetService(new QName("urn:test", "http"));
        ep2.setTargetEndpoint("ep3");
        ep2.setLocationURI("http://localhost:8193/ep2/");
        ep2.setRoleAsString("consumer");
        ep2.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep2.setSoap(true);
        
        HttpEndpoint ep3 = new HttpEndpoint();
        ep3.setService(new QName("urn:test", "http"));
        ep3.setEndpoint("ep3");
        ep3.setLocationURI("http://localhost:8193/ep1/");
        ep3.setRoleAsString("provider");
        ep3.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep3.setSoap(true);
        
        HttpSpringComponent http = new HttpSpringComponent();
        http.setEndpoints(new HttpEndpoint[] { ep1, ep2, ep3 });
        container.activateComponent(http, "http1");
        
        container.start();
        
        PostMethod method = new PostMethod("http://localhost:8193/ep2/");
        method.setRequestEntity(new InputStreamRequestEntity(getClass().getResourceAsStream("soap-request-12.xml")));
        int state = new HttpClient().executeMethod(method);
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, state);
        SourceTransformer st = new SourceTransformer();
        Node node = st.toDOMNode(new StreamSource(method.getResponseBodyAsStream()));
        System.err.println(st.toString(node));

        Element e = ((Document) node).getDocumentElement();
        assertEquals(new QName(SoapMarshaler.SOAP_12_URI, SoapMarshaler.ENVELOPE), DOMUtil.getQName(e));
        e = DOMUtil.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_12_URI, SoapMarshaler.BODY), DOMUtil.getQName(e));
        e = DOMUtil.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_12_URI, SoapMarshaler.FAULT), DOMUtil.getQName(e));
    }
    
    public void testSoapFault11() throws Exception {
        TransformComponentSupport echo = new TransformComponentSupport() {
            protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws MessagingException {
                Fault f = exchange.createFault();
                f.setContent(new StringSource("<hello xmlns='myuri'>this is a fault</hello>"));
                throw new FaultException(null, exchange, f);
            }
        };
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        container.activateComponent(echo, "echo");
        
        HttpEndpoint ep1 = new HttpEndpoint();
        ep1.setService(new QName("urn:test", "http"));
        ep1.setEndpoint("ep1");
        ep1.setTargetService(new QName("urn:test", "echo"));
        ep1.setTargetEndpoint("echo");
        ep1.setLocationURI("http://localhost:8194/ep1/");
        ep1.setRoleAsString("consumer");
        ep1.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep1.setSoap(true);
        
        HttpEndpoint ep2 = new HttpEndpoint();
        ep2.setService(new QName("urn:test", "http"));
        ep2.setEndpoint("ep2");
        ep2.setTargetService(new QName("urn:test", "http"));
        ep2.setTargetEndpoint("ep3");
        ep2.setLocationURI("http://localhost:8194/ep2/");
        ep2.setRoleAsString("consumer");
        ep2.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep2.setSoap(true);
        
        HttpEndpoint ep3 = new HttpEndpoint();
        ep3.setService(new QName("urn:test", "http"));
        ep3.setEndpoint("ep3");
        ep3.setLocationURI("http://localhost:8194/ep1/");
        ep3.setRoleAsString("provider");
        ep3.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep3.setSoap(true);
        
        HttpSpringComponent http = new HttpSpringComponent();
        http.setEndpoints(new HttpEndpoint[] { ep1, ep2, ep3 });
        container.activateComponent(http, "http1");
        
        container.start();
        
        PostMethod method = new PostMethod("http://localhost:8194/ep2/");
        method.setRequestEntity(new InputStreamRequestEntity(getClass().getResourceAsStream("soap-request.xml")));
        int state = new HttpClient().executeMethod(method);
        String str = method.getResponseBodyAsString();
        System.err.println(str);
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, state);
        SourceTransformer st = new SourceTransformer();
        Node node = st.toDOMNode(new StringSource(str));

        Element e = ((Document) node).getDocumentElement();
        assertEquals(new QName(SoapMarshaler.SOAP_11_URI, SoapMarshaler.ENVELOPE), DOMUtil.getQName(e));
        e = DOMUtils.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_11_URI, SoapMarshaler.BODY), DOMUtil.getQName(e));
        e = DOMUtils.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_11_URI, SoapMarshaler.FAULT), DOMUtil.getQName(e));
    }

}
