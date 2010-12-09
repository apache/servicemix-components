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

import java.util.LinkedList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.spi.HeaderFilterStrategy;

/**
 * {@link HeaderFilterStrategy} implementation that will evaluate a set of
 * strategies in an OR-like fashion (if one of the strategies filters the property,
 * the HeaderFilter
 */
public class HeaderFilterStrategies implements HeaderFilterStrategy {

    private final List<HeaderFilterStrategy> strategies = new LinkedList<HeaderFilterStrategy>();

    public boolean applyFilterToCamelHeaders(String s, Object o, Exchange exchange) {
        for (HeaderFilterStrategy strategy : strategies) {
            if (strategy.applyFilterToCamelHeaders(s, o, exchange)) {
                return true;
            }
        }
        return false;
    }

    public boolean applyFilterToExternalHeaders(String s, Object o, Exchange exchange) {
        for (HeaderFilterStrategy strategy : strategies) {
            if (strategy.applyFilterToExternalHeaders(s, o, exchange)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a strategy to the set of strategies
     *
     * @param strategy
     */
    public void add(HeaderFilterStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
        }
    }
}
