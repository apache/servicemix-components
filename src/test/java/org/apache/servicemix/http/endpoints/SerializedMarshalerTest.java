package org.apache.servicemix.http.endpoints;

import java.io.ByteArrayInputStream;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.w3c.dom.DocumentFragment;

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

	public void testCreateExchange() throws Exception{
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
		
		DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut inout = client.createInOutExchange();
        inout.setService(new QName("urn:httpconsumer", "HttpConsumer"));
        DocumentFragment epr = URIResolver.createWSAEPR("http://localhost:8192/service");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        inout.setEndpoint(se);
        inout.getInMessage().setContent(
                new StreamSource(new ByteArrayInputStream(createRequestMessage())));
        
        long t0 = System.currentTimeMillis();
        client.sendSync(inout);
        long t1 = System.currentTimeMillis();
        System.out.println("%%%%%%%%%%%%%%% Exchange: " + inout);
        System.out.println("Error: " + inout.getError());
        assertTrue(inout.getStatus() == ExchangeStatus.ACTIVE);
        
        System.err.println("Executed in " + (t1 - t0) + "ms");

        assertNotNull(inout.getOutMessage());
        assertNotNull(inout.getOutMessage().getContent());
        
        SourceTransformer sourceTransformer = new SourceTransformer();
        String reply = sourceTransformer.toString(inout.getOutMessage().getContent());
        
        String inputMesage = sourceTransformer.toString(new StreamSource(
                new ByteArrayInputStream(createRequestMessage())));
        
        System.err.println("##################################################");
        System.err.println("Msg Sent [" + inputMesage + "]");
        System.err.println("Msg Recieved [" + reply + "]");
        System.err.println("##################################################");
	}
	
	public void XtestUsingSpringHttpRemoting() throws Exception {
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
	}

	private byte[] createRequestMessage() {
		Person p = new PersonImpl("Hunter", "Thompson", 67); 
		return p.toString().getBytes();
	}
	
}
