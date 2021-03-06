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
package org.apache.servicemix.cxfbc.ws.rm;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.types.FaultDetail;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.greeter_control.types.PingMeResponse;
import org.apache.cxf.greeter_control.types.SayHiResponse;


@WebService(serviceName = "GreeterService",
        portName = "GreeterPort",
        endpointInterface = "org.apache.cxf.greeter_control.Greeter",
        targetNamespace = "http://cxf.apache.org/greeter_control")
public class GreeterImpl {
    private static final Logger LOG = LogUtils.getL7dLogger(GreeterImpl.class);
    private long delay;
    private String lastOnewayArg;
    private boolean throwAlways;
    private boolean useLastOnewayArg;
    private int pingMeCount;
     
    public long getDelay() {
        return delay;
    }

    public void setDelay(long d) {
        delay = d;
    }

    public void resetLastOnewayArg() {
        lastOnewayArg = null;
    }

    public void useLastOnewayArg(Boolean use) {
        useLastOnewayArg = use;
    }

    public void setThrowAlways(boolean t) {
        throwAlways = t;
    }

    public String greetMe(String arg0) {
        LOG.fine("Executing operation greetMe with parameter: " + arg0);
        if ("twoway".equals(arg0)) {
            useLastOnewayArg(true);
            setDelay(5000);
        }
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        String result = null;
        synchronized (this) {
            result = useLastOnewayArg ? lastOnewayArg : arg0.toUpperCase();
        }
        LOG.fine("returning: " + result);
        return result;
    }

    public Future<?> greetMeAsync(String arg0, AsyncHandler<GreetMeResponse> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    public Response<GreetMeResponse> greetMeAsync(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public void greetMeOneWay(String arg0) {
        synchronized (this) {
            lastOnewayArg = arg0;
        }
        LOG.info("Executing operation greetMeOneWay with parameter: " + arg0);
    }

    public void pingMe() throws PingMeFault {
        pingMeCount++;
        if ((pingMeCount % 2) == 0 || throwAlways) {
            LOG.fine("Throwing PingMeFault while executiong operation pingMe");
            FaultDetail fd = new FaultDetail();
            fd.setMajor((short)2);
            fd.setMinor((short)1);
            throw new PingMeFault("Pings succeed only every other time.", fd);
        } else {
            LOG.fine("Executing operation pingMe");        
        }
    }

    public Response<PingMeResponse> pingMeAsync() {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> pingMeAsync(AsyncHandler<PingMeResponse> arg0) {
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

    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}

