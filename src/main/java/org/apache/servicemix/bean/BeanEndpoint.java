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
package org.apache.servicemix.bean;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.common.ProviderEndpoint;
import org.apache.servicemix.bean.support.BeanInfo;
import org.apache.servicemix.bean.support.DefaultMethodInvocationStrategy;
import org.apache.servicemix.bean.support.MethodInvocationStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;

/**
 * Represents a bean endpoint which consists of a together with a {@link MethodInvocationStrategy}
 * so that JBI message exchanges can be invoked on the bean.
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="endpoint"
 */
public class BeanEndpoint extends ProviderEndpoint implements BeanFactoryAware {
    private BeanFactory beanFactory;
    private String beanName;
    private Object bean;
    private BeanInfo beanInfo;
    private MethodInvocationStrategy methodInvocationStrategy;

    public BeanEndpoint() {
    }

    public BeanEndpoint(BeanComponent component, ServiceEndpoint serviceEndpoint) {
        super(component, serviceEndpoint);
        setBeanFactory(component.getBeanFactory());
    }

    public void start() throws Exception {
        super.start();

        if (getBean() == null) {
            throw new IllegalArgumentException("No 'bean' property set");
        }
        if (getMethodInvocationStrategy() == null) {
            throw new IllegalArgumentException("No 'methodInvocationStrategy' property set");
        }

        // TODO invoke the bean's lifecycle methods for @PostConstruct
        // could use Spring to do this?
        injectBean(getBean());
    }


    public void stop() throws Exception {
        super.stop();

        // TODO invoke the beans destroy methods for @PreDestroy

        // lets allow garbage collection to take place
        bean = null;
    }


    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Object getBean() {
        if (bean == null) {
            bean = createBean();
        }
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }


    public BeanInfo getBeanInfo() {
        if (beanInfo == null) {
            beanInfo = new BeanInfo(getBean().getClass(), getMethodInvocationStrategy());
        }
        return beanInfo;
    }

    public void setBeanInfo(BeanInfo beanInfo) {
        this.beanInfo = beanInfo;
    }

    public MethodInvocationStrategy getMethodInvocationStrategy() {
        if (methodInvocationStrategy == null) {
            methodInvocationStrategy = createMethodInvocationStrategy();
        }
        return methodInvocationStrategy;
    }

    public void setMethodInvocationStrategy(MethodInvocationStrategy methodInvocationStrategy) {
        this.methodInvocationStrategy = methodInvocationStrategy;
    }


    public void process(MessageExchange exchange) throws Exception {
        // TODO refactor this validation code back up into the base class?

        // The component acts as a provider, this means that another component has requested our service
        // As this exchange is active, this is either an in or a fault (out are sent by this component)
        if (exchange.getRole() != MessageExchange.Role.PROVIDER) {
            throw new IllegalStateException("Unsupported role: " + exchange.getRole());
        }

        // Exchange is finished
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
            // Exchange has been aborted with an exception
        }
        else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }

        // Fault message
        if (exchange.getFault() != null) {
            done(exchange);
            return;
        }
        try {
            onMessageExchange(exchange);
            done(exchange);
        }
        catch (Exception e) {
            fail(exchange, e);
        }
    }

    protected void onMessageExchange(MessageExchange exchange) throws Exception {
        Object pojo = getBean();
        if (pojo instanceof MessageExchangeListener) {
            MessageExchangeListener listener = (MessageExchangeListener) pojo;
            listener.onMessageExchange(exchange);
        }
        else {
            MethodInvocation invocation = getMethodInvocationStrategy().createInvocation(pojo, getBeanInfo(), exchange, this);
            if (invocation == null) {
                throw new UnknownMessageExchangeTypeException(exchange, this);
            }
            try {
                invocation.proceed();
            }
            catch (Exception e) {
                throw e;
            }
            catch (Throwable throwable) {
                throw new MethodInvocationFailedException(pojo, invocation, exchange, this, throwable);
            }
        }
    }

    protected Object createBean() {
        if (beanName == null) {
            throw new IllegalArgumentException("Property 'beanName' has not been set!");
        }
        Object answer = beanFactory.getBean(beanName);
        if (answer == null) {
            throw new NoSuchBeanException(beanName, this);
        }
        return answer;
    }

    protected MethodInvocationStrategy createMethodInvocationStrategy() {
        return new DefaultMethodInvocationStrategy();
    }

    /**
     * A strategy method to allow implementations to perform some custom JBI based injection of the POJO
     *
     * @param bean the bean to be injected
     */
    protected void injectBean(Object bean) {
    }
}
