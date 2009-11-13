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
package org.apache.servicemix.camel.util;

import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.servicemix.camel.test.InvalidSerializableObject;

/**
 * Test cases for {@link org.apache.servicemix.camel.util.StrictSerializationHeaderFilterStrategy}
 */
public class StrictSerializationHeaderFilterStrategyTest extends TestCase {

    private HeaderFilterStrategy strategy = new StrictSerializationHeaderFilterStrategy();

    public void testApplyFilterToCamelHeaders() {
        assertTrue("Strategy should filter ByteArrayInputStream - is not Serializable",
                   strategy.applyFilterToCamelHeaders("key", new ByteArrayOutputStream(), null));
        assertTrue("Strategy should filter Serializable implementations that can not be serialized",
                   strategy.applyFilterToCamelHeaders("key", new InvalidSerializableObject(), null));
        assertFalse("Strategy should not filter String - is Serializable",
                    strategy.applyFilterToCamelHeaders("key", "value", null));
    }
}
