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

import java.util.Collections;
import java.util.List;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.xbean.AbstractXBeanServiceUnitAnalyzer;
import org.apache.servicemix.saxon.SaxonEndpoint;

public class SaxonServiceUnitAnalyzer extends AbstractXBeanServiceUnitAnalyzer {

    @Override
    protected List<Consumes> getConsumes(Endpoint endpoint) {
        return Collections.emptyList();
    }

    @Override
    protected String getXBeanFile() {
        return "xbean.xml";
    }

    @Override
    protected boolean isValidEndpoint(Object bean) {
        return bean instanceof SaxonEndpoint;
    }

}
