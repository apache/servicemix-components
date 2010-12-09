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
package org.apache.servicemix.rmi.util;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.apache.cxf.BusFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.simple.SimpleServiceBuilder;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;

/**
 * <p>
 * A helper around WSDL generation.
 * </p>
 * 
 * @author jbonofre
 */
public class WsdlHelper {
    
    private Class bean;
    private SimpleServiceBuilder serviceBuilder;
    private DataBinding dataBinding = new AegisDatabinding();
    
    public WsdlHelper(Class bean) {
        this.bean = bean;
        serviceBuilder = new SimpleServiceBuilder();
        serviceBuilder.setServiceClass(bean);
        serviceBuilder.setDataBinding(dataBinding);
    }
    
    public DataBinding getDataBinding() {
        return this.dataBinding;
    }
    
    /**
     * <p>
     * Generate a WSDL definition based on the given class using CXF.
     * </p>
     * 
     * @param bean the base class to generate the WSDL.
     * @return the WSDL definition.
     */
    public Definition createDocument() throws WSDLException {
        ServiceInfo serviceInfo = serviceBuilder.createService();
        ServiceWSDLBuilder wsdlBuilder = new ServiceWSDLBuilder(BusFactory.getDefaultBus(), serviceInfo);
        return wsdlBuilder.build();
    }

}
