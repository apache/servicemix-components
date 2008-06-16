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

import java.util.concurrent.Future;

import javax.jbi.component.ComponentContext;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;
import uri.helloworld.HelloFault_Exception;
import uri.helloworld.HelloHeader;
import uri.helloworld.HelloPortType;
import uri.helloworld.HelloRequest;

@WebService(serviceName = "SOAPServiceProvider", 
        portName = "SoapPort", 
        endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
        targetNamespace = "http://apache.org/hello_world_soap_http")

public class GreeterImplForProvider implements Greeter {
    private ComponentContext context;
    private CalculatorPortType calculator;
    private Greeter greeter;
    private Greeter securityGreeter;
    private HelloPortType hello;

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
            } else if ("header test".equals(me)) { 
                HelloRequest req = new HelloRequest();
                req.setText("12");
                HelloHeader header = new HelloHeader();
                header.setId("345");
                ret = ret + hello.hello(req, header).getText();
            
            } else if ("https test".equals(me)) {
                ret = ret + securityGreeter.greetMe("ffang");
            } else if ("concurrency test".equals(me)) {
                MultiClientThread[] clients = new MultiClientThread[10];
                for (int i = 0; i < clients.length; i++) {
                    clients[i] = new MultiClientThread(getCalculator(), i);
                }
                
                for (int i = 0; i < clients.length; i++) {
                    clients[i].start();
                }
                
                for (int i = 0; i < clients.length; i++) {
                    clients[i].join();
                }
                
                for (int i = 0; i < clients.length; i++) {
                    ret += i * 2 + " ";
                }
            } else if ("ffang with no server".equals(me)) {
                //should catch exception since external server is stop
                getCalculator().add(1, 2);
            }
                        
        } catch (AddNumbersFault e) {
            //should catch exception here if negative number is passed
            ret = ret + e.getFaultInfo().getMessage();
        } catch (HelloFault_Exception e) {
            ret = ret + e.getFaultInfo().getId();
        } catch (InterruptedException e) {
            //
        } catch (Exception e) {
            ret = ret + "server is stop";
        }
        return "Hello " + me  + " " + ret;
    }

    public String greetMeLater(long requestType) {
        // TODO Auto-generated method stub
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

    public void setGreeter(Greeter greeter) {
        this.greeter = greeter;
    }

    public Greeter getGreeter() {
        return greeter;
    }

    public void setHello(HelloPortType hello) {
        this.hello = hello;
    }

    public HelloPortType getHello() {
        return hello;
    }

    public void setSecurityGreeter(Greeter securityGreeter) {
        this.securityGreeter = securityGreeter;
    }

    public Greeter getSecurityGreeter() {
        return securityGreeter;
    }

    public Response<GreetMeResponse> greetMeAsync(String requestType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
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
        // TODO Auto-generated method stub
        
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


    class MultiClientThread extends Thread {
        private CalculatorPortType port;
        private int index;
        private int ret;
        public MultiClientThread(CalculatorPortType port, int i) {
            this.port = port;
            this.index = i;
        }
        
        public void run() {
            try {
                ret = port.add(index, index);
            } catch (AddNumbersFault e) {
                //  
            }
        }
        
        public int getRet() {
            return ret;
        }
    }
}
