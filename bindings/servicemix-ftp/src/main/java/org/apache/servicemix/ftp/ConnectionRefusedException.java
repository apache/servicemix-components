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
package org.apache.servicemix.ftp;

import javax.jbi.messaging.MessagingException;

/**
 * Exception thrown if the connection could not be established.
 *
 * @version $Revision: 426415 $
 */
public class ConnectionRefusedException extends MessagingException {
    
    private final int code;

    public ConnectionRefusedException(int code) {
        super("Connection refused with return code: " + code);
        this.code = code;
    }

    /**
     * @return the status code for why the connection was refused
     */
    public int getCode() {
        return code;
    }
}
