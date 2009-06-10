/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.saxon.packaging;

import javax.jbi.JBIException;

import junit.framework.TestCase;

import org.apache.servicemix.saxon.XQueryEndpoint;
import org.apache.servicemix.saxon.XsltEndpoint;
import org.apache.servicemix.saxon.XsltProxyEndpoint;

public class SaxonServiceUnitAnalyzerTest extends TestCase {

	private SaxonServiceUnitAnalyzer analyzer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		analyzer = new SaxonServiceUnitAnalyzer();
	}

	public void testGetConsumes() throws JBIException {
		assertNotNull("Collection should not be null", analyzer.getConsumes(new XsltEndpoint()));
		assertTrue("Collection should be empty", analyzer.getConsumes().isEmpty());
	}

	public void testGetXBeanFile() throws Exception {
		assertEquals("xbean.xml", analyzer.getXBeanFile());
	}

	public void testIsValidEndpoint() throws Exception {
		assertTrue(analyzer.isValidEndpoint(new XsltEndpoint()));
        assertTrue(analyzer.isValidEndpoint(new XQueryEndpoint()));
		assertTrue(analyzer.isValidEndpoint(new XsltProxyEndpoint()));

	}
}
