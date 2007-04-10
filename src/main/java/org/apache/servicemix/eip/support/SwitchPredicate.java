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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.beans.factory.InitializingBean;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * switch (on/off) predicate based on a property that can come from
 * 1. a system property
 * 2. a property from a property file (specified as Spring resource)
 * 3. a property from the exchange
 * 4. swtich on/off via JMX MBean (not yet implemented)
 * <p/>
 * the property is interpreted as a boolean value
 * If fromExchange is true --> 3.
 * If propertyResource is specified --> 2.
 * else --> 1.
 *
 * @org.apache.xbean.XBean element="switch-predicate"
 */
public class SwitchPredicate implements InitializingBean, Predicate {

    private static final Log log = LogFactory.getLog(SwitchPredicate.class);

    // property resource optionally
    private Resource propertyResource;

    // use property from exchange
    private boolean fromExchange = false;

    // property name
    private String propertyName = "on";

    private Boolean on = Boolean.FALSE;

    private File propertyFile = null;

    private boolean dirty = false;

    public void afterPropertiesSet() throws Exception {
        try {
            Properties props = null;
            if (propertyResource != null && propertyResource.exists()) {
                // get property from properties file
                propertyFile = propertyResource.getFile();
                if (log.isDebugEnabled()) {
                    log.debug("loading property file: " + propertyFile.getAbsolutePath());
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
            log.error("could not load switch property file - filter is off", e);
            on = Boolean.FALSE;
        }
        dirty = false;
    }

    public boolean isOn() {
        return on.booleanValue();
    }

    public void setOn(boolean status) {
        on = new Boolean(status);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String newPropertyName) {
        propertyName = newPropertyName;
        dirty = true;
    }

    public Resource getPropertyResource() {
        return propertyResource;
    }

    public void setPropertyResource(Resource newPropertyResource) {
        propertyResource = newPropertyResource;
    }

    public boolean getFromExchange() {
        return fromExchange;
    }

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
                NormalizedMessage in = exchange.getMessage("in");
                Object value = exchange.getProperty(propertyName);
                if (value != null) {
                    match = Boolean.valueOf(value.toString());
                }
            } catch (Exception e) {
                log.warn("Could not evaluate xpath expression", e);
                match = Boolean.FALSE;
            }
        } else {
            // property comes in from external - use pre-evaluated value
            match = on;
        }
        return match.booleanValue();
    }
}
