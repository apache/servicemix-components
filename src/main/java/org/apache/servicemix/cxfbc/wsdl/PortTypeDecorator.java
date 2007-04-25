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
package org.apache.servicemix.cxfbc.wsdl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Fault;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPFaultImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12AddressImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12BindingImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12BodyImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12FaultImpl;
import com.ibm.wsdl.extensions.soap12.SOAP12OperationImpl;

public class PortTypeDecorator {

    public static Definition createImportDef(Definition definition,
                                             String targetNamespace,
                                             String importUri) throws Exception {
        // Create definition
        Definition def = WSDLFactory.newInstance().newDefinition();
        def.setTargetNamespace(targetNamespace);
        
        // Add namespaces
        Map namespaces = definition.getNamespaces();
        for (Iterator iter = namespaces.keySet().iterator(); iter.hasNext();) {
            String prefix = (String) iter.next();
            String uri = definition.getNamespace(prefix);
            def.addNamespace(prefix, uri);
        }
        def.addNamespace("tns", targetNamespace);
        def.addNamespace("tnspt", definition.getTargetNamespace());
        
        // Create import
        Import imp = def.createImport();
        imp.setNamespaceURI(definition.getTargetNamespace());
        imp.setLocationURI(importUri);
        imp.setDefinition(definition);
        def.addImport(imp);
        
        return def;
    }
    
    public static void decorate(Definition def,
                                PortType portType,
                                String locationUri) throws Exception {
        decorate(def,
                 portType,
                 locationUri,
                 portType.getQName().getLocalPart() + "Binding",
                 portType.getQName().getLocalPart() + "Service",
                 "JBI",
                 "1.1");
    }
    
    public static void decorate(Definition def,
                                PortType portType,
                                String locationUri,
                                String bindingName,
                                String serviceName,
                                String portName,
                                String soapVersion) throws Exception {
        boolean soap11 = "1.1".equals(soapVersion);
        if (soap11) {
            def.addNamespace("wsdlsoap", "http://schemas.xmlsoap.org/wsdl/soap/");
        } else {
            def.addNamespace("wsdlsoap", "http://schemas.xmlsoap.org/wsdl/soap12/");
        }
        // Create binding
        Binding binding = def.createBinding();
        binding.setQName(new QName(def.getTargetNamespace(), bindingName));
        binding.setPortType(portType);
        binding.setUndefined(false);
        // Create soap extension
        if (soap11) {
            SOAPBinding soap = new SOAPBindingImpl();
            soap.setTransportURI("http://schemas.xmlsoap.org/soap/http");
            soap.setStyle("document");
            binding.addExtensibilityElement(soap);
        } else {
            SOAP12Binding soap = new SOAP12BindingImpl();
            soap.setTransportURI("http://schemas.xmlsoap.org/soap/http");
            soap.setStyle("document");
            binding.addExtensibilityElement(soap);
        }
        // Create operations
        List operations = portType.getOperations();
        for (Iterator iter = operations.iterator(); iter.hasNext();) {
            Operation operation = (Operation) iter.next();
            BindingOperation bindingOp = def.createBindingOperation();
            bindingOp.setName(operation.getName());
            if (soap11) {
                SOAPOperation op = new SOAPOperationImpl();
                op.setSoapActionURI("");
                bindingOp.addExtensibilityElement(op);
            } else {
                SOAP12Operation op = new SOAP12OperationImpl();
                op.setSoapActionURI("");
                bindingOp.addExtensibilityElement(op);
            }
            if (operation.getInput() != null) {
                BindingInput in = def.createBindingInput();
                in.setName(operation.getInput().getName());
                if (soap11) {
                    SOAPBody body = new SOAPBodyImpl();
                    body.setUse("literal");
                    in.addExtensibilityElement(body);
                } else {
                    SOAP12Body body = new SOAP12BodyImpl();
                    body.setUse("literal");
                    in.addExtensibilityElement(body);
                }
                bindingOp.setBindingInput(in);
            }
            if (operation.getOutput() != null) {
                BindingOutput out = def.createBindingOutput();
                out.setName(operation.getOutput().getName());
                if (soap11) {
                    SOAPBody body = new SOAPBodyImpl();
                    body.setUse("literal");
                    out.addExtensibilityElement(body);
                } else {
                    SOAP12Body body = new SOAP12BodyImpl();
                    body.setUse("literal");
                    out.addExtensibilityElement(body);
                }
                bindingOp.setBindingOutput(out);
            }
            for (Iterator itf = operation.getFaults().values().iterator(); itf.hasNext();) {
                Fault fault = (Fault) itf.next();
                BindingFault bindingFault = def.createBindingFault();
                bindingFault.setName(fault.getName());
                if (soap11) {
                    SOAPFault soapFault = new SOAPFaultImpl();
                    soapFault.setUse("literal");
                    soapFault.setName(fault.getName());
                    bindingFault.addExtensibilityElement(soapFault);
                } else {
                    SOAP12Fault soapFault = new SOAP12FaultImpl();
                    soapFault.setUse("literal");
                    soapFault.setName(fault.getName());
                    bindingFault.addExtensibilityElement(soapFault);
                }
                bindingOp.addBindingFault(bindingFault);
            }
            binding.addBindingOperation(bindingOp);
        }
        def.addBinding(binding);
        // Create service
        Service service = def.createService();
        service.setQName(new QName(def.getTargetNamespace(), serviceName));
        Port port = def.createPort();
        port.setName(portName);
        port.setBinding(binding);
        if (soap11) {
            SOAPAddress address = new SOAPAddressImpl();
            address.setLocationURI(locationUri);
            port.addExtensibilityElement(address);
        } else {
            SOAP12Address address = new SOAP12AddressImpl();
            address.setLocationURI(locationUri);
            port.addExtensibilityElement(address);
        }
        service.addPort(port);
        def.addService(service);
    }
    
    public static Definition decorate(Definition definition,
                                      String importUri,
                                      String targetNamespace,
                                      String locationUri) throws Exception {
        // Create definition
        Definition def = createImportDef(definition, targetNamespace, importUri);
        
        // Iterator through port types
        for (Iterator it = definition.getPortTypes().values().iterator(); it.hasNext();) {
            PortType portType = (PortType) it.next();
            decorate(def, portType, locationUri);
        }
        return def;
    }
    
}
