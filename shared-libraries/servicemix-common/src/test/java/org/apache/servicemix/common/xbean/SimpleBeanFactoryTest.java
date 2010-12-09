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

import junit.framework.TestCase;
import org.apache.servicemix.common.Container;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.xbean.classloader.JarFileClassLoader;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.io.File;
import java.net.URL;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Test cases for {@link org.apache.servicemix.common.xbean.SimpleBeanFactory}
 */
public class SimpleBeanFactoryTest extends TestCase {

    private static final String BEAN1_NAME = "bean1";
    private static final String BEAN2_NAME = "bean2";
    private static final String BEAN3_NAME = "bean3";

    private static final String BEAN1 = "A simple bean object";
    private static final String BEAN2 = "Another bean object";
    private static final Integer BEAN3 = 100;

    private BeanFactory factory;

    protected void setUp() throws Exception {
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put(BEAN1_NAME, BEAN1);
        beans.put(BEAN2_NAME, BEAN2);
        beans.put(BEAN3_NAME, BEAN3);

        factory = new SimpleBeanFactory(beans);
    }

    public void testGetBeanByType() throws Exception {
        assertEquals(BEAN3, factory.getBean(Integer.class));

        try {
            factory.getBean(String.class);
            fail("Should have thrown NoSuchBeanDefinitionException: two beans of matching type available");
        } catch (NoSuchBeanDefinitionException e) {
            // this is OK
        }

        try {
            factory.getBean(Currency.class);
            fail("Should have thrown NoSuchBeanDefinitionException: no bean of matching type available");
        } catch (NoSuchBeanDefinitionException e) {
            // this is OK
        }
    }
}
