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

import javax.jbi.component.ComponentContext;
import javax.jws.WebService;

import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.hello_world_soap_http.Greeter;

@WebService(serviceName = "SOAPServiceProvider", 
        portName = "SoapPort", 
        endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
        targetNamespace = "http://apache.org/hello_world_soap_http")

public class GreeterImplForProvider {
    private ComponentContext context;
    private CalculatorPortType calculator;
    private Greeter greeter;
    private Greeter securityGreeter;
    public String greetMe(String me) {
        String ret = "";
        
        try {
            if ("ffang".equals(me)) {
                ret = ret + getCalculator().add(1, 2);
            } else if ("exception test".equals(me)) {
                ret = ret + getCalculator().add(1, -1);
            } else if ("oneway test".equals(me)) {
                getGreeter().greetMeOneWay("oneway");
                ret = "oneway";
            } else if ("https test".equals(me)) {
                ret = ret + securityGreeter.greetMe("ffang");
            }
                        
        } catch (AddNumbersFault e) {
            //should catch exception here if negative number is passed
            ret = ret + e.getFaultInfo().getMessage();
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

    public void setGreeter(Greeter greeter) {
        this.greeter = greeter;
    }

    public Greeter getGreeter() {
        return greeter;
    }

    public void setSecurityGreeter(Greeter securityGreeter) {
        this.securityGreeter = securityGreeter;
    }

    public Greeter getSecurityGreeter() {
        return securityGreeter;
    }


}
