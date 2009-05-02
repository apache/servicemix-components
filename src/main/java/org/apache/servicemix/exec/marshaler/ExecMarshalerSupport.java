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
import javax.xml.transform.TransformerException;

/**
 * This interface describes the behavior of an exec marshaler.
 * 
 * @author jbonofre
 */
public interface ExecMarshalerSupport {
    
    /**
     * <p>
     * Parses the content of the <code>NormalizedMessage</code>, extracts command and arguments
     * to constructs the execution command.
     * </p>
     * 
     * @param message the <code>NormalizedMessage</code>.
     * @return the execution command.
     * @throws TransformerException in case of error during command construction.
     */
    public String constructExecCommand(NormalizedMessage message) throws TransformerException;
    
    /**
     * <p>
     * Formats the execution command output to be embedded in the exchange out message.
     * </p>
     * 
     * @param output the command execution output.
     * @return the command execution output formatted to be embedded in the exchagen out message.
     */
    public String formatExecutionOutput(String output);

}
