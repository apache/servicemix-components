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
package org.apache.servicemix.scripting;

import java.io.IOException;
import java.io.InputStream;

import javax.jbi.messaging.MessageExchange;
import javax.script.Bindings;

/**
 * @author lhein
 */
public class DefaultScriptingMarshaler implements ScriptingMarshalerSupport {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.script.ScriptingMarshalerSupport#onStartup(org.apache.servicemix.script.ScriptEndpoint)
     */
    public void onStartup(ScriptingEndpoint endpoint) throws Exception {
        // nothing to do for now
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.script.ScriptingMarshalerSupport#onShutdown(org.apache.servicemix.script.ScriptEndpoint)
     */
    public void onShutdown(ScriptingEndpoint endpoint) throws Exception {
        // nothing to do for now
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.script.ScriptingMarshalerSupport#registerUserBeans(org.apache.servicemix.script.ScriptEndpoint,
     *      javax.jbi.messaging.MessageExchange, javax.script.Bindings)
     */
    public void registerUserBeans(ScriptingEndpoint endpoint, MessageExchange exchange, Bindings bindings) {
        // no additional beans needed for now
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.script.ScriptingMarshalerSupport#getScriptCode(org.apache.servicemix.script.ScriptEndpoint,
     *      javax.jbi.messaging.MessageExchange)
     */
    public InputStream getScriptCode(ScriptingEndpoint endpoint, MessageExchange exchange) throws IOException {
        return endpoint.getScript().getInputStream();
    }
}
