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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.jbi.FaultException;

public class Holder implements Future<NormalizedMessage> {
    
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