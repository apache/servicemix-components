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
package org.apache.servicemix.soap.bindings.soap.impl;

import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapPart;
import org.apache.servicemix.soap.core.model.AbstractWsdl1Part;

public class Wsdl1SoapPartImpl extends AbstractWsdl1Part implements Wsdl1SoapPart {

    private boolean isBody;
    private boolean isHeader;
    
    /**
     * @return the isBody
     */
    public boolean isBody() {
        return isBody;
    }
    /**
     * @param isBody the isBody to set
     */
    public void setBody(boolean isBody) {
        this.isBody = isBody;
    }
    /**
     * @return the isHeader
     */
    public boolean isHeader() {
        return isHeader;
    }
    /**
     * @param isHeader the isHeader to set
     */
    public void setHeader(boolean isHeader) {
        this.isHeader = isHeader;
    }
    
}
