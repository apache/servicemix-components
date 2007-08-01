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
package org.apache.servicemix.eip.support.resequence;

import java.net.URI;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;

import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.jbi.util.MessageCopier;
import org.apache.servicemix.jbi.util.MessageUtil;

public abstract class ResequencerBase extends EIPEndpoint {

    private MessageCopier messageCopier = new MessageCopier();

    private ExchangeTarget target;
    
    public MessageCopier getMessageCopier() {
        return messageCopier;
    }

    public ExchangeTarget getTarget() {
        return target;
    }

    public void setTarget(ExchangeTarget target) {
        this.target = target;
    }
    
    @Override
    public void validate() throws DeploymentException {
        super.validate();
        if (target == null) {
            throw new IllegalArgumentException("target must be set to a valid ExchangeTarget");
        }
    }
    
    public void validateMessageExchange(MessageExchange exchange) throws MessagingException {
        if ((exchange instanceof InOnly) || (exchange instanceof RobustInOnly)) {
            return;
        }
        fail(exchange, new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
    }

    protected MessageExchange createTargetExchange(NormalizedMessage message, URI exchangePattern) throws MessagingException {
        MessageExchange targetExchange = getExchangeFactory().createExchange(exchangePattern);
        target.configureTarget(targetExchange, getContext());
        MessageUtil.transferToIn(message, targetExchange);
        return targetExchange;
    }
    
}
