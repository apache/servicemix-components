package org.servicemix.jms;

import java.io.File;
import java.net.URI;
import java.net.URL;
import junit.framework.TestCase;
import org.activemq.broker.BrokerService;
import org.activemq.xbean.BrokerFactoryBean;
import org.servicemix.jbi.container.JBIContainer;
import org.springframework.core.io.ClassPathResource;

public class CopyOfJMSComponentTest extends TestCase {

    protected JBIContainer container;
    protected BrokerService broker;
    
    public void setUp() throws Exception {
        container = new JBIContainer();
        container.setCreateMBeanServer(true);
        container.init();
        //container.start();
        
       
        BrokerFactoryBean bfb = new BrokerFactoryBean(new ClassPathResource("org/servicemix/jms/activemq.xml"));
        bfb.afterPropertiesSet();
        broker = bfb.getBroker();
        broker.start();
    }
    
    protected void tearDown() throws Exception {
        if (broker != null) {
            broker.stop();
        }
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testJms() throws Exception {
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");

        URL url = Thread.currentThread().getContextClassLoader().getResource("provider/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");
    }
    
    public void testJmsSoap() throws Exception {
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");

        URL url = Thread.currentThread().getContextClassLoader().getResource("jms_soap/provider.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("jms_soap", path.getAbsolutePath());
        component.getServiceUnitManager().start("jms_soap");
    }
}
