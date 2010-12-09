package org.apache.servicemix.eip.support;

import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.tck.mock.MockMessageExchange;

import junit.framework.TestCase;

// ExchangeTargetTest creates an ExchangeTarget object and tests the methods on it
// that are not covered by other tests.
public class ExchangeTargetTest extends TestCase {

	private ExchangeTarget exchangeTarget;
	
	protected void setUp() throws Exception {
		super.setUp();
		exchangeTarget = new ExchangeTarget();
	}

	protected void tearDown() throws Exception {
		exchangeTarget = null;
		super.tearDown();
	}
	
	// Test afterPropertiesSet() when interface, service, and uri are all null.
	public void testAfterPropertiesSetException() throws Exception {
		try {
			exchangeTarget.afterPropertiesSet();
			fail("afterPropertiesSet should fail when interface, service, and uri are null.");
		} catch (MessagingException me) {
			// test succeeds
		}
	}
	
	// Test configureTarget() when interface, service, and uri are all null.
	public void testConfigureTargetException() throws Exception {
		try {
			exchangeTarget.configureTarget(null, null);
			fail("configureTarget should fail when interface, service, and uri are null.");
		} catch (MessagingException me) {
			// test succeeds
		}
	}
	
	// Test configureTarget() when interface, service, uri, and endpoint are set.
	public void testConfigureTargetSet() throws Exception {
		MockMessageExchange exchange = new MockMessageExchange();
		EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("endpoint");
        
        JBIContainer container = new JBIContainer();
        container.init();
        
        container.activateComponent(echo, "echo");
        container.start();

		exchangeTarget.setInterface(new QName("test-interface"));
		exchangeTarget.setService(new QName("urn:test", "echo"));
		exchangeTarget.setUri("urn:test:echo");
		exchangeTarget.setEndpoint("endpoint");
		
		// configureTarget should set the interface, service, and endpoint on the
		// exchange.
		exchangeTarget.configureTarget(exchange, echo.getContext());
		
		assertNotNull("Service name should be set on the exchange", exchange.getService());
		assertNotNull("Interface name should be set on the exchange", exchange.getInterfaceName());
		assertNotNull("Endpoint should be set on the exchange", exchange.getEndpoint());
		
		container.stop();
		
	}

}
