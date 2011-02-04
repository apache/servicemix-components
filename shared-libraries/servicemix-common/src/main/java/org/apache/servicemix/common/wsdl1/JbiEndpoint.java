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
package org.apache.servicemix.common.wsdl1;

import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import java.io.Serializable;
import java.net.URI;

public class JbiEndpoint implements ExtensibilityElement, Serializable {

    private static final long serialVersionUID = -3118867357618475968L;
    
    protected Boolean required;
    protected QName elementType;
    
    protected Role role;
    protected URI defaultMep;
    protected QName defaultOperation;

    public QName getElementType() {
        return elementType;
    }

    public void setElementType(QName elementType) {
        this.elementType = elementType;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public URI getDefaultMep() {
        return defaultMep;
    }

    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public QName getDefaultOperation() {
        return defaultOperation;
    }

    public void setDefaultOperation(QName defaultOperation) {
        this.defaultOperation = defaultOperation;
    }
    
    public String toString() {
        return "JbiEndpoint[" + 
                    "required=" + required + ", " +
                    "elementType=" + elementType + ", " +
                    "role=" + role + ", " +
                    "defaultMep=" + defaultMep + "," +
                    "defaultOperation=" + defaultOperation + "]";
    }
    
}
