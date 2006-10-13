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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.bean.Content;
import org.apache.servicemix.bean.Property;
import org.apache.servicemix.bean.XPath;
import org.apache.servicemix.components.util.MessageHelper;
import org.apache.servicemix.expression.Expression;
import org.apache.servicemix.expression.JAXPStringXPathExpression;
import org.apache.servicemix.expression.PropertyExpression;
import org.apache.servicemix.jbi.messaging.PojoMarshaler;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the metadata about a bean type created via a combination of introspection and annotations together with some
 * useful sensible defaults
 *
 * @version $Revision: $
 */
public class BeanInfo {

    private static final Log log = LogFactory.getLog(BeanInfo.class);

    private Class type;
    private MethodInvocationStrategy strategy;
    private Map<String, MethodInfo> operations = new ConcurrentHashMap<String, MethodInfo>();
    private MethodInfo defaultExpression;


    public BeanInfo(Class type, MethodInvocationStrategy strategy) {
        this.type = type;
        this.strategy = strategy;
        introspect(type);
        if (operations.size() == 0) {
            Collection<MethodInfo> methodInfos = operations.values();
            for (MethodInfo methodInfo : methodInfos) {
                defaultExpression = methodInfo;
            }
        }
    }

    public MethodInvocation createInvocation(Object pojo, MessageExchange messageExchange) throws MessagingException {
        QName operation = messageExchange.getOperation();
        MethodInfo methodInfo = null;
        if (operation == null) {
            methodInfo = defaultExpression;
        }
        else {
            methodInfo = operations.get(operation.getLocalPart());
        }
        if (methodInfo != null) {
            return methodInfo.createMethodInvocation(pojo, messageExchange);
        }
        return null;
    }

    protected void introspect(Class type) {
        Method[] methods = type.getDeclaredMethods();
        for (Method method : methods) {
            introspect(type, method);
        }

        Class superclass = type.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            introspect(superclass);
        }
    }

    protected void introspect(Class type, Method method) {
        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        final Expression[] parameterExpressions = new Expression[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            Expression expression = createParameterUnmarshalExpression(type, method, parameterType, parameterAnnotations[i]);
            if (expression == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No expression available for method: " + method.toString() + " parameter: " + i + " so ignoring method");
                }
                return;
            }
            parameterExpressions[i] = expression;
        }

        // now lets add the method to the repository
        Expression parametersExpression = createMethodParametersExpression(parameterExpressions);
        operations.put(method.getName(), new MethodInfo(type, method, parametersExpression));
    }

    protected Expression createMethodParametersExpression(final Expression[] parameterExpressions) {
        return new Expression() {

            public Object evaluate(MessageExchange messageExchange, NormalizedMessage normalizedMessage) throws MessagingException {
                Object[] answer = new Object[parameterExpressions.length];
                for (int i = 0; i < parameterExpressions.length; i++) {
                    Expression parameterExpression = parameterExpressions[i];
                    answer[i] = parameterExpression.evaluate(messageExchange, normalizedMessage);
                }
                return answer;
            }
        };
    }

    /**
     * Creates an expression for the given parameter type if the parameter can be mapped automatically or null
     * if the parameter cannot be mapped due to unsufficient annotations or not fitting with the default type conventions.
     */
    protected Expression createParameterUnmarshalExpression(Class type, Method method, Class parameterType, Annotation[] parameterAnnotation) {
        // TODO look for a parameter annotation that converts into an expression
        for (Annotation annotation : parameterAnnotation) {
            Expression answer = createParameterUnmarshalExpressionForAnnotation(type, method, parameterType, annotation);
            if (answer != null) {
                return answer;
            }
        }

        return strategy.getDefaultParameterTypeExpression(parameterType);
    }

    protected Expression createParameterUnmarshalExpressionForAnnotation(Class type, Method method, Class parameterType, Annotation annotation) {
        if (annotation instanceof Property) {
            Property propertyAnnotation = (Property) annotation;
            return new PropertyExpression(propertyAnnotation.name());
        }
        else if (annotation instanceof Content) {
            Content content = (Content) annotation;
            final PojoMarshaler marshaller = newInstance(content);
            return createContentExpression(marshaller);
        }
        else if (annotation instanceof XPath) {
            XPath xpathAnnotation = (XPath) annotation;
            return new JAXPStringXPathExpression(xpathAnnotation.xpath());
        }
        return null;
    }

    protected Expression createContentExpression(final PojoMarshaler marshaller) {
        return new Expression() {
            public Object evaluate(MessageExchange exchange, NormalizedMessage message) throws MessagingException {
                return MessageHelper.getBody(message, marshaller);
            }
        };
    }

    protected PojoMarshaler newInstance(Content content) {
        try {
            return content.marshalType().newInstance();
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
