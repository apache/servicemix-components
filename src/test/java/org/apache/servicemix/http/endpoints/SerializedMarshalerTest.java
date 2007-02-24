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
import org.springframework.remoting.support.DefaultRemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

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
