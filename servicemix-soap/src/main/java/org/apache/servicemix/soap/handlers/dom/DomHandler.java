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
package org.apache.servicemix.soap.handlers.dom;

import org.apache.servicemix.soap.handlers.AbstractHandler;

/**
 * Simple handler which force the message to be transformed as a DOM document.
 * This can be useful when debugging or to detect a non-xml input
 * stream before sending a JBI exchange. 
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 * @org.apache.xbean.XBean element="dom"
 */
public class DomHandler extends AbstractHandler {

    public boolean isRequired() {
        return false;
    }
    
    public void setRequired(boolean required) {
        throw new UnsupportedOperationException("required can not be set");
    }
    
    public boolean requireDOM() {
        return true;
    }

}
