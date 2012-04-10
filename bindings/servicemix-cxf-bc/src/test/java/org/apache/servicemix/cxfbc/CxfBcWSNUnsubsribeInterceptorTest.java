package org.apache.servicemix.cxfbc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.components.util.MockServiceComponent;
import org.apache.servicemix.cxfbc.interceptors.WSNUnsubsribeInterceptor;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.springframework.core.io.ClassPathResource;

public class CxfBcWSNUnsubsribeInterceptorTest extends TestCase{

    private JBIContainer jbi;

    protected void setUp() throws Exception {
        jbi = new JBIContainer();
        jbi.setEmbedded(true);
        jbi.init();
        jbi.start();

    }
    
    public void testWSNUnsubsribeInterceptor() throws Exception {
        
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setWsdl(new ClassPathResource("org/apache/servicemix/cxfbc/wsn/wsn.wsdl"));
        ep.setService(new QName("http://sample.com/wsn", "SubscriptionManagerService"));
        ep.setTargetService(new QName("http://servicemix.org/wsnotification", "Subscription"));
        ep.getInInterceptors().add(new WSNUnsubsribeInterceptor());
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");

        MockServiceComponent echo = new MockServiceComponent();
        echo.setService(new QName("http://servicemix.org/wsnotification", "Subscription"));
        echo.setEndpoint("http://servicemix.org/wsnotification/Subscription/ID-192-168-1-102-1369a1368ec-27-0");
        echo.setResponseXml(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\" version=\"1.0\"> " +
                "<jbi:part><ns4:UnsubscribeResponse xmlns:ns4=\"http://docs.oasis-open.org/wsn/b-2\" " +
                "xmlns:ns2=\"http://www.w3.org/2005/08/addressing\" " +
                "xmlns:ns3=\"http://docs.oasis-open.org/wsrf/bf-2\" " +
                "xmlns:ns5=\"http://docs.oasis-open.org/wsrf/rp-2\" " +
                "xmlns:ns6=\"http://docs.oasis-open.org/wsn/t-1\"/>" +
                "</jbi:part></jbi:message>"
        );
        jbi.activateComponent(echo, "echo");

        URLConnection connection = new URL("http://localhost:5728/sample/SubscriptionManagerService/")
                .openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        // Post the request file.
        InputStream fis = new ClassPathResource("org/apache/servicemix/cxfbc/wsn/WSN-Unsubsribe-Input.xml")
                .getInputStream();
        
        CxfBcComponentTest.copyInputStream(fis, os);
        // Read the response.
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CxfBcComponentTest.copyInputStream(is, baos);
        System.err.println(baos.toString());
    }

}
