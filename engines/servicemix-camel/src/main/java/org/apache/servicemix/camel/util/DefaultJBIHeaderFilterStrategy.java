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

import org.apache.camel.Exchange;
import org.apache.camel.spi.HeaderFilterStrategy;

public class DefaultJBIHeaderFilterStrategy implements HeaderFilterStrategy {

    public boolean applyFilterToCamelHeaders(String s, Object o, Exchange exchange) {
        return doApplyFilter(s);
    }

    public boolean applyFilterToExternalHeaders(String s, Object o, Exchange exchange) {
        return doApplyFilter(s);
    }

    // Here we should filter the jbi message headers which should be not be exposed to Camel
    private boolean doApplyFilter(String header) {
        if (header.startsWith("javax.jbi.")) {
            return true;
        } else {
            return false;
        }
    }
}
