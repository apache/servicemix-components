package org.apache.servicemix.http;

import junit.framework.TestCase;

public class SslParametersTest extends TestCase {

	private SslParameters sslParams;
	
	protected void tearDown() throws Exception {
		super.tearDown();
		sslParams = null;
	}
	
	// Test to verify that keyManagerFactoryAlgorithm and trustManagerFactoryAlgorithm
	// are set up at object construction.
	public void testSslParamsKeyAndTrustManagerAlgorithms() throws Exception {
		sslParams = new SslParameters();
		assertTrue("Ssl Parameters keyManagerFactoryAlgorithm should not be null", sslParams.getKeyManagerFactoryAlgorithm() != null);
		assertTrue("Ssl Parameters trustManagerFactoryAlgorithm should not be null", sslParams.getTrustManagerFactoryAlgorithm() != null);
	}
	
	// SSL Parameters equals test - same object
	public void testSslParamsObjectsEqualSameObject() throws Exception {
		sslParams = new SslParameters();
		SslParameters sslParamsSame = sslParams;
		assertTrue("SSL Parameters objects should be equal (same)", sslParams.equals(sslParamsSame));
	}
	
	// Test SSL Parameters 2 separate objects equal
	public void testSslParamsObjectsEquals() throws Exception {
		sslParams = new SslParameters();
		SslParameters sslParams2 = new SslParameters();
		assertTrue("SSL Parameters objects should be equal", sslParams.equals(sslParams2));
	}
	
	// Test SSL Parameters 2 objects not equal
	public void testSslParamsObjectsNotEqual() throws Exception {
		sslParams = new SslParameters();
		SslParameters sslParams2 = new SslParameters();
		sslParams2.setKeyPassword("bogus");
		assertTrue("SSL Parameters objects should NOT be equal", !sslParams.equals(sslParams2));
	}
	
	// Test SSL Parameters object not equal to another object type
	public void testSslParamsObjectNotEqualOtherObjectType() throws Exception {
		sslParams = new SslParameters();
		String nonSslParamObj = "some string";
		assertTrue("SSL Parameters object should NOT equal another object type", !sslParams.equals(nonSslParamObj));
	}
}
