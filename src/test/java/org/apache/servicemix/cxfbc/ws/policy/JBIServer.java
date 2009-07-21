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
package org.apache.servicemix.cxfbc.ws.policy;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class JBIServer extends AbstractBusTestServerBase {
    
    protected AbstractXmlApplicationContext context;

    protected SpringJBIContainer jbi;
    
    public static void main(String[] args) {
        new JBIServer().start();
    }
    
  
    
   
    
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/ws/policy/xbean.xml");

    }

    @Override
    protected void run() {
        context = createBeanFactory();
        jbi = (SpringJBIContainer) context.getBean("jbi");
        
    }


}
