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
package org.apache.servicemix.soap.api;

import java.util.Set;


public interface Interceptor {
    
    /**
     * Intercepts a message. 
     * Interceptors need NOT invoke handleMessage or handleFault
     * on the next interceptor - the interceptor chain will
     * take care of this.
     * 
     * @param message
     */
    void handleMessage(Message message);
    
    /**
     * Called for all interceptors (in reverse order) on which handleMessage
     * had been successfully invoked, when normal execution of the chain was
     * aborted for some reason.
     * 
     * @param message
     */
    void handleFault(Message message);
    
    /**
     * A Set of IDs that this interceptor needs to run after.
     * @return
     */
    Set<String> getAfter();

    /**
     * A Set of IDs that this interceptor needs to run before.
     * @return
     */
    Set<String> getBefore();

    /**
     * The ID of the interceptor.
     * @return
     */
    String getId();

}
