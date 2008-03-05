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
package org.apache.servicemix.cxfbc.interceptors;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.servicemix.cxfbc.interceptors.types.quote.Quote;

import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcInterceptorTest extends SpringTestSupport {

    static final Logger LOG = Logger.getLogger(CxfBcInterceptorTest.class
            .getName());

    public void testEndpointRPCWithExternalConsumerWithMultipleResponeParts() throws Exception {
        
        QName stockServiceName = new QName("http://servicemix.apache.org/cxfbc/interceptors", 
                                      "StockQuoteReporterRPCService");
        URL wsdlUrl = CxfBcInterceptorTest.class.getClassLoader().getResource(
                "org/apache/servicemix/cxfbc/interceptors/quote.wsdl");
        StockQuoteReporterRPCService quoteService = new StockQuoteReporterRPCService(wsdlUrl, stockServiceName);
        
        
        QuoteReporterRPC port = quoteService.getStockQuoteReporterRPCPort();

        javax.xml.ws.Holder<java.lang.String> retailTicker =  
            new javax.xml.ws.Holder<java.lang.String>("RetailerRPC");                
        
        Quote quote = port.getStockQuote("FUSE", retailTicker);
        Thread.sleep(1000);
        //Thread.sleep(10 * 60 * 1000); 
        
        assertEquals("FUSE ESB", quote.getID());
        assertEquals("8.00", quote.getTime());
        assertEquals(26.0, quote.getVal(), 0);        
        System.out.println("retail val = " + retailTicker.value);
        assertEquals("IONA PLC", retailTicker.value);
    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/interceptors/xbean.xml");
    }


}
