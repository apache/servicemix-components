package org.apache.servicemix.http.addressing;

import java.net.URLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.namespace.QName;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import junit.framework.TestCase;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.endpoints.HttpSoapConsumerEndpoint;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.executors.impl.ExecutorFactoryImpl;
import org.apache.servicemix.soap.ws.addressing.WsAddressingPolicy;
import org.apache.servicemix.soap.api.Policy;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.components.util.EchoComponent;
import org.springframework.core.io.ClassPathResource;

public class AddressingConsumerTest extends TestCase {

    private final Logger logger = LoggerFactory.getLogger(AddressingConsumerTest.class);
    
    String port1 = System.getProperty("http.port1");
    protected JBIContainer container;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        ExecutorFactoryImpl factory = new ExecutorFactoryImpl();
        factory.getDefaultConfig().setQueueSize(0);
        container.setExecutorFactory(factory);
        container.init();
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    protected void initSoapEndpoints(boolean useJbiWrapper) throws Exception {
        HttpComponent http = new HttpComponent();
        HttpSoapConsumerEndpoint ep1 = new HttpSoapConsumerEndpoint();
        ep1.setService(new QName("uri:HelloWorld", "HelloService"));
        ep1.setEndpoint("HelloPortSoap11");
        ep1.setTargetService(new QName("urn:test", "echo"));
        ep1.setLocationURI("http://localhost:"+port1+"/ep1/");
        ep1.setWsdl(new ClassPathResource("/org/apache/servicemix/http/HelloWorld-DOC-soap12.wsdl"));
        ep1.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep1.setUseJbiWrapper(useJbiWrapper);
        ep1.setPolicies(new Policy[] { new WsAddressingPolicy() });
        HttpSoapConsumerEndpoint ep2 = new HttpSoapConsumerEndpoint();
        ep2.setService(new QName("uri:HelloWorld", "HelloService"));
        ep2.setEndpoint("HelloPortSoap12");
        ep2.setTargetService(new QName("urn:test", "echo"));
        ep2.setLocationURI("http://localhost:"+port1+"/ep2/");
        ep2.setWsdl(new ClassPathResource("/org/apache/servicemix/http/HelloWorld-DOC-soap12.wsdl"));
        ep2.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep2.setUseJbiWrapper(useJbiWrapper);
        ep2.setPolicies(new Policy[] { new WsAddressingPolicy() });
        http.setEndpoints(new HttpEndpointType[] {ep1, ep2});
        container.activateComponent(http, "http");
    }

    protected void initEchoEndpoint() throws Exception {
        final Document wsdl = DomUtil.parse(getClass().getResourceAsStream("/org/apache/servicemix/http/Echo.wsdl"));
        EchoComponent echo = new EchoComponent() {
            @Override
            public Document getServiceDescription(ServiceEndpoint endpoint) {
                return wsdl;
            }
        };
        echo.setService(new QName("http://test", "MyConsumerService"));
        echo.setEndpoint("myConsumer");
        container.activateComponent(echo, "echo");
    }

    public void testAddressing() throws Exception {
        initSoapEndpoints(true);
        initEchoEndpoint();
        container.start();

        URLConnection connection = new URL("http://localhost:"+port1+"/ep2/").openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        OutputStream os = connection.getOutputStream();
        // Post the request file.
        InputStream fis = getClass().getResourceAsStream("/org/apache/servicemix/http/addressing-request.xml");
        FileUtil.copyInputStream(fis, os);
        // Read the response.
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(is, baos);
        logger.info(baos.toString());
    }

}
