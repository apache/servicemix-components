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
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import org.mortbay.jetty.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpProviderMarshaler implements HttpProviderMarshaler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Set<String> DEFAULT_HEADER_BLACKLIST =
            new HashSet<String>(
                Arrays.asList(HttpHeaders.AUTHORIZATION,
                              HttpHeaders.EXPECT,
                              HttpHeaders.FORWARDED,
                              HttpHeaders.FROM,
                              HttpHeaders.HOST,
                              HttpHeaders.CONTENT_ENCODING,
                              HttpHeaders.CONTENT_TYPE));

    private String contentEncoding;
    private String acceptEncoding;
    /**
     * a blacklist for properties which shouldn't be copied
     */
    private Set<String> headerBlackList = DEFAULT_HEADER_BLACKLIST;


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
            logger.debug("Using gzip request encoding in provider marshaller.");
            return new GZIPOutputStream(new BufferedOutputStream(dataStream));
        } else {
            logger.debug("Using default request encoding in provider marshaller.");
            return new BufferedOutputStream(dataStream);
        }
    }

    protected InputStream getResponseEncodingStream(String encoding, InputStream dataStream) throws IOException {
        if (encoding != null && encoding.toLowerCase().indexOf("gzip") >= 0) {
            logger.debug("Using gzip response encoding in provider marshaller.");
            return new GZIPInputStream(new BufferedInputStream(dataStream));
        } else {
            logger.debug("Using default response encoding in provider marshaller.");
            return new BufferedInputStream(dataStream);
        }
    }

    /**
     * checks if a property is on black list
     *
     * @param name the property
     * @return true if on black list
     */
    protected boolean isBlackListed(String name) {
        return (this.headerBlackList != null && this.headerBlackList.contains(name));
    }

    public Set<String> getHeaderBlackList() {
        return headerBlackList;
    }

    /**
     * Specifies a list of headers to not include in the HTTP request.
     * 
     * @param headerBlackList list of headers to not include in the HTTP request
     */
    public void setHeaderBlackList(Set<String> headerBlackList) {
        this.headerBlackList = headerBlackList;
    }
}
