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
package org.apache.servicemix.soap.handlers;

import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.Handler;

/**
 * 
 * @author Guillaume Nodet
 * @version $Revision: 1.5 $
 * @since 3.0
 */
public class AbstractHandler implements Handler {
    
    private boolean required;

    /**
     * @return the required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required the required to set
     */
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public boolean requireDOM() {
        return false;
    }

    public void onReceive(Context context) throws Exception {
    }

    public void onReply(Context context) throws Exception {
    }

    public void onFault(Context context) throws Exception {
    }

	public void onSend(Context context) {
	}

	public void onAnswer(Context context) {
	}

}
