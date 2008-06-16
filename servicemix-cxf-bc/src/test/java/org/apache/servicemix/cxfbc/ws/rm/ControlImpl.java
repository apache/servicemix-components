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

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.FaultThrowingInterceptor;
import org.apache.cxf.greeter_control.types.FaultLocation;
import org.apache.cxf.greeter_control.types.StartGreeterResponse;
import org.apache.cxf.greeter_control.types.StopGreeterResponse;
import org.apache.cxf.interceptor.Interceptor;

@WebService(serviceName = "ControlService", 
            portName = "ControlPort", 
            endpointInterface = "org.apache.cxf.greeter_control.Control", 
            targetNamespace = "http://cxf.apache.org/greeter_control")
public class ControlImpl {

    private static final Logger LOG = LogUtils.getL7dLogger(ControlImpl.class);

    protected Object implementor;

    protected String address;

    protected Endpoint endpoint;

    protected Bus greeterBus;

    public void setImplementor(Object i) {
        implementor = i;
    }

    public Object getImplementor() {
        return implementor;
    }

    public void setAddress(String a) {
        address = a;
    }

    public String getAddress() {
        return address;
    }

    public boolean startGreeter(String cfgResource) {
        String derbyHome = System.getProperty("derby.system.home"); 
        try {
            System.setProperty("derby.system.home", derbyHome + "-server");   
            /*SpringBusFactory bf = new SpringBusFactory();
            greeterBus = bf.createBus(cfgResource);
            BusFactory.setDefaultBus(greeterBus);
            LOG.info("Initialised bus " + greeterBus + " with cfg file resource: " + cfgResource);
            LOG.fine("greeterBus inInterceptors: " + greeterBus.getInInterceptors());

            Interceptor logIn = new LoggingInInterceptor();
            Interceptor logOut = new LoggingOutInterceptor();
            greeterBus.getInInterceptors().add(logIn);
            greeterBus.getOutInterceptors().add(logOut);
            greeterBus.getOutFaultInterceptors().add(logOut);*/
            if (null == getImplementor()) {
                setImplementor(new GreeterImpl());
            }
            //this shouldn't happen in se
            //Endpoint.publish(address, implementor);
            LOG.info("Published greeter endpoint.");
        } finally {
            if (derbyHome != null) {
                System.setProperty("derby.system.home", derbyHome);
            } else {
                System.clearProperty("derby.system.home");
            }
        }
        
        return true;        
    }
    
    

    public boolean stopGreeter(String requestType) {
        LOG.fine("Stopping greeter");

        if (null != endpoint) {
            LOG.info("Stopping Greeter endpoint");
            endpoint.stop();
        } else {
            LOG.info("No endpoint active.");
        }
        endpoint = null;
        if (null != greeterBus) {
            greeterBus.shutdown(true);
        }
        greeterBus = null;
        return true;
    }

    public void setFaultLocation(FaultLocation fl) {
        List<Interceptor> interceptors = greeterBus.getInInterceptors();
        FaultThrowingInterceptor fi = null;
        for (Interceptor i : interceptors) {
            if (i instanceof FaultThrowingInterceptor) {
                interceptors.remove(i);
                LOG.fine("Removed existing FaultThrowingInterceptor");
                break;
            }
        }

        fi = new FaultThrowingInterceptor(fl.getPhase());
        if (null != fl.getBefore() && !"".equals(fl.getBefore())) {
            fi.addBefore(fl.getBefore());
        }
        if (null != fl.getAfter() && !"".equals(fl.getAfter())) {
            fi.addAfter(fl.getAfter());
        }

        interceptors.add(fi);
        LOG.fine("Added FaultThrowingInterceptor to phase " + fl.getPhase());
    }

    public Future<?> startGreeterAsync(String requestType,
            AsyncHandler<StartGreeterResponse> asyncHandler) {
        // never called
        return null;
    }

    public Response<StartGreeterResponse> startGreeterAsync(String requestType) {
        // never called
        return null;
    }

    public Response<StopGreeterResponse> stopGreeterAsync(String requestType) {
        // never called
        return null;
    }

    public Future<?> stopGreeterAsync(String requestType,
            AsyncHandler<StopGreeterResponse> asyncHandler) {
        // never called
        return null;
    }

  
}
