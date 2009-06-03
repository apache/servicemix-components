package org.apache.servicemix.http;

import javax.jbi.management.DeploymentException;

import org.apache.servicemix.common.DefaultServiceUnit;

import junit.framework.TestCase;

public class HttpEndpointTest extends TestCase {

	private HttpEndpoint httpEndpoint;
	private MyServiceUnit httpSU;
	
	protected void setUp() throws Exception {
		super.setUp();
		httpEndpoint = new HttpEndpoint();
		httpSU = new MyServiceUnit();
		httpEndpoint.setServiceUnit(httpSU);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		httpEndpoint = null;
	}
	
	// Test validate() when getRole() returns null.
	public void testValidateRoleNull() throws Exception {
		try {
			httpEndpoint.validate();
			fail("validate() should fail when Role is null");
		} catch (DeploymentException de) {
			String errorMsg = de.getMessage();
			assertTrue("Exception should contain the correct error message string", 
					errorMsg.contains("Endpoint must have a defined role"));
		}
	}
	
	// Test validate() when location URI is null.
	public void testValidateLocationUriNull() throws Exception {
		httpEndpoint.setRoleAsString("consumer");
		
		try {
			httpEndpoint.validate();
			fail("validate() should fail when Location URI is null");
		} catch (DeploymentException de) {
			String errorMsg = de.getMessage();
			assertTrue("Exception should contain the correct error message string", 
					errorMsg.contains("Endpoint must have a defined locationURI"));
		}
	}
	
	// Test validate() for non-SOAP endpoint when default MEP is not set.
	public void testValidateNonSoapNoMep() throws Exception {
		httpEndpoint.setRoleAsString("consumer");
		httpEndpoint.setSoap(false);
		httpEndpoint.setLocationURI("http://webhost:8080/someService");
		httpEndpoint.setDefaultMep(null);
		
		try {
			httpEndpoint.validate();
			fail("validate() should fail for non-SOAP endpoint with no default MEP");
		} catch (DeploymentException de) {
			String errorMsg = de.getMessage();
			assertTrue("Exception should contain the correct error message string", 
					errorMsg.contains("Non soap endpoints must have a defined defaultMep"));
		}
	}

	// Support class needed for HttpEndpoint tests.
    public class MyServiceUnit extends DefaultServiceUnit {
        public MyServiceUnit() {
            super(new HttpComponent());
        }
    }
}
