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

import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractHttpProviderMarshaler implements HttpProviderMarshaler {

    private final Log log = LogFactory.getLog(getClass());

    private String contentEncoding;
    private String acceptEncoding;

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getAcceptEncoding() {
        return acceptEncoding;
    }

    public void setAcceptEncoding(String acceptEncoding) {
        this.acceptEncoding = acceptEncoding;
    }

    protected OutputStream getRequestEncodingStream(String encoding, OutputStream dataStream) throws IOException {
        if (encoding != null && encoding.toLowerCase().indexOf("gzip") >= 0) {
            log.debug("Using gzip request encoding in provider marshaller.");
            return new GZIPOutputStream(new BufferedOutputStream(dataStream));
        } else {
            log.debug("Using default request encoding in provider marshaller.");
            return new BufferedOutputStream(dataStream);
        }
    }

    protected InputStream getResponseEncodingStream(String encoding, InputStream dataStream) throws IOException {
        if (encoding != null && encoding.toLowerCase().indexOf("gzip") >= 0) {
            log.debug("Using gzip response encoding in provider marshaller.");
            return new GZIPInputStream(new BufferedInputStream(dataStream));
        } else {
            log.debug("Using default response encoding in provider marshaller.");
            return new BufferedInputStream(dataStream);
        }
    }

}
