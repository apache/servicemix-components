package org.apache.servicemix.eip.packaging;

import java.util.List;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultServiceUnit;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.eip.AbstractEIPTest;
import org.apache.servicemix.eip.patterns.ContentBasedRouter;
import org.apache.servicemix.eip.patterns.ContentEnricher;
import org.apache.servicemix.eip.patterns.MessageFilter;
import org.apache.servicemix.eip.patterns.Pipeline;
import org.apache.servicemix.eip.patterns.SplitAggregator;
import org.apache.servicemix.eip.patterns.StaticRecipientList;
import org.apache.servicemix.eip.patterns.StaticRoutingSlip;
import org.apache.servicemix.eip.patterns.WireTap;
import org.apache.servicemix.eip.patterns.XPathSplitter;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.eip.support.RoutingRule;
import org.apache.servicemix.eip.support.XPathPredicate;
import org.w3c.dom.Document;

public class EipServiceUnitAnalyzerTest extends AbstractEIPTest {

	private EipServiceUnitAnalyzer analyzer;
	private DefaultServiceUnit su;
	
	protected void setUp() throws Exception {
		super.setUp();
		analyzer = new EipServiceUnitAnalyzer();
		su = new DefaultServiceUnit();
	}

	protected void tearDown() throws Exception {
		su = null;
		analyzer = null;
		super.tearDown();
	}
	
	// Test isValidEndpoint for a valid EIP endpoint.
	public void testIsValidEndpointTrue() throws Exception {
		ContentBasedRouter router = new ContentBasedRouter();
		router.setService(new QName("cbr"));
		router.setEndpoint("cbr");
		su.addEndpoint(router);
		
		assertTrue("isValidEndpoint() should return true for ContentBasedRouter endpoint", 
				analyzer.isValidEndpoint(router));
	}
	
	// Test isValidEndpoint for a non-EIP endpoint.
	public void testIsValidEndpointFalse() throws Exception {
		TestEndpoint testEp = new TestEndpoint();
		su.addEndpoint(testEp);
		
		assertFalse("isValidEndpoint() should return false for non-EIP endpoint",
				analyzer.isValidEndpoint(testEp));
	}
	
	// test getConsumes() with a ContentBasedRouter endpoint
	public void testGetConsumesContentBasedRouter() throws Exception {
		ContentBasedRouter router = new ContentBasedRouter();
		router.setService(new QName("cbr"));
		router.setEndpoint("cbr");
		router.setRules(new RoutingRule[] {
	            new RoutingRule(
	                    new XPathPredicate("/hello/@id = '1'"),
	                    createServiceExchangeTarget(new QName("target1"))),
	            new RoutingRule(
	                    new XPathPredicate("/hello/@id = '2'"),
	                    createServiceExchangeTarget(new QName("target2"))),
	            new RoutingRule(
	                    null,
	                    createServiceExchangeTarget(new QName("target3")))
	        });
		su.addEndpoint(router);
		
		List<Consumes> consumesList = analyzer.getConsumes(router);
		
		assertNotNull("getConsumes() for ContentBasedRouter endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 3 entries", consumesList.size() == 3);
	}
	
	// test getConsumes() with the MessageFilter endpoint
	public void testGetConsumesMessageFilter() throws Exception {
		MessageFilter messageFilter = new MessageFilter();
		messageFilter = new MessageFilter();
        messageFilter.setFilter(new XPathPredicate("/hello/@id = '1'"));
        messageFilter.setTarget(createServiceExchangeTarget(new QName("target")));
        messageFilter.setService(new QName("messageFilter"));
        messageFilter.setEndpoint("messageFilter");
        
        su.addEndpoint(messageFilter);
        
        List<Consumes> consumesList = analyzer.getConsumes(messageFilter);
        
        assertNotNull("getConsumes() for MessageFilter endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 1 entry", consumesList.size() == 1);
	}
	
	// test getConsumes() with the Pipeline endpoint
	public void testGetConsumesPipeline() throws Exception {
		Pipeline pipeline = new Pipeline();
		pipeline.setTransformer(createServiceExchangeTarget(new QName("transformer")));
        pipeline.setTarget(createServiceExchangeTarget(new QName("target")));
        pipeline.setService(new QName("pipeline"));
        pipeline.setEndpoint("pipeline");
        
        su.addEndpoint(pipeline);
        
        List<Consumes> consumesList = analyzer.getConsumes(pipeline);
        
        assertNotNull("getConsumes() for Pipeline endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 2 entries", consumesList.size() == 2);
	}
	
	// test getConsumes() with SplitAggregator endpoint
	public void testGetConsumesSplitAggregator() throws Exception {
		SplitAggregator aggregator = new SplitAggregator();
		aggregator.setTarget(createServiceExchangeTarget(new QName("target")));
        aggregator.setCopyProperties(true);
        aggregator.setService(new QName("splitAggregator"));
        aggregator.setEndpoint("splitAggregator");
        
        su.addEndpoint(aggregator);
        
        List<Consumes> consumesList = analyzer.getConsumes(aggregator);
        
        assertNotNull("getConsumes() for SplitAggregator endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 1 entry", consumesList.size() == 1);      
	}
	
