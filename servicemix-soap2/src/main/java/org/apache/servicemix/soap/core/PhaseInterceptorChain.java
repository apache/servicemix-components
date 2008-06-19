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
package org.apache.servicemix.soap.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.soap.api.Interceptor;
import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.Message;

/**
 * A PhaseInterceptorChain orders Interceptors according 
 * to the before & after properties on an Interceptor.
 *
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class PhaseInterceptorChain implements InterceptorChain {

    private static final Log LOG = LogFactory.getLog(PhaseInterceptorChain.class);
    
    private final List<Interceptor> interceptors = new ArrayList<Interceptor>();
    
    public PhaseInterceptorChain() {
    }

    public void add(Iterable<? extends Interceptor> newhandlers) {
        if (newhandlers == null) {
            return;
        }
        for (Interceptor handler : newhandlers) {
            add(handler);
        }
    }
    
    public Iterable<Interceptor> getInterceptors() {
        return interceptors;
    }

    public void add(Interceptor i) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding interceptor " + i.getId());
        }
        insertInterceptor(i);
    }

    /**
     * Invokes each phase's handler in turn.
     * 
     * @param context
     * @throws Exception
     */
    public void doIntercept(Message message) {
        ListIterator<Interceptor> iterator = getState(message);
        
        if (iterator == null) {
            iterator = interceptors.listIterator();
            setState(message, iterator);
        }

        try {
            while (iterator.hasNext()) {
                Interceptor currentInterceptor = iterator.next();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invoking handleMessage on interceptor " + currentInterceptor.getId());
                }
                currentInterceptor.handleMessage(message);
            }
        } catch (RuntimeException ex) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Interceptor has thrown exception, unwinding now", ex);
            }
            message.setContent(Exception.class, ex);
            // Unwind
            while (iterator.hasPrevious()) {
                Interceptor currentInterceptor = iterator.previous();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invoking handleFault on interceptor " + currentInterceptor.getId());
                }
                currentInterceptor.handleFault(message);
            }
            throw ex;
        }
    }
    
    @SuppressWarnings("unchecked")
    protected ListIterator<Interceptor> getState(Message message) {
        Object state = message.get(this.toString());
        return (ListIterator<Interceptor>) state;
    }
    
    protected void setState(Message message, ListIterator<Interceptor> state) {
        message.put(this.toString(), state);
        message.put(InterceptorChain.class, this);
    }
    
    protected void insertInterceptor(Interceptor interc) {
        if (interceptors.size() == 0) {
            interceptors.add(interc);
            return;
        }
        int begin = -1;
        int end = interceptors.size();
        Collection before = interc.getBefore();
        Collection after = interc.getAfter();
        for (int i = 0; i < interceptors.size(); i++) {
            Interceptor cmp = interceptors.get(i);
            if (cmp.getId() == null) {
                continue;
            }
            if (before.contains(cmp.getId()) && i < end) {
                end = i;
            }
            if (cmp.getBefore().contains(interc.getId()) && i > begin) {
                begin = i;
            }
            if (after.contains(cmp.getId()) && i > begin) {
                begin = i;
            }
            if (cmp.getAfter().contains(interc.getId()) && i < end) {
                end = i;
            }
        }
        if (end < begin + 1) {
            throw new IllegalStateException("Invalid ordering for interceptor " + interc.getId());
        }
        interceptors.add(end, interc);
    }

    
}
