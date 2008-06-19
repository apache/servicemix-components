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
package org.apache.servicemix.common.xbean;

import javax.jbi.JBIException;

import org.apache.servicemix.common.ServiceUnit;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class XBeanServiceUnit extends ServiceUnit {

    private ClassLoader classLoader;
    private AbstractXmlApplicationContext applicationContext;

    public AbstractXmlApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(AbstractXmlApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.ServiceUnit#shutDown()
     */
    public void shutDown() throws JBIException {
        super.shutDown();
        classLoader = null;
        try {
            if (applicationContext != null) {
                applicationContext.destroy();
            }
            applicationContext = null;
        } catch (Exception e) {
            throw new JBIException("Unable to close application context", e);
        }
    }
    
    public ClassLoader getConfigurationClassLoader() {
        if (classLoader == null && applicationContext != null) {
            classLoader = applicationContext.getClassLoader();
        }
        ClassLoader cl = classLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }
    
}
