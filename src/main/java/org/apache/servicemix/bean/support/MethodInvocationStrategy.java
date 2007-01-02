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

import org.aopalliance.intercept.MethodInvocation;
import org.apache.servicemix.bean.BeanEndpoint;
import org.apache.servicemix.expression.Expression;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * @version $Revision: $
 */
public interface MethodInvocationStrategy {
    /**
     * Creates an invocation on the given POJO using annotations to decide which method to invoke
     * and to figure out which parameters to use
     */
    MethodInvocation createInvocation(Object pojo, BeanInfo beanInfo, MessageExchange messageExchange, BeanEndpoint pojoEndpoint) throws MessagingException;

    Expression getDefaultParameterTypeExpression(Class parameterType);
}
