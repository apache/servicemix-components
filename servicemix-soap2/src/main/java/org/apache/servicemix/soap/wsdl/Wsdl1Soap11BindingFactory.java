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
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;

import org.apache.servicemix.soap.bindings.soap.Soap11;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapBindingImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapMessageImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapOperationImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapPartImpl;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding.Style;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;

public class Wsdl1Soap11BindingFactory {

    public static Wsdl1SoapBinding createWsdl1SoapBinding(Port wsdlPort) {
        Wsdl1SoapBindingImpl binding = new Wsdl1SoapBindingImpl(Soap11.getInstance());
        // Find infos from port
        for (Iterator iter = wsdlPort.getExtensibilityElements().iterator(); iter.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) iter.next();
            if (element instanceof SOAPAddress) {
                SOAPAddress soapAddress = (SOAPAddress) element;
                binding.setLocationURI(soapAddress.getLocationURI());
            } else {
                //throw new IllegalStateException("Unrecognized extension: " + QNameUtil.toString(element.getElementType()));
            }
        }
        javax.wsdl.Binding wsdlBinding = wsdlPort.getBinding();
        for (Iterator iter = wsdlBinding.getExtensibilityElements().iterator(); iter.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) iter.next();
            if (element instanceof SOAPBinding) {
                SOAPBinding soapBinding = (SOAPBinding) element;
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
            SOAPOperation wsdlSoapOperation = WSDLUtils.getExtension(wsdlBindingOperation, SOAPOperation.class);
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
        SOAPBody wsdlSoapBody = WSDLUtils.getExtension(wsdlBindingInput, SOAPBody.class);
        List<SOAPHeader> wsdlSoapHeaders = WSDLUtils.getExtensions(wsdlBindingInput, SOAPHeader.class);
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
                if (operation.getStyle() == Style.DOCUMENT) {
                    input.setElementName(wsdlPart.getElementName());
                }
            } else {
                boolean header = false;
                for (SOAPHeader h : wsdlSoapHeaders) {
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
        SOAPBody wsdlSoapBody = WSDLUtils.getExtension(wsdlBindingOutput, SOAPBody.class);
        List<SOAPHeader> wsdlSoapHeaders = WSDLUtils.getExtensions(wsdlBindingOutput, SOAPHeader.class);
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
                for (SOAPHeader h : wsdlSoapHeaders) {
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
        if (WSDLUtils.WSDL1_STYLE_DOCUMENT.equalsIgnoreCase(str)) {
            return Style.DOCUMENT;
        } else if (WSDLUtils.WSDL1_STYLE_RPC.equalsIgnoreCase(str)) {
            return Style.RPC;
        } else {
            return null;
        }
    }
}
