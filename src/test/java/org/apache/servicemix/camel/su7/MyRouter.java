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
package org.apache.servicemix.camel.su7;

import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.camel.JbiEndpoint;

/**
 * @version $Revision$
 */
public class MyRouter extends RouteBuilder {
    String toUri = "jbi:service:namespace:echo?mep=in-out";

    public void configure() throws Exception {
        // Set the operation from Camel DSL
        org.apache.camel.Endpoint endpoint = getContext().getEndpoint(toUri);
        if (endpoint instanceof JbiEndpoint) {
            JbiEndpoint jbiEndpoint = (JbiEndpoint) endpoint;
            jbiEndpoint.setOperation("{http://test}echo");
        }

        from("jbi:name:cheese").to(toUri);
    }
}
