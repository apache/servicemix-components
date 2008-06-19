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
package org.apache.servicemix.soap.wsdl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Header;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;

import org.apache.servicemix.soap.bindings.soap.Soap12;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapBindingImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapMessageImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapOperationImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapPartImpl;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding.Style;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;

public class Wsdl1Soap12BindingFactory {

    public static final String STYLE_RPC = "rpc";
    public static final String STYLE_DOCUMENT = "document";
    
    public static Wsdl1SoapBinding createWsdl1SoapBinding(Port wsdlPort) {
        Wsdl1SoapBindingImpl binding = new Wsdl1SoapBindingImpl(Soap12.getInstance());
        // Find infos from port
        for (Iterator iter = wsdlPort.getExtensibilityElements().iterator(); iter.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) iter.next();
            if (element instanceof SOAP12Address) {
                SOAP12Address soapAddress = (SOAP12Address) element;
                binding.setLocationURI(soapAddress.getLocationURI());
            } else {
                //throw new IllegalStateException("Unrecognized extension: " + QNameUtil.toString(element.getElementType()));
            }
        }
        javax.wsdl.Binding wsdlBinding = wsdlPort.getBinding();
        for (Iterator iter = wsdlBinding.getExtensibilityElements().iterator(); iter.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) iter.next();
            if (element instanceof SOAP12Binding) {
                SOAP12Binding soapBinding = (SOAP12Binding) element;
                binding.setTransportURI(soapBinding.getTransportURI());
                binding.setStyle(getStyle(soapBinding.getStyle()));
            } else {
                //throw new IllegalStateException("Unrecognized extension: " + QNameUtil.toString(element.getElementType()));
            }
        }
        PortType wsdlPortType = wsdlBinding.getPortType();
        for (Iterator iter = wsdlPortType.getOperations().iterator(); iter.hasNext();) {
            Operation wsdlOperation = (Operation) iter.next();
            BindingOperation wsdlBindingOperation = wsdlBinding.getBindingOperation(wsdlOperation.getName(), null, null);
            SOAP12Operation wsdlSoapOperation = WSDLUtils.getExtension(wsdlBindingOperation, SOAP12Operation.class);
            Wsdl1SoapOperationImpl operation = new Wsdl1SoapOperationImpl();
            operation.setName(new QName(wsdlPortType.getQName().getNamespaceURI(), wsdlOperation.getName()));
            if (wsdlSoapOperation != null) {
                operation.setSoapAction(wsdlSoapOperation.getSoapActionURI());
                operation.setStyle(getStyle(wsdlSoapOperation.getStyle()));
            } else {
                operation.setSoapAction("");
            }
            if (operation.getStyle() == null) {
                operation.setStyle(binding.getStyle() != null ? binding.getStyle() : Style.DOCUMENT);
            }
            if (wsdlOperation.getStyle() == OperationType.ONE_WAY) {
                operation.setMep(JbiConstants.IN_ONLY);
            } else if (wsdlOperation.getStyle() == OperationType.REQUEST_RESPONSE) {
                operation.setMep(JbiConstants.IN_OUT);
            }
            
            // Create input
            createInput(operation, wsdlBindingOperation);
            // Create output
            createOutput(operation, wsdlBindingOperation);
            // Create faults
            Collection faults = wsdlOperation.getFaults().values();
            for (Iterator itFault = faults.iterator(); itFault.hasNext();) {
                Fault fault = (Fault) itFault.next();
                createFault(operation, wsdlBindingOperation.getBindingFaults().get(fault.getName()));
            }
            // Add operation
            binding.addOperation(operation);
        }
        
        return binding;
    }
    
    private static void createFault(Wsdl1SoapOperationImpl operation, Object object) {
        // TODO Auto-generated method stub
        
    }

    private static void createInput(Wsdl1SoapOperationImpl operation, BindingOperation wsdlBindingOperation) {
        Operation wsdlOperation = wsdlBindingOperation.getOperation();
        Input wsdlInput = wsdlOperation.getInput();
        if (wsdlInput == null) {
            return;
        }
        BindingInput wsdlBindingInput = wsdlBindingOperation.getBindingInput();
        SOAP12Body wsdlSoapBody = WSDLUtils.getExtension(wsdlBindingInput, SOAP12Body.class);
        List<SOAP12Header> wsdlSoapHeaders = WSDLUtils.getExtensions(wsdlBindingInput, SOAP12Header.class);
        Wsdl1SoapMessageImpl input = new Wsdl1SoapMessageImpl();
        input.setName(wsdlInput.getMessage().getQName());
        input.setNamespace(wsdlSoapBody.getNamespaceURI());
        String inputName = wsdlInput.getName();
        if (inputName == null || inputName.length() == 0) {
            inputName = wsdlOperation.getName();
        }
        input.setMessageName(inputName);
        
        for (Iterator itPart = wsdlInput.getMessage().getOrderedParts(null).iterator(); itPart.hasNext();) {
            Part wsdlPart = (Part) itPart.next();
            Wsdl1SoapPartImpl part = new Wsdl1SoapPartImpl();
            part.setName(wsdlPart.getName());
            part.setType(wsdlPart.getTypeName());
            part.setElement(wsdlPart.getElementName());
            if ((wsdlSoapBody.getParts() == null && wsdlInput.getMessage().getOrderedParts(null).size() == 1) ||
                    wsdlSoapBody.getParts().contains(part.getName())) {
                part.setBody(true);
                input.setElementName(wsdlPart.getElementName());
            } else {
                boolean header = false;
                for (SOAP12Header h : wsdlSoapHeaders) {
                    if (wsdlPart.getName().equals(h.getPart())) {
                        header = true;
                    }
                }
                if (header) {
                    part.setHeader(true);
                } else {
                    throw new IllegalStateException("Unbound part: " + part.getName());
                }
            }
            input.addPart(part);
        }
        if (operation.getStyle() == Style.RPC) {
            input.setElementName(new QName(input.getNamespace(), operation.getName().getLocalPart()));
        }
        operation.setInput(input);
    }
    
    private static void createOutput(Wsdl1SoapOperationImpl operation, BindingOperation wsdlBindingOperation) {
        Operation wsdlOperation = wsdlBindingOperation.getOperation();
        Output wsdlOutput = wsdlOperation.getOutput();
        if (wsdlOutput == null) {
            return;
        }
        BindingOutput wsdlBindingOutput = wsdlBindingOperation.getBindingOutput();
        SOAP12Body wsdlSoapBody = WSDLUtils.getExtension(wsdlBindingOutput, SOAP12Body.class);
        List<SOAP12Header> wsdlSoapHeaders = WSDLUtils.getExtensions(wsdlBindingOutput, SOAP12Header.class);
        Wsdl1SoapMessageImpl output = new Wsdl1SoapMessageImpl();
        output.setName(wsdlOutput.getMessage().getQName());
        output.setNamespace(wsdlSoapBody.getNamespaceURI());
        String outputName = wsdlOutput.getName();
        if (outputName == null || outputName.length() == 0) {
            outputName = wsdlOperation.getName() + "Response";
        }
        output.setMessageName(outputName);
        
        for (Iterator itPart = wsdlOutput.getMessage().getOrderedParts(null).iterator(); itPart.hasNext();) {
            Part wsdlPart = (Part) itPart.next();
            Wsdl1SoapPartImpl part = new Wsdl1SoapPartImpl();
            part.setName(wsdlPart.getName());
            part.setType(wsdlPart.getTypeName());
            part.setElement(wsdlPart.getElementName());
            if ((wsdlSoapBody.getParts() == null && wsdlOutput.getMessage().getOrderedParts(null).size() == 1) ||
                    wsdlSoapBody.getParts().contains(part.getName())) {
                part.setBody(true);
                output.setElementName(wsdlPart.getElementName());
            } else {
                boolean header = false;
                for (SOAP12Header h : wsdlSoapHeaders) {
                    if (wsdlPart.getName().equals(h.getPart())) {
                        header = true;
                    }
                }
                if (header) {
                    part.setHeader(true);
                } else {
                    throw new IllegalStateException("Unbound part: " + part.getName());
                }
            }
            output.addPart(part);
        }
        if (operation.getStyle() == Style.RPC) {
            output.setElementName(new QName(output.getNamespace(), operation.getName().getLocalPart() + "Response"));
        }
        operation.setOutput(output);
    }
    
    private static Style getStyle(String str) {
        if (STYLE_DOCUMENT.equalsIgnoreCase(str)) {
            return Style.DOCUMENT;
        } else if (STYLE_RPC.equalsIgnoreCase(str)) {
            return Style.RPC;
        } else {
            return null;
        }
    }
}
