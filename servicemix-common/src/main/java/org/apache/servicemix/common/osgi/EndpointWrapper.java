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
package org.apache.servicemix.common.osgi;

import org.apache.servicemix.common.Endpoint;

/**
 * The EndpointWrapper is a very simple interface that wraps an Endpoint.
 * The main purpose of this wrapper is that Spring-DM creates proxy when using
 * collections, so that we don't have access to the real class anymore and can not
 * do anything based on the clas itself.  Going through a wrapper works around
 * this problem.
 */
public interface EndpointWrapper {

    Endpoint getEndpoint();

}
