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
package org.apache.servicemix.eip.support;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.jbi.messaging.MessageExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Switch (on/off) predicate based on a property that can come from
 * a system property, a property from a property file (specified as Spring resource),
 * or a property from the exchange.
 * <p/>
 * The property is interpreted as a boolean value.
 *
 * @org.apache.xbean.XBean element="switch-predicate"
 */
public class SwitchPredicate implements InitializingBean, Predicate {

    private final Logger logger = LoggerFactory.getLogger(SwitchPredicate.class);

    // property resource optionally
    private Resource propertyResource;

    // use property from exchange
    private boolean fromExchange;

    // property name
    private String propertyName = "on";

    private Boolean on = Boolean.FALSE;

    private File propertyFile;

    private boolean dirty;

    public void afterPropertiesSet() throws Exception {
        try {
            Properties props = null;
            if (propertyResource != null && propertyResource.exists()) {
                // get property from properties file
                propertyFile = propertyResource.getFile();
                if (logger.isDebugEnabled()) {
                    logger.debug("loading property file: " + propertyFile.getAbsolutePath());
                }
                props = new Properties();
                props.load(propertyResource.getInputStream());
            } else {
                // property is a system property
                props = System.getProperties();
            }
            String value = props.getProperty(propertyName);
            if (value != null) {
                on = Boolean.valueOf(value);
            }
        } catch (IOException e) {
            logger.error("could not load switch property file - filter is off", e);
            on = Boolean.FALSE;
        }
        dirty = false;
    }

    public boolean isOn() {
        return on.booleanValue();
    }

    /**
     * Default value of this predicate
     *
     * @org.apache.xbean.Property
     */
    public void setOn(boolean status) {
        on = Boolean.valueOf(status);
    }

    public String getPropertyName() {
        return propertyName;
    }

    /**
     * The name of the property to retrieve
     *
     * @org.apache.xbean.Property
     */
    public void setPropertyName(String newPropertyName) {
        propertyName = newPropertyName;
        dirty = true;
    }

    public Resource getPropertyResource() {
        return propertyResource;
    }

    /**
     * A spring resource to load a set of properties from.
     *
     * @org.apache.xbean.Property
     */
    public void setPropertyResource(Resource newPropertyResource) {
        propertyResource = newPropertyResource;
    }

    public boolean getFromExchange() {
        return fromExchange;
    }

    /**
     * Boolean flag indicating that the value is retrieved from the exchange properties.
     *
     * @org.apache.xbean.Property
     */
    public void setFromExchange(boolean fromExchange) {
        this.fromExchange = fromExchange;
    }

    public void createResource(String resource) {
        if (resource != null) {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource tempPropertyResource = resourceLoader.getResource(resource);
            if (tempPropertyResource != null && tempPropertyResource.exists()) {
                propertyResource = tempPropertyResource;
                dirty = true;
            }
        }
    }

    public String showResource() throws IOException {
        return propertyResource.getURL().toExternalForm();
    }

    /* (non-Javadoc)
    * @see org.apache.servicemix.components.eip.RoutingRule#matches(javax.jbi.messaging.MessageExchange)
    */
    public boolean matches(MessageExchange exchange) {
        if (dirty) {
            try {
                afterPropertiesSet();
            } catch (Exception e) {
                return false;
            }

        }
        Boolean match = Boolean.FALSE;
        if (fromExchange) {
            // property comes from exchange
            try {
                Object value = exchange.getProperty(propertyName);
                if (value != null) {
                    match = Boolean.valueOf(value.toString());
                }
            } catch (Exception e) {
                logger.warn("Could not evaluate xpath expression", e);
                match = Boolean.FALSE;
            }
        } else {
            // property comes in from external - use pre-evaluated value
            match = on;
        }
        return match.booleanValue();
    }
}
