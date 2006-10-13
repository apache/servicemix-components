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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.bean.support.BeanInfo;
import org.apache.servicemix.bean.support.DefaultMethodInvocationStrategy;
import org.apache.servicemix.bean.support.MethodInvocationStrategy;
import org.apache.servicemix.bean.support.ReflectionUtils;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.ProviderEndpoint;
import org.apache.servicemix.jbi.FaultException;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

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
    
    private Map<String, Holder> exchanges = new ConcurrentHashMap<String, Holder>();
    private Map<String, Request> requests = new ConcurrentHashMap<String, Request>();
    private ThreadLocal<Request> currentRequest = new ThreadLocal<Request>();
    private ComponentContext context;
    private DeliveryChannel channel;

    public BeanEndpoint() {
    }

    public BeanEndpoint(BeanComponent component, ServiceEndpoint serviceEndpoint) {
        super(component, serviceEndpoint);
        setBeanFactory(component.getBeanFactory());
    }

    public void start() throws Exception {
        super.start();
        context = new EndpointComponentContext(getServiceUnit().getComponent().getComponentContext());
        channel = context.getDeliveryChannel();

        if (getBean() == null) {
            throw new IllegalArgumentException("No 'bean' property set");
        }
        if (getMethodInvocationStrategy() == null) {
            throw new IllegalArgumentException("No 'methodInvocationStrategy' property set");
        }

        injectBean(getBean());
        // Call PostConstruct annotated methods
        ReflectionUtils.callLifecycleMethod(getBean(), PostConstruct.class);

        if (getBean() instanceof ExchangeProcessor) {
            ExchangeProcessor processor = (ExchangeProcessor) getBean();
            processor.start();
        }
    }


    public void stop() throws Exception {
        super.stop();

        if (getBean() instanceof ExchangeProcessor) {
            ExchangeProcessor processor = (ExchangeProcessor) getBean();
            processor.stop();
        }

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
        if (exchange.getRole() == MessageExchange.Role.CONSUMER) {
            onConsumerExchange(exchange);
        } else if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            onProviderExchange(exchange);
        } else {
            throw new IllegalStateException("Unknown role: " + exchange.getRole());
        }
    }
    
    protected void onProviderExchange(MessageExchange exchange) throws Exception {
        // Exchange is finished
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        }
        // Exchange has been aborted with an exception
        else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        // Fault message
        } else if (exchange.getFault() != null) {
            // TODO: find a way to send it back to the bean before setting the DONE status
            done(exchange);
        } else {
            onMessageExchange(exchange);
            done(exchange);
        }
    }
    
    protected void onConsumerExchange(MessageExchange exchange) throws Exception {
        Holder me = exchanges.get(exchange.getExchangeId());
        if (me == null) {
            throw new IllegalStateException("Consumer exchange not found");
        }
        me.set(exchange);
        evaluateCallbacks(requests.remove(exchange.getExchangeId()));
    }

    protected void onMessageExchange(MessageExchange exchange) throws Exception {
        Request req = new Request(getBean(), exchange);
        requests.put(exchange.getExchangeId(), req);
        currentRequest.set(req);
        Object pojo = req.getBean();
        if (pojo instanceof MessageExchangeListener) {
            MessageExchangeListener listener = (MessageExchangeListener) pojo;
            listener.onMessageExchange(exchange);
        }
        else if (pojo instanceof ExchangeProcessor) {
            ExchangeProcessor processor = (ExchangeProcessor) pojo;
            processor.process(exchange);
        }
        else {
            MethodInvocation invocation = getMethodInvocationStrategy().createInvocation(pojo, getBeanInfo(), exchange, this);
            if (invocation == null) {
                throw new UnknownMessageExchangeTypeException(exchange, this);
            }
            try {
                invocation.proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable throwable) {
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
    protected void injectBean(final Object bean) {
        // Inject fields
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            public void doWith(Field f) throws IllegalArgumentException, IllegalAccessException {
                ExchangeTarget et = f.getAnnotation(ExchangeTarget.class);
                if (et != null) {
                    ReflectionUtils.setField(f, bean, new DestinationImpl(et.uri()));
                }
                if (f.getAnnotation(Resource.class) != null) {
                    if (ComponentContext.class.isAssignableFrom(f.getType())) {
                        ReflectionUtils.setField(f, bean, context);
                    } else if (DeliveryChannel.class.isAssignableFrom(f.getType())) {
                        ReflectionUtils.setField(f, bean, channel);
                    }
                }
            }
        });
    }
    
    protected void evaluateCallbacks(Request req) {
        final Object bean = req.getBean();
        ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
            @SuppressWarnings("unchecked")
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                if (method.getAnnotation(Callback.class) != null) {
                    try {
                        Expression e = ExpressionFactory.createExpression(method.getAnnotation(Callback.class).condition());
                        JexlContext jc = JexlHelper.createContext();
                        jc.getVars().put("this", bean);
                        Object r = e.evaluate(jc);
                        if (r instanceof Boolean && ((Boolean) r).booleanValue()) {
                            Object o = method.invoke(bean, new Object[0]);
                            // TODO
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to invoke callback", e);
                    }
                }
            }
        });
    }
    
    public class DestinationImpl implements Destination {

        private final String uri;
        
        public DestinationImpl(String uri) {
            this.uri = uri;
        }
        
        public NormalizedMessage createMessage() {
            return new MessageUtil.NormalizedMessageImpl();
        }

        public Future<NormalizedMessage> send(NormalizedMessage message) {
            try {
                InOut me = getExchangeFactory().createInOutExchange();
                URIResolver.configureExchange(me, getServiceUnit().getComponent().getComponentContext(), uri);
                MessageUtil.transferTo(message, me, "in");
                return send(me);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        protected Future<NormalizedMessage> send(final MessageExchange me) throws Exception {
            final Holder h = new Holder(); 
            requests.put(me.getExchangeId(), currentRequest.get());
            exchanges.put(me.getExchangeId(), h);
            BeanEndpoint.this.send(me);
            return h;
        }
    }
    
    public static class Request {
        private Object bean;
        private MessageExchange exchange;
        private Set<String> sentExchanges = new HashSet<String>();
        
        public Request() {
        }
        
        public Request(Object bean, MessageExchange exchange) {
            this.bean = bean;
            this.exchange = exchange;
        }
        
        /**
         * @return the bean
         */
        public Object getBean() {
            return bean;
        }
        /**
         * @param bean the bean to set
         */
        public void setBean(Object bean) {
            this.bean = bean;
        }
        /**
         * @return the exchange
         */
        public MessageExchange getExchange() {
            return exchange;
        }
        /**
         * @param exchange the exchange to set
         */
        public void setExchange(MessageExchange exchange) {
            this.exchange = exchange;
        }
        /**
         * @param id the id of the exchange sent 
         */
        public void addSentExchange(String id) {
            sentExchanges.add(id);
        }
    }
    
    public static class Holder implements Future<NormalizedMessage> {
        
        private MessageExchange object;
        private boolean cancel;
        
        public synchronized NormalizedMessage get() throws InterruptedException, ExecutionException {
            if (object == null) {
                wait();
            }
            return extract(object);
        }
        public synchronized NormalizedMessage get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
            if (object == null) {
                wait(unit.toMillis(timeout));
            }
            return extract(object);
        }
        public synchronized void set(MessageExchange t) {
            object = t;
            notifyAll();
        }
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel = true;
            return false;
        }
        public boolean isCancelled() {
            return cancel;
        }
        public boolean isDone() {
            return object != null;
        }
        protected NormalizedMessage extract(MessageExchange me) throws ExecutionException {
            if (me.getStatus() == ExchangeStatus.ERROR) {
                throw new ExecutionException(me.getError());
            } else if (me.getFault() != null) {
                throw new ExecutionException(new FaultException("Fault occured", me, me.getFault()));
            } else {
                return me.getMessage("out");
            }
        }
    }
}
