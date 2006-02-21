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

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.apache.servicemix.jbi.util.FileUtil;

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
    
    public void testFault() throws Exception {
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
        assertEquals(400, state);
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

}
