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
package org.apache.servicemix.quartz;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.quartz.support.DefaultQuartzMarshaler;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CustomMarshaler extends DefaultQuartzMarshaler {

    /* (non-Javadoc)
     * @see org.apache.servicemix.quartz.support.DefaultQuartzMarshaler#populateNormalizedMessage(javax.jbi.messaging.NormalizedMessage, org.quartz.JobExecutionContext)
     */
    @Override
    public void populateNormalizedMessage(NormalizedMessage message, JobExecutionContext context) throws JobExecutionException, MessagingException {
        super.populateNormalizedMessage(message, context);
        message.setContent(new StringSource((String) context.getJobDetail().getJobDataMap().get("xml"))); 
    }

}
