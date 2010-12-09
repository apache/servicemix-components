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
package org.apache.servicemix.soap.bindings.http.impl;

import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpMessage;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpOperation;
import org.apache.servicemix.soap.core.model.AbstractWsdl2Operation;

public class Wsdl2HttpOperationImpl extends AbstractWsdl2Operation<Wsdl2HttpMessage> implements Wsdl2HttpOperation {

    private String httpInputSerialization;
    private String httpOutputSerialization;
    private String httpFaultSerialization;
    private String httpLocation;
    private String httpMethod;
    private String httpTransferCodingDefault;
    private boolean httpLocationIgnoreUncited;
    
    /**
     * @return the httpFaultSerialization
     */
    public String getHttpFaultSerialization() {
        return httpFaultSerialization;
    }
    /**
     * @param httpFaultSerialization the httpFaultSerialization to set
     */
    public void setHttpFaultSerialization(String httpFaultSerialization) {
        this.httpFaultSerialization = httpFaultSerialization;
    }
    /**
     * @return the httpInputSerialization
     */
    public String getHttpInputSerialization() {
        return httpInputSerialization;
    }
    /**
     * @param httpInputSerialization the httpInputSerialization to set
     */
    public void setHttpInputSerialization(String httpInputSerialization) {
        this.httpInputSerialization = httpInputSerialization;
    }
    /**
     * @return the httpLocation
     */
    public String getHttpLocation() {
        return httpLocation;
    }
    /**
     * @param httpLocation the httpLocation to set
     */
    public void setHttpLocation(String httpLocation) {
        this.httpLocation = httpLocation;
    }
    /**
     * @return the httpMethod
     */
    public String getHttpMethod() {
        return httpMethod;
    }
    /**
     * @param httpMethod the httpMethod to set
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    /**
     * @return the httpOutputSerialization
     */
    public String getHttpOutputSerialization() {
        return httpOutputSerialization;
    }
    /**
     * @param httpOutputSerialization the httpOutputSerialization to set
     */
    public void setHttpOutputSerialization(String httpOutputSerialization) {
        this.httpOutputSerialization = httpOutputSerialization;
    }
    /**
     * @return the httpTransferCodingDefault
     */
    public String getHttpTransferCodingDefault() {
        return httpTransferCodingDefault;
    }
    /**
     * @param httpTransferCodingDefault the httpTransferCodingDefault to set
     */
    public void setHttpTransferCodingDefault(String httpTransferCodingDefault) {
        this.httpTransferCodingDefault = httpTransferCodingDefault;
    }
    /**
     * @return the httpLocationIgnoreUncited
     */
    public boolean isHttpLocationIgnoreUncited() {
        return httpLocationIgnoreUncited;
    }
    /**
     * @param httpLocationIgnoreUncited the httpLocationIgnoreUncited to set
     */
    public void setHttpLocationIgnoreUncited(boolean httpLocationIgnoreUncited) {
        this.httpLocationIgnoreUncited = httpLocationIgnoreUncited;
    }

}
