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
package org.apache.servicemix.quartz.support;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.MessagingException;

/**
 * @version $Revision$
 */
public interface QuartzMarshaler {

    /**
     * Populates the given normalized message from the Quartz context
     *
     * @param message the normalized message to populate
     * @param context the Quartz context
     * @throws JobExecutionException
     * @throws MessagingException
     */
    void populateNormalizedMessage(NormalizedMessage message, JobExecutionContext context) throws JobExecutionException, MessagingException;
}
