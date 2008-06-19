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
package org.apache.servicemix.soap.wsdl.validator;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPHeaderFault;
import javax.wsdl.extensions.soap.SOAPOperation;

import org.apache.servicemix.soap.util.QNameUtil;
import org.apache.servicemix.soap.wsdl.WSDLUtils;

public class WSIBPValidator {
    
    public enum Code {
        R2201, 
        R2204, 
        R2205, 
        R2210,
        R2303,
        R2304,
        R2306,
        R2401,
        R2701,
        R2702, 
        R2705,
        R2706, 
        R2710,
        R2716, 
        R2717,
        R2718, 
        R2720,
        R2721,
        R2726,
        R2754, 
    }
    
    public enum Style {
        RPC,
        DOCUMENT,
    }
    
    private Definition definition;
    private Set<String> errors;
    private ResourceBundle bundle;
    
    public WSIBPValidator(Definition definition) {
        this.definition = definition;
        this.errors = new HashSet<String>();
        this.bundle = ResourceBundle.getBundle("org.apache.servicemix.soap.wsdl.validator.WSIBP");
    }
    
    protected void error(Code code, Binding binding) {
        error(code, "binding", QNameUtil.toString(binding.getQName()));
    }
    
    protected void error(Code code, PortType portType) {
        error(code, "port-type", QNameUtil.toString(portType.getQName()));
    }
    
    protected void error(Code code, Message message) {
        error(code, "message", QNameUtil.toString(message.getQName()));
    }
    
    protected void error(Code code, String locType, String loc) {
        StringBuffer buf = new StringBuffer();
        buf.append(locType);
        buf.append(" \"");
        buf.append(loc);
        buf.append("\" : ");
        buf.append("Basic Profile Violation #");
        buf.append(code);
        buf.append(": ");
        buf.append(bundle.getString(code.toString()));
        errors.add(buf.toString());
    }
    
