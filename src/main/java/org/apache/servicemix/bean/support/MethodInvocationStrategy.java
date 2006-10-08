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
