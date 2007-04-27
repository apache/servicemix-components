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
package org.apache.servicemix.http.endpoints;

import java.io.IOException;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.servicemix.expression.Expression;

public class DefaultHttpProviderMarshaler implements HttpProviderMarshaler {

    private String locationUri;
    private Expression locationUriExpression;
    private int retryCount;

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLocationUri() {
        return locationUri;
    }

    public void setLocationUri(String locationUri) {
        this.locationUri = locationUri;
    }

    public Expression getLocationUriExpression() {
        return locationUriExpression;
    }

    public void setLocationUriExpression(Expression locationUriExpression) {
        this.locationUriExpression = locationUriExpression;
    }

    public String getDestinationUri(MessageExchange exchange, NormalizedMessage inMsg) throws Exception {
        String uri = null;
        if (locationUriExpression != null) {
            Object o = locationUriExpression.evaluate(exchange, inMsg);
            uri = (o != null) ? o.toString() : null;
        }
        if (uri == null) {
            uri = locationUri;
        }
        return uri;
    }

    public HttpMethod createMethod(MessageExchange exchange, NormalizedMessage inMsg) throws Exception {
        PostMethod method = new PostMethod();
        setRetryHandler(method);
        return method;
    }

    protected void setRetryHandler(HttpMethod method) {
        HttpMethodRetryHandler retryHandler = new HttpMethodRetryHandler() {
            public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
                return executionCount < retryCount;
            }
        };
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
    }

}
