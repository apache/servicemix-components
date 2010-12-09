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
package org.apache.servicemix.common;

import javax.jbi.messaging.MessageExchange;
import javax.transaction.Status;

import org.apache.servicemix.MessageExchangeListener;

/**
 * Base class for life cycle management of components.
 * This lifecycle uses Push delivery by implementing MessageExchangeListerner interface
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 * @since 3.0
 * @deprecated use SyncLifeCycleWrapper instead
 */
public class BaseLifeCycle extends AsyncBaseLifeCycle implements MessageExchangeListener {

    protected BaseLifeCycle() {
    }
    
    public BaseLifeCycle(ServiceMixComponent component) {
        super(component);
    }
    
}
