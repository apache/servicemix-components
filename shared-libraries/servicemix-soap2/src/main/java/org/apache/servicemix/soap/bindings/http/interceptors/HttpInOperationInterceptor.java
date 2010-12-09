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
package org.apache.servicemix.soap.bindings.http.interceptors;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.http.HttpConstants;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpBinding;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpOperation;
import org.apache.servicemix.soap.core.AbstractInterceptor;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class HttpInOperationInterceptor extends AbstractInterceptor {

    public void handleMessage(Message message) {
        Binding<?> binding = message.get(Binding.class);
        if (binding instanceof Wsdl2HttpBinding == false) {
            return;
        }
        Wsdl2HttpBinding httpBinding = (Wsdl2HttpBinding) binding;
        String uri = message.getTransportHeaders().get(HttpConstants.REQUEST_URI);
        if (uri == null) {
            throw new Fault("Transport header not set: " + HttpConstants.REQUEST_URI);
        }
        String mth = message.getTransportHeaders().get(HttpConstants.REQUEST_METHOD);
        if (mth == null) {
            throw new Fault("Transport header not set: " + HttpConstants.REQUEST_METHOD);
        }
        for (Wsdl2HttpOperation operation : httpBinding.getOperations()) {
            if (mth.equalsIgnoreCase(operation.getHttpMethod())) {
                String loc = IriDecoderHelper.combine(binding.getLocation(), operation.getHttpLocation());
                String path1 = getUriPath(uri);
                String path2 = getUriPath(loc);
                if (matchPath(path1, path2)) {
                    message.put(Operation.class, operation);
                    return;
                }
            }
        }
    }

    private boolean matchPath(String path, String template) {
        int idx = template.indexOf('{');
        while (idx >= 0 && template.charAt(idx + 1) == '{') {
            idx = template.indexOf('{', idx + 2);
        }
        if (idx > 0) {
            return path.regionMatches(0, template, 0, idx - 1);
        } else {
            return path.equals(template);
        }
    }

    private String getUriPath(String uri) {
        int idx = uri.indexOf("://");
        int idx2 = uri.indexOf('/', idx + 3);
        return uri.substring(idx2 + 1);
    }

}
