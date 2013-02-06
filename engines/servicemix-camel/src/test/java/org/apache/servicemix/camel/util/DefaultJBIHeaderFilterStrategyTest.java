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

import junit.framework.TestCase;
import org.apache.camel.spi.HeaderFilterStrategy;

import java.util.HashMap;

public class DefaultJBIHeaderFilterStrategyTest extends TestCase {

    private HeaderFilterStrategy strategy = new DefaultJBIHeaderFilterStrategy();

    public void testApplyFilterToCamelHeaders() {
        assertTrue("This header is a jbi header.", strategy.applyFilterToCamelHeaders("javax.jbi.ServiceName", "test", null));
        assertFalse("This header should not be filtered", strategy.applyFilterToCamelHeaders("javax.jbitest", "test1", null));
        assertTrue("This header is a jbi header.", strategy.applyFilterToCamelHeaders("javax.jbi.Endpoint", new HashMap(), null));
    }
}
