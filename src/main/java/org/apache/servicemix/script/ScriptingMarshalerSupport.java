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
package org.apache.servicemix.script;

import java.io.IOException;
import java.io.InputStream;

import javax.jbi.messaging.MessageExchange;
import javax.script.Bindings;

/**
 * @author lhein
 */
public interface ScriptingMarshalerSupport {
    /**
     * hook method to allow a custom marshaler to do things on endpoint startup
     * 
     * @param endpoint the endpoint
     * @throws Exception on any exception
     */
    void onStartup(final ScriptEndpoint endpoint) throws Exception;

    /**
     * hook method to allow a custom marshaler to do things on endpoint shutdown
     * 
     * @param endpoint the endpoint
     * @throws Exception on any exception
     */
    void onShutdown(final ScriptEndpoint endpoint) throws Exception;

    /**
     * returns the code of the script as input stream
     * 
     * @param endpoint the endpoint
     * @param exchange the message exchange
     * @return the code of the script as string
     */
    InputStream getScriptCode(final ScriptEndpoint endpoint, final MessageExchange exchange) throws IOException;

    /**
     * hook method for filling user beans into the available variables from
     * script
     * 
     * @param endpoint the endpoint
     * @param exchange the exchange
     * @param bindings the bindings
     */
    void registerUserBeans(final ScriptEndpoint endpoint, final MessageExchange exchange,
                           final Bindings bindings);
}
