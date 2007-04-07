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

import java.util.Iterator;
import java.util.Map;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.servicemix.components.util.MarshalerSupport;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * The default implementation of the Quartz marshaler
 *
 * @version $Revision$
 */
public class DefaultQuartzMarshaler extends MarshalerSupport implements QuartzMarshaler {

    public void populateNormalizedMessage(NormalizedMessage message, JobExecutionContext context) 
        throws JobExecutionException, MessagingException {

        JobDetail detail = context.getJobDetail();
        JobDataMap dataMap = detail.getJobDataMap();
        for (Iterator iter = dataMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            if (!key.equals(ServiceMixJob.COMPONENT_NAME) && !key.equals(ServiceMixJob.ENDPOINT_NAME)) {
                Object value = entry.getValue();
                message.setProperty(key, value);
            }
        }
        try {
            Document document = getTransformer().createDocument();
            Element root = document.createElement("timer");
            document.appendChild(root);
            DOMUtil.addChildElement(root, "name", detail.getName());
            DOMUtil.addChildElement(root, "group", detail.getGroup());
            DOMUtil.addChildElement(root, "fullname", detail.getFullName());
            DOMUtil.addChildElement(root, "description", detail.getDescription());
            DOMUtil.addChildElement(root, "fireTime", context.getFireTime());
            message.setContent(new DOMSource(document));
        } catch (ParserConfigurationException e) {
            throw new MessagingException("Failed to create content: " + e, e);
        }
    }

}
