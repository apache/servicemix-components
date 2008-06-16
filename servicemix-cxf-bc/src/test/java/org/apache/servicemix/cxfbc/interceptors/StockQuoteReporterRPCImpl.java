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

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


@javax.jws.WebService(endpointInterface = "org.apache.servicemix.cxfbc.interceptors.QuoteReporterRPC",
                targetNamespace = "http://servicemix.apache.org/cxfbc/interceptors",
                portName = "StockQuoteReporterRPCPort",
                serviceName = "StockQuoteReporterRPCService",
                wsdlLocation = "C:/fuse-esb-3.3.0.6/examples/cxf-java-first-rpc/cxf-server-rpc/src/main/resources/quote.wsdl")

public class StockQuoteReporterRPCImpl implements QuoteReporterRPC {

    private static final Logger LOG = LogUtils.getL7dLogger(StockQuoteReporterRPCImpl.class);

    public org.apache.servicemix.cxfbc.interceptors.types.quote.Quote 
    getStockQuote(java.lang.String stockTicker, 
                  javax.xml.ws.Holder<java.lang.String> retailTicker) {
        
        LOG.info("Executing operation getStockQuote");
        System.out.println(stockTicker);
        System.out.println(retailTicker.value);
        try {
            org.apache.servicemix.cxfbc.interceptors.types.quote.Quote quoteStock = 
                new org.apache.servicemix.cxfbc.interceptors.types.quote.Quote();
            quoteStock.setID("FUSE ESB");
            quoteStock.setTime("8.00");
            quoteStock.setVal(26);
            retailTicker.value = "IONA PLC";
            return quoteStock;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
}
