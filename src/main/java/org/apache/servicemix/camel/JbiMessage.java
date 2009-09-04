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
package org.apache.servicemix.camel;

import javax.jbi.messaging.NormalizedMessage;

import org.apache.camel.impl.DefaultMessage;

/**
 * A JBI {@link org.apache.camel.Message} which provides access to the underlying JBI features
 * such as {@link #getNormalizedMessage()}
 *
 * @version $Revision: 563665 $
 */
@Deprecated
public class JbiMessage extends DefaultMessage {

    private final String message;

    public JbiMessage(String message) {
        this.message = message;
    }

    @Override
    public JbiExchange getExchange() {
        return (JbiExchange) super.getExchange();
    }

    /**
     * Returns the underlying JBI message
     *
     * @return the underlying JBI message
     */
    public NormalizedMessage getNormalizedMessage() {
        return getExchange().getMessageExchange().getMessage(message);
    }

    @Override
    public JbiMessage newInstance() {
        return new JbiMessage(message);
    }
}
