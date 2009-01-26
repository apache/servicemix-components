package org.apache.servicemix.smpp;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Validate the SMPP XBean descriptor
 * 
 * @author jbonofre
 */
public class SmppXBeanDeployerTest extends TestCase {

    private final static transient Log log = LogFactory.getLog(SmppXBeanDeployerTest.class);

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

    /**
     * Main test that check xbean deployment
     * 
     * @throws Exception in case of deployment errors
     */
    public void test() throws Exception {
        // SMPP component
        SmppComponent component = new SmppComponent();
        container.activateComponent(component, "SMPPComponent");

        // add a receiver component
        // ActivationSpec asEcho = new ActivationSpec("echo", new
        // EchoComponent() {
        //	
        // });

        // start the container
        container.start();

        // deploy SU
        URL url = getClass().getClassLoader().getResource("xbean/xbean.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().init("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().start("xbean");

        // test if endpoint present
        assertNotNull("The endpoint http://test/server/sender is not found in the JBI container", container
            .getRegistry().getEndpoint(new QName("http://test", "service"), "sender"));
        // test if the endpoint descriptor contains something
        // TODO add WSDLs support in the SMPP component
        // assertNotNull("The endpoint http://test/server/sender descriptor is null",
        // container.getRegistry().getEndpointDescriptor(container.getRegistry().getEndpoint(new
        // QName("http://test", "service"), "sender")));

        // main test
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "service"));
        me.getInMessage().setContent(new StringSource("<test>Test</test>"));
        client.sendSync(me);
        // TODO test the MessageExchange ERROR status and fault
        client.done(me);
    }

}
