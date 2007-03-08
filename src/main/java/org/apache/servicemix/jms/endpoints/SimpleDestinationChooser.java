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
package org.apache.servicemix.jms.endpoints;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Destination;

/**
 * A simple destination chooser which will use the value of the {@link #DESTINATION_KEY}
 * property on the message exchange, or fall back to a default destination
 *
 * @version $Revision$
 */
public class SimpleDestinationChooser implements DestinationChooser {

    public static final String DESTINATION_KEY = "org.apache.servicemix.jms.destination";

    private Destination defaultDestination;
    private String defaultDestinationName;

    public SimpleDestinationChooser() {
    }

    public SimpleDestinationChooser(Destination defaultDestination) {
        this.defaultDestination = defaultDestination;
    }

    public SimpleDestinationChooser(String defaultDestinationName) {
        this.defaultDestinationName = defaultDestinationName;
    }

    public Object chooseDestination(MessageExchange exchange, Object message) {
        Object property = null;
        if (message instanceof NormalizedMessage) {
            property = ((NormalizedMessage) message).getProperty(DESTINATION_KEY);
        }
        if (property instanceof Destination) {
            return (Destination) property;
        }
        if (getDefaultDestination() != null) {
            return getDefaultDestination();
        }
        return getDefaultDestinationName();
    }

    // Properties
    //-------------------------------------------------------------------------
    public Destination getDefaultDestination() {
        return defaultDestination;
    }

    public void setDefaultDestination(Destination defaultDestination) {
        this.defaultDestination = defaultDestination;
    }

    public String getDefaultDestinationName() {
        return defaultDestinationName;
    }

    public void setDefaultDestinationName(String defaultDestinationName) {
        this.defaultDestinationName = defaultDestinationName;
    }

}
