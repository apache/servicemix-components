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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.servicemix.bean.Operation;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link BeanInfo}
 */
public class BeanInfoTest extends TestCase {
    
    public void testIntrospect() throws Exception {
        BeanInfo info = new BeanInfo(Pojo.class, new DefaultMethodInvocationStrategy());
        info.introspect();
        
        assertNotNull("Should find methods in superclass", 
                      createInvocation(info, "doSomethingElse"));
        assertNotNull("Should find methods based on the name in @Operation annotation", 
                      createInvocation(info, "doSomethingMoreSophisticated"));
        assertNull("Should not find method names that have @Operation information", 
                   createInvocation(info, "doSomething"));
    }

    private MethodInvocation createInvocation(BeanInfo info, String name) throws MessagingException {
        MessageExchange exchange = new MockMessageExchange();
        exchange.setOperation(new QName(name));
        return info.createInvocation(new Pojo(), exchange);
    }

    public static final class Pojo extends AbstractPojo {
        @Operation(name = "doSomethingMoreSophisticated")
        public void doSomething() {
            //just an operation
        }
    }
    
    public abstract static class AbstractPojo {

        public void doSomethingElse() {
            //just another operation
        }
    }
}
