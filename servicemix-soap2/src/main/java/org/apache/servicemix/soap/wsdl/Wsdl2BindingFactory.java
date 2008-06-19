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

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.wsdl2.Wsdl2Message.ContentModel;
import org.apache.servicemix.soap.bindings.http.impl.Wsdl2HttpBindingImpl;
import org.apache.servicemix.soap.bindings.http.impl.Wsdl2HttpHeaderImpl;
import org.apache.servicemix.soap.bindings.http.impl.Wsdl2HttpMessageImpl;
import org.apache.servicemix.soap.bindings.http.impl.Wsdl2HttpOperationImpl;
import org.apache.woden.internal.wsdl20.Constants;
import org.apache.woden.internal.wsdl20.extensions.http.HTTPConstants;
import org.apache.woden.wsdl20.BindingMessageReference;
import org.apache.woden.wsdl20.BindingOperation;
import org.apache.woden.wsdl20.Endpoint;
import org.apache.woden.wsdl20.Interface;
import org.apache.woden.wsdl20.InterfaceFault;
import org.apache.woden.wsdl20.InterfaceFaultReference;
import org.apache.woden.wsdl20.InterfaceMessageReference;
import org.apache.woden.wsdl20.InterfaceOperation;
import org.apache.woden.wsdl20.enumeration.Direction;
import org.apache.woden.wsdl20.extensions.ComponentExtensions;
import org.apache.woden.wsdl20.extensions.http.HTTPBindingMessageReferenceExtensions;
import org.apache.woden.wsdl20.extensions.http.HTTPBindingOperationExtensions;
import org.apache.woden.wsdl20.extensions.http.HTTPHeader;
import org.apache.woden.xml.XMLAttr;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class Wsdl2BindingFactory {

    public static URI HTTP_BINDING_TYPE = URI.create("http://www.w3.org/2006/01/wsdl/http");
    public static URI XSD_2001_SYSTEM = URI.create(Constants.TYPE_XSD_2001);
    
    public static Binding<?> createBinding(Endpoint wsdlEndpoint) {
        if (HTTP_BINDING_TYPE.equals(wsdlEndpoint.getBinding().getType())) {
            return createWsdl2HttpBinding(wsdlEndpoint);
        }
        return null;
    }

    private static Binding<?> createWsdl2HttpBinding(Endpoint wsdlEndpoint) {
        Wsdl2HttpBindingImpl binding = new Wsdl2HttpBindingImpl();
        binding.setLocation(wsdlEndpoint.getAddress().toString());
        Interface wsdlInterface = wsdlEndpoint.getBinding().getInterface();
        InterfaceOperation[] wsdlOperations = wsdlInterface.getInterfaceOperations();
        for (int i = 0; i < wsdlOperations.length; i++) {
            // Retrieve binding and extension
            InterfaceOperation wsdlOperation = wsdlOperations[i];
            BindingOperation wsdlBindingOperation = findBindingOperation(wsdlEndpoint.getBinding(), wsdlOperation);
            HTTPBindingOperationExtensions opExt = wsdlBindingOperation != null ? (HTTPBindingOperationExtensions) wsdlBindingOperation.getComponentExtensionsForNamespace(ComponentExtensions.URI_NS_HTTP) : null;
            // Create operation
            Wsdl2HttpOperationImpl operation = new Wsdl2HttpOperationImpl();
            // Standard WSDL2 attributes
            operation.setName(wsdlOperation.getName());
            operation.setMep(wsdlOperation.getMessageExchangePattern());
            operation.setStyle(new HashSet<URI>(Arrays.asList(wsdlOperation.getStyle())));
            // HTTP extensions
            if (opExt != null) {
                operation.setHttpInputSerialization(opExt.getHttpInputSerialization());
                operation.setHttpOutputSerialization(opExt.getHttpOutputSerialization());
                operation.setHttpFaultSerialization(opExt.getHttpFaultSerialization());
                operation.setHttpLocation(extractLocation(wsdlBindingOperation));
                operation.setHttpMethod(opExt.getHttpMethod());
                operation.setHttpTransferCodingDefault(opExt.getHttpTransferCodingDefault());
                operation.setHttpLocationIgnoreUncited(opExt.isHttpLocationIgnoreUncited());
            }
            // Messages
            InterfaceMessageReference[] iMsgRefs = wsdlOperation.getInterfaceMessageReferences();
            for (int j = 0; j < iMsgRefs.length; j++) {
                // Retrieve binding and extension
                InterfaceMessageReference iMsgRef = iMsgRefs[j];
                BindingMessageReference bMsgRef = findBindingMessage(wsdlBindingOperation, iMsgRef);
                HTTPBindingMessageReferenceExtensions msgExt = bMsgRef != null ? (HTTPBindingMessageReferenceExtensions) bMsgRef.getComponentExtensionsForNamespace(ComponentExtensions.URI_NS_HTTP) : null;
                // Create message
                Wsdl2HttpMessageImpl message = new Wsdl2HttpMessageImpl();
                // Standard WSDL2 attributes
                message.setContentModel(ContentModel.parse(iMsgRef.getMessageContentModel()));
                message.setElementName(iMsgRef.getElementDeclaration().getName());
                if (!XSD_2001_SYSTEM.equals(iMsgRef.getElementDeclaration().getSystem())) {
                    throw new IllegalStateException("Unsupported type system: " + iMsgRef.getElementDeclaration().getSystem());
                }
                if (Constants.API_APACHE_WS_XS.equals(iMsgRef.getElementDeclaration().getContentModel())) {
                    XmlSchemaElement xsEl = (XmlSchemaElement) iMsgRef.getElementDeclaration().getContent();
                    message.setElementDeclaration(xsEl);
                }
                // HTTP extensions
                if (msgExt != null) {
                    message.setHttpTransferCoding(msgExt.getHttpTransferCoding());
                    HTTPHeader[] headers = msgExt.getHttpHeaders();
                    for (int k = 0; k < headers.length; k++) {
                        Wsdl2HttpHeaderImpl h = new Wsdl2HttpHeaderImpl();
                        h.setName(headers[k].getName());
                        h.setRequired(headers[k].isRequired() ? headers[k].isRequired().booleanValue() : false);
                        if (!XSD_2001_SYSTEM.equals(headers[k].getTypeDefinition().getSystem())) {
                            throw new IllegalStateException("Unsupported type system: " + headers[k].getTypeDefinition().getSystem());
                        }
                        h.setType(headers[k].getTypeDefinition().getName());
                        message.addHttpHeader(h);
                    }
                }
                // Add the message
                if (iMsgRef.getDirection() == Direction.IN) {
                    operation.setInput(message);
                } else if (iMsgRef.getDirection() == Direction.OUT) {
                    operation.setOutput(message);
                } else {
                    throw new IllegalStateException("Unsupported message direction: " + iMsgRef.getDirection());
                }
            }
            // Faults
            InterfaceFaultReference[] faults = wsdlOperation.getInterfaceFaultReferences();
            for (int j = 0; j < faults.length; j++) {
                // TODO: handle interface faults references
            }
            // Add the operation
            binding.addOperation(operation);
        }
        // Faults
        InterfaceFault[] faults = wsdlInterface.getInterfaceFaults();
        for (int i = 0; i < faults.length; i++) {
            // TODO: handle interface faults
        }
        // Return the complete binding
        return binding;
    }
    
    private static BindingMessageReference findBindingMessage(BindingOperation wsdlBindingOperation, InterfaceMessageReference iMsgRef) {
        BindingMessageReference[] bMsgRefs = wsdlBindingOperation.getBindingMessageReferences();
        for (int i = 0; i < bMsgRefs.length; i++) {
            if (bMsgRefs[i].getInterfaceMessageReference() == iMsgRef) {
                return bMsgRefs[i];
            }
        }
        return null;
    }

    private static BindingOperation findBindingOperation(org.apache.woden.wsdl20.Binding binding, InterfaceOperation operation) {
        BindingOperation[] bindingOps = binding.getBindingOperations();
        for (int i = 0; i < bindingOps.length; i++) {
            if (bindingOps[i].getInterfaceOperation() == operation) {
                return bindingOps[i];
            }
        }
        return null;
    }

    private static String extractLocation(BindingOperation bOperation) {
        String location = null;
        if (bOperation != null) {
            XMLAttr attr = bOperation.toElement().getExtensionAttribute(HTTPConstants.Q_ATTR_LOCATION);
            if (attr != null) {
                location = attr.toExternalForm();
            }
        }
        return location;
    }
}
