package org.apache.servicemix.http.endpoints;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.components.util.TransformComponentSupport;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.remoting.support.DefaultRemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

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

//    /**
//     * Test the SerializedMarshaler using the Spring HttpInvokerProxyFactoryBean
//     * to initiate the request.
//     */
//	public void testUsingSpringHttpRemoting() throws Exception {
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
//		HttpInvokerProxyFactoryBean pfb = new HttpInvokerProxyFactoryBean();
//		pfb.setServiceInterface(Person.class);
//		pfb.setServiceUrl("http://localhost:8192/service");
//
//		// Not sure what to do about this mess
//		pfb.setHttpInvokerRequestExecutor(new AbstractHttpInvokerRequestExecutor() {
//			protected RemoteInvocationResult doExecuteRequest(
//					HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws Exception {
//				assertEquals("http://localhost:8192/service", config.getServiceUrl());
////				MockHttpServletRequest request = new MockHttpServletRequest();
////				MockHttpServletResponse response = new MockHttpServletResponse();
////				request.setContent(baos.toByteArray());
////				exporter.handleRequest(request, response);
////				return readRemoteInvocationResult(
////						new ByteArrayInputStream(response.getContentAsByteArray()), config.getCodebaseUrl());
//				HttpURLConnection con = openConnection(config);
//				prepareConnection(con, baos.size());
//				writeRequestBody(config, con, baos);
//
//				return new RemoteInvocationResult(new Object());
//			}
//		});
//
//		pfb.afterPropertiesSet();
//	}
//
//	private HttpURLConnection openConnection(HttpInvokerClientConfiguration config) throws IOException {
//		URLConnection con = new URL(config.getServiceUrl()).openConnection();
//		if (!(con instanceof HttpURLConnection)) {
//			throw new IOException("Service URL [" + config.getServiceUrl() + "] is not an HTTP URL");
//		}
//		return (HttpURLConnection) con;
//	}
//
//	private void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
//		con.setDoOutput(true);
//		con.setRequestMethod("POST");
//		con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
//		con.setRequestProperty("Content-Length", Integer.toString(contentLength));
//	}
//
//	private void writeRequestBody(HttpInvokerClientConfiguration config,
//		HttpURLConnection con, ByteArrayOutputStream baos) throws IOException {
//		baos.writeTo(con.getOutputStream());
//	}
//
//	private byte[] createBytesRequestMessage() {
//		Person p = new PersonImpl("Hunter", "Thompson", 67);
//		return p.toString().getBytes();
//	}
//
//	private OutputStream createSerializedRequestMessage() {
//		Person p = new PersonImpl("Hunter", "Thompson", 67);
//		return serializeData(p.toString());
//	}
//
//	private OutputStream serializeData(String str) {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ObjectOutputStream stream = null;
//
//		try {
//			stream = new ObjectOutputStream(baos);
//			stream.writeUTF(str);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return stream;
//	}

    public void testUsingSpringHttpRemoting() throws Exception {
        final Person person = new PersonImpl("Hunter", "Thompson", 67);

        HttpConsumerEndpoint ep = new HttpConsumerEndpoint();
        ep.setService(new QName("urn:HttpConsumer", "HttpConsumer"));
        ep.setEndpoint("HttpConsumer");
        ep.setLocationURI("http://localhost:8192/service/");
        ep.setTargetService(new QName("urn:HttpInvoker", "Endpoint"));

        SerializedMarshaler marshaler = new SerializedMarshaler();
        marshaler.setDefaultMep(MessageExchangeSupport.IN_OUT);
        ep.setMarshaler(marshaler);

        HttpComponent component = new HttpComponent();
        component.setEndpoints(new HttpEndpointType[] { ep });
        container.activateComponent(component, "HttpConsumer");

        TransformComponentSupport rmiComponent = new TransformComponentSupport() {
            protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws MessagingException {
                try {
                    // Deserialize rmi invocation
                    XStream xstream = new XStream(new DomDriver());
                    SourceTransformer st = new SourceTransformer();
                    Object rmi = xstream.fromXML(st.toString(in.getContent()));

                    DefaultRemoteInvocationExecutor executor = new DefaultRemoteInvocationExecutor();
                    Object result = executor.invoke((RemoteInvocation)rmi, person);

                    // Convert result to an rmi invocation
                    RemoteInvocationResult rmiResult = new RemoteInvocationResult(result);
                    out.setContent(new StringSource(xstream.toXML(rmiResult)));
                } catch (Exception e) {
                    throw new MessagingException (e);
                }

                return true;
            }
        };
        ActivationSpec asReceiver = new ActivationSpec("rmiComponent", rmiComponent);
        asReceiver.setService(new QName("urn:HttpInvoker", "Endpoint"));

        container.activateComponent(asReceiver);
        container.start();

        HttpInvokerProxyFactoryBean pfb = new HttpInvokerProxyFactoryBean();
        pfb.setServiceInterface(Person.class);
        pfb.setServiceUrl("http://localhost:8192/service/");
        pfb.setHttpInvokerRequestExecutor(new SimpleHttpInvokerRequestExecutor());
        pfb.afterPropertiesSet();

        Person test = (Person)pfb.getObject();

        // Test getters
        assertEquals("Hunter", test.getGivenName());
        assertEquals("Thompson", test.getSurName());
        assertEquals(67, test.getAge());

        // Test setters
        test.setGivenName("John");
        test.setSurName("Doe");
        test.setAge(34);

        assertEquals(person.getGivenName(), "John");
        assertEquals(person.getSurName(), "Doe");
        assertEquals(person.getAge(), 34);
    }
}
