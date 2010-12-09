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
package org.apache.servicemix.bean.support;

import org.apache.servicemix.bean.Endpoint;
import org.apache.servicemix.bean.beans.AutoDeployedBean;
import org.apache.servicemix.bean.beans.ListenerBean;
import org.apache.servicemix.bean.beans.PlainBean;
import org.apache.servicemix.bean.support.ResolverUtil.AnnotatedWith;
import org.apache.servicemix.bean.support.ResolverUtil.IsA;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;

import junit.framework.TestCase;

/**
 * Test cases for {@link ResolverUtil}
 */
public class ResolverUtilTest extends TestCase {
    
    public void testIsA() throws Exception {
        IsA test = new IsA(MessageExchangeListener.class);
        assertFalse(test.matches(PlainBean.class));
        assertTrue(test.matches(ListenerBean.class));
    }
    
    public void testAnnotatedWith() throws Exception {
        AnnotatedWith test = new AnnotatedWith(Endpoint.class);
        assertFalse(test.matches(PlainBean.class));
        assertTrue(test.matches(AutoDeployedBean.class));
    }
    
    public void testFindImplementations() throws Exception {
        ResolverUtil util = new ResolverUtil();
        util.findImplementations(MessageExchangeListener.class, "org.apache.servicemix.bean.beans");
        // should have found 3 implementations
        assertEquals(3, util.getClasses().size());
        // and make sure we don't break by omitting the package to search
        util.findImplementations(MessageExchangeListener.class, null);
        assertEquals(3, util.getClasses().size());
    }
    
    public void testFindImplementationsInJar() throws Exception {
        ResolverUtil util = new ResolverUtil();
        util.findImplementations(MessageExchangeListener.class, "org.apache.servicemix.common");
        // should have found 3 implementations
        assertNotNull(util.getClasses());
        assertFalse(util.getClasses().isEmpty());
    }
    
    public void testFindAnnotated() throws Exception {
        ResolverUtil util = new ResolverUtil();
        util.findAnnotated(Endpoint.class, "org.apache.servicemix.bean.beans");
        // should have found 1 implementation
        assertEquals(1, util.getClasses().size());
        // and make sure we don't break by omitting the package to search
        util.findAnnotated(Endpoint.class, null);
        assertEquals(1, util.getClasses().size());
    }
}
