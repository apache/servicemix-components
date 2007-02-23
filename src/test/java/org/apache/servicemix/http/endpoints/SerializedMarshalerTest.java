package org.apache.servicemix.http.endpoints;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.jbi.component.ComponentContext;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
//import org.springframework.mock.web.MockHttpServletRequest;
//import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.support.RemoteInvocationResult;

public class SerializedMarshalerTest extends TestCase {
	
	protected JBIContainer container; 
	protected ComponentContext context; 
	
	public void setUp() throws Exception {		
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

    /**
     * Test the SerializedMarshaler using only SMX. I don't think this is going
     * to work. 
     */
//	public void testCreateExchange() throws Exception{
//		HttpConsumerEndpoint ep = new HttpConsumerEndpoint();
//		ep.setService(new QName("urn:httpconsumer", "HttpConsumer"));
//		ep.setEndpoint("HttpConsumer");
//		ep.setLocationURI("http://localhost:8192/service");
//		ep.setTargetService(new QName("http://http.servicemix.org/Test", "ConsumerInOut"));
//		
//		SerializedMarshaler marshaler = new SerializedMarshaler();
//		ep.setMarshaler(marshaler);
//		
//		HttpComponent component = new HttpComponent();
//        component.setEndpoints(new HttpEndpointType[] { ep });
//		container.activateComponent(component, "HttpConsumer");
//		
//		EchoComponent echo = new EchoComponent();
//        ActivationSpec asReceiver = new ActivationSpec("echo", echo);
//        asReceiver.setService(new QName("http://http.servicemix.org/Test", "ConsumerInOut"));
//        container.activateComponent(asReceiver);
//		
//		container.start();
//		
//		DefaultServiceMixClient client = new DefaultServiceMixClient(container);
//        InOut inout = client.createInOutExchange();
//        inout.setService(new QName("urn:httpconsumer", "HttpConsumer"));
//        DocumentFragment epr = URIResolver.createWSAEPR("http://localhost:8192/service");
//        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
//        inout.setEndpoint(se);
//        inout.getInMessage().setContent(
//                new StreamSource(new ByteArrayInputStream(createRequestMessage())));
//        
//        long t0 = System.currentTimeMillis();
//        client.sendSync(inout);
//        long t1 = System.currentTimeMillis();
//        System.out.println("%%%%%%%%%%%%%%% Exchange: " + inout);
//        System.out.println("Fault: " + inout.getFault());
//        assertTrue(inout.getStatus() == ExchangeStatus.ACTIVE);
//        
//        System.err.println("Executed in " + (t1 - t0) + "ms");
//
//        assertNotNull(inout.getOutMessage());
//        assertNotNull(inout.getOutMessage().getContent());
//        
//        SourceTransformer sourceTransformer = new SourceTransformer();
//        String reply = sourceTransformer.toString(inout.getOutMessage().getContent());
//        
//        String inputMesage = sourceTransformer.toString(new StreamSource(
//                new ByteArrayInputStream(createBytesRequestMessage())));
//        
//        System.err.println("##################################################");
//        System.err.println("Msg Sent [" + inputMesage + "]");
//        System.err.println("Msg Recieved [" + reply + "]");
//        System.err.println("##################################################");
//	}
	
    /** 
     * Test the SerializedMarshaler using the Spring HttpInvokerProxyFactoryBean 
     * to initiate the request.  
     */
	public void testUsingSpringHttpRemoting() throws Exception {
		HttpConsumerEndpoint ep = new HttpConsumerEndpoint();
		ep.setService(new QName("urn:httpconsumer", "HttpConsumer"));
		ep.setEndpoint("HttpConsumer");
		ep.setLocationURI("http://localhost:8192/service");
		ep.setTargetService(new QName("http://http.servicemix.org/Test", "ConsumerInOut"));
		
		SerializedMarshaler marshaler = new SerializedMarshaler();
		ep.setMarshaler(marshaler);
		
		HttpComponent component = new HttpComponent();
        component.setEndpoints(new HttpEndpointType[] { ep });
		container.activateComponent(component, "HttpConsumer");
		
		EchoComponent echo = new EchoComponent();
        ActivationSpec asReceiver = new ActivationSpec("echo", echo);
        asReceiver.setService(new QName("http://http.servicemix.org/Test", "ConsumerInOut"));
        container.activateComponent(asReceiver);
		
		container.start();
		
		HttpInvokerProxyFactoryBean pfb = new HttpInvokerProxyFactoryBean();
		pfb.setServiceInterface(Person.class);
		pfb.setServiceUrl("http://localhost:8192/service");
		
		// Not sure what to do about this mess 
		pfb.setHttpInvokerRequestExecutor(new AbstractHttpInvokerRequestExecutor() {
			protected RemoteInvocationResult doExecuteRequest(
					HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws Exception {
				assertEquals("http://localhost:8192/service", config.getServiceUrl());
//				MockHttpServletRequest request = new MockHttpServletRequest();
//				MockHttpServletResponse response = new MockHttpServletResponse();
//				request.setContent(baos.toByteArray());
//				exporter.handleRequest(request, response);
//				return readRemoteInvocationResult(
//						new ByteArrayInputStream(response.getContentAsByteArray()), config.getCodebaseUrl());
				HttpURLConnection con = openConnection(config);
				prepareConnection(con, baos.size());
				writeRequestBody(config, con, baos);
				
				return new RemoteInvocationResult(new Object());
			}
		});
		
		pfb.afterPropertiesSet();
	}
	
	private HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
		URLConnection con = new URL(config.getServiceUrl()).openConnection();
		if (!(con instanceof HttpURLConnection)) {
			throw new IOException("Service URL [" + config.getServiceUrl() + "] is not an HTTP URL");
		}
		return (HttpURLConnection) con;
	}
	
	private void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
		con.setRequestProperty("Content-Length", Integer.toString(contentLength));
	}
	
	private void writeRequestBody(HttpInvokerClientConfiguration config, 
		HttpURLConnection con, ByteArrayOutputStream baos) throws IOException {
		baos.writeTo(con.getOutputStream());
	}
	
	private byte[] createBytesRequestMessage() {
		Person p = new PersonImpl("Hunter", "Thompson", 67); 
		return p.toString().getBytes();
	}

	private OutputStream createSerializedRequestMessage() {
		Person p = new PersonImpl("Hunter", "Thompson", 67); 
		return serializeData(p.toString());
	}
	
	private OutputStream serializeData(String str) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream stream = null; 
		
		try {
			stream = new ObjectOutputStream(baos);
			stream.writeUTF(str);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return stream;
	}
	
}