	// test getConsumes() with StaticRecipientList endpoint
	public void testGetConsumesStaticRecipientList() throws Exception {
		StaticRecipientList recipientList = new StaticRecipientList();
		recipientList.setRecipients(
                new ExchangeTarget[] {
                        createServiceExchangeTarget(new QName("recipient1")),
                        createServiceExchangeTarget(new QName("recipient2")),
                        createServiceExchangeTarget(new QName("recipient3")),
                        createServiceExchangeTarget(new QName("recipient4"))
                });
		recipientList.setService(new QName("recipList"));
		recipientList.setEndpoint("recipList");
		
		su.addEndpoint(recipientList);

		List<Consumes> consumesList = analyzer.getConsumes(recipientList);
        
        assertNotNull("getConsumes() for StaticRecipientList endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 4 entries", consumesList.size() == 4);      	
	}
	
	// test getConsumes() with StaticRoutingSlip endpoint
	public void testGetConsumesStaticRoutingSlip() throws Exception {
		StaticRoutingSlip routingSlip = new StaticRoutingSlip();
		routingSlip.setTargets(
                new ExchangeTarget[] {
                        createServiceExchangeTarget(new QName("target1")),
                        createServiceExchangeTarget(new QName("target2")),
                        createServiceExchangeTarget(new QName("target3"))
                });
		routingSlip.setService(new QName("routingSlip"));
		routingSlip.setEndpoint("routingSlip");
		
		su.addEndpoint(routingSlip);
		
        List<Consumes> consumesList = analyzer.getConsumes(routingSlip);
        
        assertNotNull("getConsumes() for StaticRoutingSlip endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 3 entries", consumesList.size() == 3);
	}
	
	// test getConsumes() with WireTap endpoint
	public void testGetConsumesWireTap() throws Exception {
		WireTap wireTap = new WireTap();
        wireTap.setInListener(createServiceExchangeTarget(new QName("in")));
        wireTap.setOutListener(createServiceExchangeTarget(new QName("out")));
        wireTap.setFaultListener(createServiceExchangeTarget(new QName("fault")));
        wireTap.setTarget(createServiceExchangeTarget(new QName("target")));
        wireTap.setService(new QName("wireTap"));
        wireTap.setEndpoint("wireTap");
        
        su.addEndpoint(wireTap);
        
        List<Consumes> consumesList = analyzer.getConsumes(wireTap);
        
        assertNotNull("getConsumes() for WireTap endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 4 entries", consumesList.size() == 4);
	}
	
	// test getConsumes() with XPathSplitter endpoint
	public void testGetConsumesXPathSplitter() throws Exception {
		XPathSplitter splitter = new XPathSplitter();
        splitter.setTarget(createServiceExchangeTarget(new QName("target")));
        splitter.setXPath("/hello/*");
        splitter.setService(new QName("splitter"));
        splitter.setEndpoint("splitter");
        
        su.addEndpoint(splitter);
       
        List<Consumes> consumesList = analyzer.getConsumes(splitter);
        
        assertNotNull("getConsumes() for XPathSplitter endpoint should return a non-null list", 
				consumesList);
		assertTrue("Consumes list should contain 1 entry", consumesList.size() == 1);      
	}
	
	// test getConsumes() with unresolvable EIP endpoint
	public void testGetConsumesUnresolvableEndpoint() throws Exception {
		ContentEnricher enricher = new ContentEnricher();
        enricher.setEnricherTarget(createServiceExchangeTarget(new QName("enricherTarget")));
        enricher.setTarget(createServiceExchangeTarget(new QName("target")));
        enricher.setService(new QName("enricher"));
        enricher.setEndpoint("enricher");
        
        su.addEndpoint(enricher);
        
        List<Consumes> consumesList = analyzer.getConsumes(enricher);
        
        // EipServiceUnitAnalyzer's getConsumes just creates an empty array for
        // unresolvable endpoints.  Unresolvable means that there is no resolve*()
        // method for it in EipServiceUnitAnalyzer.java.
        assertNotNull("getConsumes() for ContentEnricher endpoint should return a non-null list", 
        		consumesList);
        assertTrue("Consumes list should contain 0 entries", consumesList.size() == 0);
	}
	
	// test getXBeanFile
	public void testGetXBeanFile() throws Exception {
		String xbeanFile = analyzer.getXBeanFile();
		
		assertTrue("getXBeanFile should return xbean.xml", xbeanFile.equalsIgnoreCase("xbean.xml"));
	}

	// Dummy Endpoint implementation for testing.
	public static class TestEndpoint implements Endpoint {

		private String key;
		
		public TestEndpoint() {
			key = "test";
		}
		
		public void activate() throws Exception {
		}

		public void deactivate() throws Exception {
		}

		public Document getDescription() {
			return null;
		}

		public String getEndpoint() {
			return null;
		}

		public QName getInterfaceName() {
			return null;
		}

		public String getKey() {
			return key;
		}

		public Role getRole() {
			return null;
		}

		public QName getService() {
			return null;
		}

		public ServiceUnit getServiceUnit() {
			return null;
		}

		public boolean isExchangeOkay(MessageExchange exchange) {
			return false;
		}

		public void process(MessageExchange exchange) throws Exception {
			
		}

		public void setServiceUnit(ServiceUnit serviceUnit) {
			
		}

		public void start() throws Exception {
			
		}

		public void stop() throws Exception {
			
		}

		public void validate() throws DeploymentException {
			
		}
		
	}
}
