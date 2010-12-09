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
package org.apache.servicemix.soap.bindings.soap.model.wsdl1;

import org.apache.servicemix.soap.api.model.wsdl1.Wsdl1Binding;
import org.apache.servicemix.soap.bindings.soap.model.SoapBinding;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public interface Wsdl1SoapBinding extends Wsdl1Binding<Wsdl1SoapOperation>,
                                          SoapBinding<Wsdl1SoapOperation> {

    public enum Style {
        RPC,
        DOCUMENT
    }
    
    public String getLocationURI();
    
    public String getTransportURI();

    public Style getStyle();
}
