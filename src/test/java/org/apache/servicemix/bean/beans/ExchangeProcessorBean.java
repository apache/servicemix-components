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
package org.apache.servicemix.bean.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.ExchangeProcessor;

import javax.jbi.messaging.MessageExchange;

/**
 * A simple POJO which implements the {@link ExchangeProcessor} interface
 *
 * @version $Revision: $
 */
public class ExchangeProcessorBean implements ExchangeProcessor {

    private static final Log log = LogFactory.getLog(ExchangeProcessorBean.class);

    private MessageExchange lastExchange;
    private String param;

    public void process(MessageExchange messageExchange) throws Exception {
        this.lastExchange = messageExchange;

        log.info("Received exchange: " + messageExchange);
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

    public MessageExchange getLastExchange() {
        return lastExchange;
    }


    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }


}
