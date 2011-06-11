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
package org.apache.servicemix.http.exception;

import javax.jbi.messaging.MessageExchange;

/**
 * Exception to indicate that a late response has been received from the ESB, i.e. the ESB response was received
 * after the HTTP connection has timed out so we were unable to send back the response to the client.
 */
public class LateResponseException extends Exception {

    public LateResponseException(MessageExchange exchange) {
        super(createMessage(exchange));
    }

    /**
     * Helper method to create the late response error message
     *
     * @param exchange the exchange we received a late response for
     * @return the error message
     */
    public static String createMessage(MessageExchange exchange) {
        return String.format("Late response message for %s - received after HTTP connection has timed out",
                             exchange.getExchangeId());
    }

}