    public boolean isValid() {
        for (Method m : getClass().getMethods()) {
            if (m.getName().startsWith("check")) {
                try {
                    m.invoke(this, new Object[] {});
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return errors.size() == 0;
    }
    
    public Collection<String> getErrors() {
        return errors;
    }

    public void checkPortTypes() {
        for (Iterator itPt = definition.getPortTypes().values().iterator(); itPt.hasNext();) {
            PortType portType = (PortType) itPt.next();
            Set<String> operationNames = new HashSet<String>();
            for (Iterator itOp = portType.getOperations().iterator(); itOp.hasNext();) {
                Operation operation = (Operation) itOp.next();
                if (operation.getStyle() != OperationType.ONE_WAY && operation.getStyle() != OperationType.REQUEST_RESPONSE) {
                    error(Code.R2303, portType);
                }
                if (!operationNames.add(operation.getName())) {
                    error(Code.R2304, portType);
                }
            }
        }
    }
    
    public void checkMessages() {
        for (Iterator itMsg = definition.getMessages().values().iterator(); itMsg.hasNext();) {
            Message msg = (Message) itMsg.next();
            for (Iterator it2 = msg.getParts().values().iterator(); it2.hasNext();) {
                Part p = (Part) it2.next();
                if (p.getTypeName() != null && p.getElementName() != null) {
                    error(Code.R2306, msg);
                }
            }
        }
    }
    
    public void checkBindings() {
        for (Iterator itBd = definition.getBindings().values().iterator(); itBd.hasNext();) {
            Binding binding = (Binding) itBd.next();
            SOAPBinding soapBinding = WSDLUtils.getExtension(binding, SOAPBinding.class);
            
            // R2401
            if (soapBinding == null) {
                error(Code.R2401, binding);
                continue;
            } 
            
            // R2701 R2702
            if (soapBinding.getTransportURI() == null) {
                error(Code.R2701, binding);
            } else if (!"http://schemas.xmlsoap.org/soap/http".equals(soapBinding.getTransportURI())) {
                error(Code.R2702, binding);
            }
            
            // R2706
            for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                BindingOperation bop = (BindingOperation) itBop.next();
                if (!ensureLiteral(bop)) {
                    error(Code.R2706, binding);
                    break;
                }
            }
            
            // R2705
            Style bindingStyle = null;
            for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                BindingOperation bop = (BindingOperation) itBop.next();
                SOAPOperation soapBop = WSDLUtils.getExtension(bop, SOAPOperation.class);
                Style opStyle = soapBop != null ? getStyle(soapBop.getStyle()) : null;
                if (opStyle == null) {
                    opStyle = getStyle(soapBinding.getStyle());
                    if (opStyle == null) {
                        opStyle = Style.DOCUMENT;
                    }
                }
                if (bindingStyle == null) {
                    bindingStyle = opStyle;
                } else if (bindingStyle != opStyle) {
                    error(Code.R2705, binding);
                    break;
                }
            }
            
            // R2201, R2204, R2209
            if (bindingStyle == Style.DOCUMENT) {
                for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                    BindingOperation bop = (BindingOperation) itBop.next();
                    validateDocLitBodyParts(binding, 
                                            WSDLUtils.getExtension(bop.getBindingInput(), SOAPBody.class),
                                            bop.getOperation().getInput().getMessage());
                    validateDocLitBodyParts(binding, 
                                            WSDLUtils.getExtension(bop.getBindingOutput(), SOAPBody.class),
                                            bop.getOperation().getOutput().getMessage());
                }
            }
            
            // R2203
            if (bindingStyle == Style.RPC) {
                
            }
            
            // R2205
            for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                BindingOperation bop = (BindingOperation) itBop.next();
                validateHeaderParts(binding,
                                    WSDLUtils.getExtensions(bop.getBindingInput(), SOAPHeader.class),
                                    bop.getOperation().getInput().getMessage());
                if (bop.getOperation().getOutput() != null) {
                    validateHeaderParts(binding,
                                        WSDLUtils.getExtensions(bop.getBindingOutput(), SOAPHeader.class),
                                        bop.getOperation().getOutput().getMessage());
                }
                /*
                for (BindingFault fault : getBindingFaults(bop)) {
                    validateFaultParts(binding,
                                       getExtension(fault, SOAPFault.class),
                                       bop.getOperation().getFault(fault.getName()).getMessage());
                }
                */
            }
            
            // R2716
            if (bindingStyle == Style.DOCUMENT) {
                for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                    BindingOperation bop = (BindingOperation) itBop.next();
                    if (!ensureNamespace(bop, false, false) || !ensureNamespace(bop, true, false)) {
                        error(Code.R2716, binding);
                        break;
                    }
                }
            }
            // R2717
            if (bindingStyle == Style.RPC) {
                for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                    BindingOperation bop = (BindingOperation) itBop.next();
                    if (!ensureNamespace(bop, true, true)) {
                        error(Code.R2717, binding);
                        break;
                    }
                }
            }
            // R2726
            if (bindingStyle == Style.RPC) {
                for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                    BindingOperation bop = (BindingOperation) itBop.next();
                    if (!ensureNamespace(bop, false, false)) {
                        error(Code.R2726, binding);
                        break;
                    }
                }
            }
            
            // R2718
            {
                Set<String> opNames = new HashSet<String>();
                for (Iterator itOp = binding.getPortType().getOperations().iterator(); itOp.hasNext();) {
                    opNames.add(((Operation) itOp.next()).getName());
                }
                Set<String> bopNames = new HashSet<String>();
                for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                    bopNames.add(((BindingOperation) itBop.next()).getName());
                }
                if (!opNames.equals(bopNames)) {
                    error(Code.R2718, binding);
                }
            }
            
            // R2720
            for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                BindingOperation bop = (BindingOperation) itBop.next();
                List<ElementExtensible> els = WSDLUtils.getElements(bop);
                boolean error = false;
                for (SOAPHeader sh : WSDLUtils.getExtensions(els, SOAPHeader.class)) {
                    error |= (sh.getPart() == null);
                    for (SOAPHeaderFault shf : WSDLUtils.getSOAPHeaderFaults(sh)) {
                        error |= (shf.getPart() == null);
                    }
                }
                if (error) {
                    error(Code.R2720, binding);
                }
            }
            
            // R2721, R2754
            for (Iterator itBop = binding.getBindingOperations().iterator(); itBop.hasNext();) {
                BindingOperation bop = (BindingOperation) itBop.next();
                for (BindingFault fault : WSDLUtils.getBindingFaults(bop)) {
                    for (SOAPFault sf : WSDLUtils.getExtensions(fault, SOAPFault.class)) {
                        if (sf.getName() == null) {
                            error(Code.R2721, binding);
                        } else if (!sf.getName().equals(fault.getName())) {
                            error(Code.R2754, binding);
                        }
                    }
                }
            }
        }
    }
    
    private void validateHeaderParts(Binding binding, List<SOAPHeader> headers, Message message) {
        for (SOAPHeader sh : headers) {
            if (message.getPart(sh.getPart()).getElementName() == null) {
                error(Code.R2205, binding);
            }
            for (SOAPHeaderFault shf : WSDLUtils.getSOAPHeaderFaults(sh)) {
                if (message.getPart(shf.getPart()).getElementName() == null) {
                    error(Code.R2205, binding);
                }
            }
        }
    }

    private void validateDocLitBodyParts(Binding binding, SOAPBody body, Message message) {
        if (body != null) {
            if (body.getParts() == null) {
                if (message.getParts().size() > 1) {
                    error(Code.R2210, binding);
                }
            } else {
                if (body.getParts().size() > 1) {
                    error(Code.R2201, binding);
                }
                for (String p : WSDLUtils.getParts(body)) {
                    if (message.getPart(p).getElementName() == null) {
                        error(Code.R2204, binding);
                    }
                }
            }
        }
    }
    
    private boolean ensureLiteral(BindingOperation operation) {
        // R2707: use attribute defaults to "literal" 
        List<ElementExtensible> els = WSDLUtils.getElements(operation);
        for (SOAPBody sb : WSDLUtils.getExtensions(els, SOAPBody.class)) {
            if (sb.getUse() == null || !WSDLUtils.WSDL1_USE_LITERAL.equals(sb.getUse())) {
                return false;
            }
        }
        for (SOAPHeader sh : WSDLUtils.getExtensions(els, SOAPHeader.class)) {
            if (sh.getUse() == null || !WSDLUtils.WSDL1_USE_LITERAL.equals(sh.getUse())) {
                return false;
            }
            for (SOAPHeaderFault shf : WSDLUtils.getSOAPHeaderFaults(sh)) {
                if (shf.getUse() == null || !WSDLUtils.WSDL1_USE_LITERAL.equals(shf.getUse())) {
                    return false;
                }
            }
        }
        for (SOAPFault sf : WSDLUtils.getExtensions(els, SOAPFault.class)) {
            if (sf.getUse() == null || !WSDLUtils.WSDL1_USE_LITERAL.equals(sf.getUse())) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkNullOrNonEmpty(String str, boolean isNonEmpty) {
        if (isNonEmpty) {
            if (str != null && str.length() > 0) {
                try {
                    return new URI(str).isAbsolute();
                } catch (Exception e) {
                }
            }
            return false;
        } else {
            return str == null;
        }
    }
    
    private boolean ensureNamespace(BindingOperation operation, boolean body, boolean set) {
        List<ElementExtensible> els = WSDLUtils.getElements(operation);
        if (body) {
            for (SOAPBody sb : WSDLUtils.getExtensions(els, SOAPBody.class)) {
                if (!checkNullOrNonEmpty(sb.getNamespaceURI(), set)) {
                    return false;
                }
            }
        } else {
            for (SOAPHeader sh : WSDLUtils.getExtensions(els, SOAPHeader.class)) {
                if (!checkNullOrNonEmpty(sh.getNamespaceURI(), set)) {
                    return false;
                }
                for (SOAPHeaderFault shf : WSDLUtils.getSOAPHeaderFaults(sh)) {
                    if (!checkNullOrNonEmpty(shf.getNamespaceURI(), set)) {
                        return false;
                    }
                }
            }
            for (SOAPFault sf : WSDLUtils.getExtensions(els, SOAPFault.class)) {
                if (!checkNullOrNonEmpty(sf.getNamespaceURI(), set)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private Style getStyle(String str) {
        if (WSDLUtils.WSDL1_STYLE_DOCUMENT.equalsIgnoreCase(str)) {
            return Style.DOCUMENT;
        } else if (WSDLUtils.WSDL1_STYLE_RPC.equalsIgnoreCase(str)) {
            return Style.RPC;
        } else {
            return null;
        }
    }
}
