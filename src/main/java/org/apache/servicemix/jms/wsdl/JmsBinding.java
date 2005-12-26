/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jms.wsdl;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import java.io.Serializable;

public class JmsBinding implements ExtensibilityElement, Serializable {

    /**
     * Generated serial version UID
     */
    private static final long serialVersionUID = 8700457966223002286L;
    
    protected Boolean required;
    protected QName elementType;
    
    /**
     * @return Returns the elementType.
     */
    public QName getElementType() {
        return elementType;
    }
    /**
     * @param elementType The elementType to set.
     */
    public void setElementType(QName elementType) {
        this.elementType = elementType;
    }
    /**
     * @return Returns the required.
     */
    public Boolean getRequired() {
        return required;
    }
    /**
     * @param required The required to set.
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }
    
}
