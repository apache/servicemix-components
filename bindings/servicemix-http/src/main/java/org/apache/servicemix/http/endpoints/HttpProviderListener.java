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
package org.apache.servicemix.http.endpoints;

import org.eclipse.jetty.client.HttpClient;

import javax.jbi.messaging.MessageExchange;

/**
 * Listener interface that allows for monitoring Jetty client instances in a {@link HttpProviderEndpoint} and
 * to handle some Jetty client events
 */
public interface HttpProviderListener {

    /*
     * Called when the endpoint starts using the Jetty client instance
     *
     * @param client the Jetty client instance
     */
    void startJettyClientMonitoring(HttpClient client);

    /**
     * Called when the endpoint stops using the Jetty client instance
     *
     * @param client the Jetty client instance
     */
    void stopJettyClientMonitoring(HttpClient client);

    /**
     * Called when the Jetty HTTP exchange gets committed, also passing along the corresponding JBI exchange
     *
     * @param jbiExchange the JBI message exchange
     * @param httpExchange the Jetty HTTP exchange
     */
    void onRequestCommited(MessageExchange jbiExchange, HttpProviderEndpoint.Exchange httpExchange);

    /**
     * Called when the Jetty HTTP exchange is completed, also passing along the corresponding JBI exchange
     *
     * @param jbiExchange the JBI message exchange
     * @param httpExchange the Jetty HTTP exchange
     */
    void onRequestComplete(MessageExchange jbiExchange, HttpProviderEndpoint.Exchange httpExchange);
}
