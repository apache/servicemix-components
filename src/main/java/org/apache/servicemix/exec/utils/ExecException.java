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
package org.apache.servicemix.exec.utils;

/**
 * Exception raised during a system command execution.
 * 
 * @author jbonofre
 */
public class ExecException extends Exception {

    private static final long serialVersionUID = -7151089812651503402L;

    /**
     * <p>
     * Creates an <code>ExecException</code> with a description message.
     * </p>
     * 
     * @param message the exception description message.
     */
    public ExecException(String message) {
        super(message);
    }
    
    /**
     * <p>
     * Creates an <code>ExecException</code> with an underlying cause.
     * </p>
     * 
     * @param cause the underlying cause.
     */
    public ExecException(Throwable cause) {
        super(cause);
    }
    
    /**
     * <p>
     * Creates an <code>ExecException</code> with a description message and a underlying cause.
     * </p>
     * 
     * @param message the exception description message.
     * @param cause the underlying cause.
     */
    public ExecException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
