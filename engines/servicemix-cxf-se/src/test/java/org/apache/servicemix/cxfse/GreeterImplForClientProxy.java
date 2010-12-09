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

import java.io.InputStream;
import java.util.concurrent.Future;

import javax.activation.DataHandler;
import javax.jbi.component.ComponentContext;
import javax.jws.WebService;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.Response;

import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.mime.TestMtom;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;



@WebService(serviceName = "SOAPService", 
        portName = "SoapPort", 
        endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
        targetNamespace = "http://apache.org/hello_world_soap_http")

public class GreeterImplForClientProxy implements Greeter {

    private ComponentContext context;
    private CalculatorPortType calculator;
    private CalculatorPortType calculatorPayload;
    private TestMtom mtom;    
   

    public String greetMe(String me) {
        int ret = 0;
        try {
            if ("ffang".equals(me)) {
                ret = getCalculator().add(1, 2);
                return "Hello " + me  + " " + ret;
            } else if ("fault".equals(me)) {
                ret = getCalculator().add(-1, 2);
            } else if ("mtom".equals(me)) {
                Holder<DataHandler> param = new Holder<DataHandler>();
                
                param.value = new DataHandler(new ByteArrayDataSource("foobar".getBytes(), 
                    "application/octet-stream"));
                
                Holder<String> name = new Holder<String>("call detail");
                mtom.testXop(name, param);
                InputStream bis = param.value.getDataSource().getInputStream();
                byte b[] = new byte[10];
                bis.read(b, 0, 10);
                String attachContent = new String(b);
                return "Hello " + me  + " " + attachContent;
            } else if ("payload".equals(me)) {
                ret = getCalculatorPayload().add(1, 2);
                return "Hello " + me  + " " + ret;
            } else if ("property".equals(me)) {
                ((BindingProvider)mtom).getRequestContext().put("test-property", "Hello ");
                Holder<DataHandler> param = new Holder<DataHandler>();
                param.value = new DataHandler(new ByteArrayDataSource("foobar".getBytes(), 
                    "application/octet-stream"));
                Holder<String> name = new Holder<String>("property");
                mtom.testXop(name, param);
                return (String) ((BindingProvider)mtom).getResponseContext().get("test-property");
            } 
        } catch (AddNumbersFault e) {
            return "AddNumbersFault";
                        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        
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

    public Response<GreetMeResponse> greetMeAsync(String requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    public String greetMeLater(long requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Response<GreetMeLaterResponse> greetMeLaterAsync(long requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> greetMeLaterAsync(long requestType, AsyncHandler<GreetMeLaterResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    public void greetMeOneWay(String requestType) {
        System.out.println("greetMeOneWay get invoked");
        try {
            getCalculator().add(1, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public String greetMeSometime(String requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> greetMeSometimeAsync(String requestType, AsyncHandler<GreetMeSometimeResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    public String sayHi() {
        // TODO Auto-generated method stub
        return null;
    }

    public Response<SayHiResponse> sayHiAsync() {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    public void testDocLitFault(String faultType) throws BadRecordLitFault, NoSuchCodeLitFault {
        // TODO Auto-generated method stub
        
    }

    public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> testDocLitFaultAsync(String faultType, AsyncHandler<TestDocLitFaultResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    public String testNillable(String nillElem, int intElem) {
        // TODO Auto-generated method stub
        return null;
    }

    public Response<TestNillableResponse> testNillableAsync(String nillElem, int intElem) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> testNillableAsync(String nillElem, int intElem, AsyncHandler<TestNillableResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setMtom(TestMtom mtom) {
        this.mtom = mtom;
    }

    public TestMtom getMtom() {
        return mtom;
    }

    public void setCalculatorPayload(CalculatorPortType calculatorPayload) {
        this.calculatorPayload = calculatorPayload;
    }

    public CalculatorPortType getCalculatorPayload() {
        return calculatorPayload;
    }

}
