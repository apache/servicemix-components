package org.apache.servicemix.smpp;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Validate the SMPP XBean descriptor
 * 
 * @author jbonofre
 */
public class SmppXBeanDeployerTest extends TestCase {

    private final static transient Log LOG = LogFactory.getLog(SmppXBeanDeployerTest.class);

    private static final String SOURCE = "0123456789";
    private static final String DESTINATION = "9876543210";
    private static final String TEXT = "This is a SMPP test ...";
    private static final String NPI = "NATIONAL";
    private static final String TON = "INTERNATIONAL";

    private static final String MSG_VALID = "<message><source>" + SOURCE + "</source><destination>"
                                            + DESTINATION + "</destination><text>" + TEXT + "</text><npi>"
                                            + NPI + "</npi><ton>" + TON + "</ton></message>";
    
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
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("http://test", "service"));
        me.getInMessage().setContent(new StringSource(MSG_VALID));
        client.sendSync(me);
        
        if (me.getStatus() == ExchangeStatus.ERROR) {
            // the failure is "normal" as there is no SMPP server mock for now
        	// TODO add a SMPP server mock
        	// fail("Received ERROR status: " + me.getError());
        	LOG.warn("Received ERROR status");
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        } 
    }
}
