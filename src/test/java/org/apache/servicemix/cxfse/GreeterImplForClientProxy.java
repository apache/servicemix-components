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
package org.apache.servicemix.cxfse;


import javax.jbi.component.ComponentContext;
import javax.jws.WebService;

import org.apache.cxf.calculator.CalculatorPortType;



@WebService(serviceName = "SOAPService", 
        portName = "SoapPort", 
        endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
        targetNamespace = "http://apache.org/hello_world_soap_http")

public class GreeterImplForClientProxy {

    private ComponentContext context;
    private CalculatorPortType calculator;
    public String greetMe(String me) {
        int ret = 0;
        try {
            
            ret = getCalculator().add(1, 2);
                        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Hello " + me  + " " + ret;
    }
    
    public ComponentContext getContext() {
        return context;
    }

    public void setContext(ComponentContext context) {
        this.context = context;
    }

    public void setCalculator(CalculatorPortType calculator) {
        this.calculator = calculator;
    }

    public CalculatorPortType getCalculator() {
        return calculator;
    }

    

}
