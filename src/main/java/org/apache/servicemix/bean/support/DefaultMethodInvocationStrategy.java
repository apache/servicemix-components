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
import org.apache.servicemix.expression.Expression;
import org.apache.servicemix.bean.BeanEndpoint;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the strategy used to figure out how to map a JBI message exchange to a POJO method invocation
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="defaultMethodInvocationStrategy" description="The default strategy for invoking methods on POJOs from a JBI message exchange"
 */
public class DefaultMethodInvocationStrategy implements MethodInvocationStrategy {

    private Map<Class, Expression> parameterTypeToExpressionMap = new ConcurrentHashMap<Class, Expression>();

    public DefaultMethodInvocationStrategy() {
        loadDefaultRegistry();
    }


    public Expression getDefaultParameterTypeExpression(Class parameterType) {
        return parameterTypeToExpressionMap.get(parameterType);
    }

    /**
     * Adds a default parameter type mapping to an expression
     */
    public void addParameterMapping(Class parameterType, Expression expression) {
        parameterTypeToExpressionMap.put(parameterType, expression);
    }


    /**
     * Creates an invocation on the given POJO using annotations to decide which method to invoke
     * and to figure out which parameters to use
     */
    public MethodInvocation createInvocation(Object pojo, BeanInfo beanInfo, MessageExchange messageExchange, BeanEndpoint pojoEndpoint) throws MessagingException {
        return beanInfo.createInvocation(pojo, messageExchange);
    }


    protected void loadDefaultRegistry() {
        addParameterMapping(MessageExchange.class, new Expression() {
            public Object evaluate(MessageExchange messageExchange, NormalizedMessage normalizedMessage) throws MessagingException {
                return messageExchange;
            }
        });

        addParameterMapping(NormalizedMessage.class, new Expression() {
            public Object evaluate(MessageExchange messageExchange, NormalizedMessage normalizedMessage) throws MessagingException {
                return normalizedMessage;
            }
        });

        addParameterMapping(Source.class, new Expression() {
            public Object evaluate(MessageExchange messageExchange, NormalizedMessage normalizedMessage) throws MessagingException {
                return normalizedMessage.getContent();
            }
        });
    }


}
