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
package org.apache.servicemix.jms.wsdl;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import java.io.Serializable;

/**
 * A JMS extensibily element used to specify the parameters needed
 * to retrieve the JMS ConnectionFactory and Destination to use. 
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JmsAddress implements ExtensibilityElement, Serializable {

    /**
     * Generated serial version UID
     */
    private static final long serialVersionUID = -3118867357618475968L;
    
    protected Boolean required;
    protected QName elementType;
    
    protected String initialContextFactory;
    protected String jndiProviderURL;
    protected String destinationStyle;
    protected String jndiConnectionFactoryName;
    protected String jndiDestinationName;
    protected String jmsProviderDestinationName;
    
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
    /**
     * @return Returns the destinationStyle.
     */
    public String getDestinationStyle() {
        return destinationStyle;
    }
    /**
     * @param destinationStyle The destinationStyle to set.
     */
    public void setDestinationStyle(String destinationStyle) {
        this.destinationStyle = destinationStyle;
    }
    /**
     * @return Returns the initialContextFactory.
     */
    public String getInitialContextFactory() {
        return initialContextFactory;
    }
    /**
     * @param initialContextFactory The initialContextFactory to set.
     */
    public void setInitialContextFactory(String initialContextFactory) {
        this.initialContextFactory = initialContextFactory;
    }
    /**
     * @return Returns the jmsProviderDestinationName.
     */
    public String getJmsProviderDestinationName() {
        return jmsProviderDestinationName;
    }
    /**
     * @param jmsProviderDestinationName The jmsProviderDestinationName to set.
     */
    public void setJmsProviderDestinationName(String jmsProviderDestinationName) {
        this.jmsProviderDestinationName = jmsProviderDestinationName;
    }
    /**
     * @return Returns the jndiConnectionFactoryName.
     */
    public String getJndiConnectionFactoryName() {
        return jndiConnectionFactoryName;
    }
    /**
     * @param jndiConnectionFactoryName The jndiConnectionFactoryName to set.
     */
    public void setJndiConnectionFactoryName(String jndiConnectionFactoryName) {
        this.jndiConnectionFactoryName = jndiConnectionFactoryName;
    }
    /**
     * @return Returns the jndiDestinationName.
     */
    public String getJndiDestinationName() {
        return jndiDestinationName;
    }
    /**
     * @param jndiDestinationName The jndiDestinationName to set.
     */
    public void setJndiDestinationName(String jndiDestinationName) {
        this.jndiDestinationName = jndiDestinationName;
    }
    /**
     * @return Returns the jndiProviderURL.
     */
    public String getJndiProviderURL() {
        return jndiProviderURL;
    }
    /**
     * @param jndiProviderURL The jndiProviderURL to set.
     */
    public void setJndiProviderURL(String jndiProviderURL) {
        this.jndiProviderURL = jndiProviderURL;
    }
    
    public String toString() {
        return "JmsAddress[" + 
                    "required=" + required + ", " +
                    "elementType=" + elementType + ", " +
                    "initialContextFactory=" + initialContextFactory + ", " +
                    "jndiProviderURL=" + jndiProviderURL + ", " +
                    "destinationStyle=" + destinationStyle + ", " +
                    "jndiConnectionFactoryName=" + jndiConnectionFactoryName + ", " +
                    "jndiDestinationName=" + jndiDestinationName + ", " +
                    "jmsProviderDestinationName=" + jmsProviderDestinationName + "]";
    }
    
    
}
