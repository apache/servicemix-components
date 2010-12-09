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
import org.springframework.context.support.AbstractXmlApplicationContext;


public class CxfBcSchemaValidationTest extends CxfBcSpringTestSupport {

    private static final Logger LOG = LogUtils.getL7dLogger(CxfBcSchemaValidationTest.class);
    
    private static final java.net.URL WSDL_LOC;
    static {
        java.net.URL tmp = null;
        try {
            tmp = CxfBcSchemaValidationTest.class.getClassLoader().getResource(
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

    public void testSchemaValidationWithJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean-schema-validation.xml");
        schemaValidationBase();
    }
    
    public void testSchemaValidationWithoutJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean-schema-validation-withoutjbiwrapper.xml");
        schemaValidationBase();
    }
    
    public void testSchemaValidationWithoutJBIandSOAPWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean-schema-validation-withoutjbiandsoapwrapper.xml");
        schemaValidationBase();
    }
    
    public void schemaValidationBase() {
        LOG.info("test schema validation");
        
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http_provider",
                        "SOAPService"));
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http_provider",
                "SoapPort"), Greeter.class);
        
        String ret = greeter.greetMe("ffang");
        assertEquals(ret, "Hello ffang");
        try {
            ret = greeter.greetMe("schemavalidation");
            fail("should catch exception as schemavalidation failed");
        } catch(Exception ex) {
            //should catch exception as schemavalidation failed
            assertTrue(ex.getMessage().
                    indexOf("is not facet-valid with respect to maxLength '30' for type 'MyStringType'.") > 0);
        }
        try {
            ret = greeter.greetMe("should catch exception as schemavalidation failed");
            fail("should catch exception as schemavalidation failed");
        } catch(Exception ex) {
            //should catch exception as schemavalidation failed
            assertTrue(ex.getMessage().
                    indexOf("is not facet-valid with respect to maxLength '30' for type 'MyStringType'.") > 0);
        }
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/xbean-schema-validation.xml");
    }
    
    protected AbstractXmlApplicationContext createBeanFactory(String beanFile) {
        //load cxf se and bc from specified spring config file
        return new ClassPathXmlApplicationContext(
            beanFile);
    }

}
