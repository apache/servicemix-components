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
package org.apache.servicemix.soap.handlers.dom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class DomHandlerTest extends TestCase {

	private DomHandler domHandler;
	private static transient Log log = LogFactory.getLog(DomHandlerTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		domHandler = new DomHandler();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsRequired() {
		assertFalse("isRequired should return false", domHandler.isRequired());
	}

	public void testSetRequired() {
		try {
			domHandler.setRequired(true);
			fail("setRequired to true should throw an UnsupportedOperationException");
		} catch (UnsupportedOperationException uoe) {
			// test passes
			log.info("setRequired method threw the expected exception.");
		}
	}

	public void testRequireDOM() {
		assertTrue("requireDOM should return true", domHandler.requireDOM());
	}

}
