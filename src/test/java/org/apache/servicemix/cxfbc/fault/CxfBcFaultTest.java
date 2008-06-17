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
package org.apache.servicemix.cxfbc.fault;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.servicemix.samples.wsdl_first.Person;
import org.apache.servicemix.samples.wsdl_first.PersonService;
import org.apache.servicemix.samples.wsdl_first.UnknownPersonFault;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcFaultTest extends SpringTestSupport {

    public void testFault() throws Exception {
        QName serviceName = new QName("http://servicemix.apache.org/samples/wsdl-first", 
                "PersonService");
        URL wsdlUrl = CxfBcFaultTest.class.getClassLoader().getResource(
                "org/apache/servicemix/cxfbc/fault/person.wsdl");
        PersonService service = new PersonService(wsdlUrl, serviceName);
        Person person = service.getSoap();
        Holder<String> personId = new Holder<String>();
        Holder<String> ssn = new Holder<String>();
        Holder<String> name = new Holder<String>();
        personId.value = "";
        try {
            person.getPerson(personId, ssn, name);
            fail();
        } catch (UnknownPersonFault e) {
            //should catch this Fault
        }
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/fault/xbean.xml");
    }

}
