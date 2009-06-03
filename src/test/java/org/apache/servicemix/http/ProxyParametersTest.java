package org.apache.servicemix.http;

import org.apache.servicemix.expression.PropertyExpression;

import junit.framework.TestCase;

public class ProxyParametersTest extends TestCase {
	
	private PropertyExpression usernameProp;
	private PropertyExpression passwordProp;
	private ProxyParameters proxyParams;
	private static final String userName = "username";
	private static final String password = "password";
	private static final String proxyHost = "hostname";
	private static final int proxyPort = 80;

	protected void setUp() throws Exception {
		super.setUp();
		usernameProp = new PropertyExpression(userName);
		passwordProp = new PropertyExpression(password);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		usernameProp = null;
		passwordProp = null;
		proxyParams = null;
	}
	
	public void testProxyParams() throws Exception {
		BasicAuthCredentials bac = new BasicAuthCredentials();
		bac.setUsername(usernameProp);
		bac.setPassword(passwordProp);

		// Create the Proxy Parameters
		proxyParams = new ProxyParameters();
		proxyParams.setProxyCredentials(bac);
		proxyParams.setProxyHost(proxyHost);
		proxyParams.setProxyPort(proxyPort);
		
		assertTrue("Proxy Parameters should have non-null authentication credentials", 
				proxyParams.getProxyCredentials() != null);
		assertTrue("Proxy Host should be: " + proxyHost, proxyParams.getProxyHost().equals(proxyHost));
		assertTrue("Proxy Port should be: " + proxyPort, proxyParams.getProxyPort() == proxyPort);		
	}

}
