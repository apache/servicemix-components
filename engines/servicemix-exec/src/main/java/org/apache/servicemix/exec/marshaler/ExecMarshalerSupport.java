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
package org.apache.servicemix.exec.marshaler;

import javax.jbi.messaging.NormalizedMessage;

/**
 * <p>
 * This interface describes the behavior of an exec marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public interface ExecMarshalerSupport {
    
    /**
     * <p>
     * Unmarshal the content of the in <code>NormalizedMessage</code> to an <code>ExecRequest</code>.
     * </p>
     * 
     * @param in the in message.
     * @return the exec request.
     * @throws Exception in case of unmarshalling error.
     */
    public ExecRequest unmarshal(NormalizedMessage in) throws Exception;
    
    /**
     * <p>
     * Marshal an <code>ExecResponse</code> into the out <code>NormalizedMessage</code>.
     * </p>
     * 
     * @param execResponse the exec response.
     * @param out the out message.
     * @throws Exception in case of marshalling error.
     */
    public void marshal(ExecResponse execResponse, NormalizedMessage out) throws Exception;
    
}
