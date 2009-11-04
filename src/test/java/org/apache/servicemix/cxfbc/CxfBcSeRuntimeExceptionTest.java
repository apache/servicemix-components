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
package org.apache.servicemix.cxfbc;


import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.hello_world_soap_http_provider.Greeter;

import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;


public class CxfBcSeRuntimeExceptionTest extends SpringTestSupport {

    private static final Logger LOG = LogUtils.getL7dLogger(CxfBcSchemaValidationTest.class);
    
    private static final java.net.URL WSDL_LOC;
    static {
        java.net.URL tmp = null;
        try {
            tmp = CxfBcSeRuntimeExceptionTest.class.getClassLoader().getResource(
                "hello_world.wsdl"
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        WSDL_LOC = tmp;
        
    }
    
    public void setUp() throws Exception {
        //override super setup
        LOG.info("setUp is invoked");
    }
    
    public void setUpJBI(String beanFile) throws Exception {
        if (context != null) {
            context.refresh();
        }
        transformer = new SourceTransformer();
        if (beanFile == null) {
            context = createBeanFactory();
        } else {
            context = createBeanFactory(beanFile);
        }
                
        jbi = (SpringJBIContainer) context.getBean("jbi");
        assertNotNull("JBI Container not found in spring!", jbi);
    }
    
    public void tearDown() throws Exception {
        if (context != null) {
            context.destroy();
            context = null;
        }
        if (jbi != null) {
            jbi.shutDown();
            jbi.destroy();
            jbi = null;
        }
    }

    public void testRuntimeExceptionWithJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean-exception.xml");
        schemaValidationBase();
    }
    
    public void testRuntimeExceptionWithoutJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean-exception-withoutjbiwrapper.xml");
        schemaValidationBase();
    }
    
    public void testRuntimeExceptionJBIandSOAPWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean-exception-withoutjbiandsoapwrapper.xml");
        schemaValidationBase();
    }
    
    public void schemaValidationBase() throws Exception {
        LOG.info("test runtime exception");
        
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http_provider",
                        "SOAPService"));
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http_provider",
                "SoapPort"), Greeter.class);
        
        
        
        try {
            greeter.greetMe("runtime exception");
            fail("should catch runtime exception");
        } catch(Exception ex) {
            //should catch exception
            
            assertTrue(ex.getMessage().
                    indexOf("runtime exception") >= 0);
        }
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/xbean-exception.xml");
    }
    
    protected AbstractXmlApplicationContext createBeanFactory(String beanFile) {
        //load cxf se and bc from specified spring config file
        return new ClassPathXmlApplicationContext(
            beanFile);
    }

}
