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

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.jbi.messaging.MessageExchange;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.HttpHeaders;

public abstract class AbstractHttpConsumerMarshaler implements HttpConsumerMarshaler {

    private final Log log = LogFactory.getLog(getClass());

    protected InputStream getRequestEncodingStream(String contentEncoding, InputStream dataStream) throws IOException {
        if (contentEncoding != null && contentEncoding.toLowerCase().indexOf("gzip") >= 0) {
            log.debug("Using GZIP request content encoding.");
            return new GZIPInputStream(new BufferedInputStream(dataStream));
        } else {
            log.debug("Using default request content encoding.");
            return new BufferedInputStream(dataStream);
        }
    }

    protected OutputStream getResponseEncodingStream(String acceptEncoding, OutputStream dataStream) throws IOException {
        if (acceptEncoding != null && acceptEncoding.toLowerCase().indexOf("gzip") >= 0) {
            log.debug("Using GZIP response content encoding.");
            return new GZIPOutputStream(new BufferedOutputStream(dataStream));
        } else {
            log.debug("Using default response content encoding.");
            return new BufferedOutputStream(dataStream);
        }
    }

    protected void addResponseHeaders(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) {
        String acceptEncoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        if (acceptEncoding != null && acceptEncoding.toLowerCase().indexOf("gzip") >= 0) {
            response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
    }

}
