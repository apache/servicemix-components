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
package org.apache.servicemix.http.jetty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.client.HttpExchange;
import org.mortbay.util.StringUtil;

public class SmxHttpExchange extends HttpExchange {

    int responseStatus;
    HttpFields responseFields;
    String encoding = "utf-8";
    ByteArrayOutputStream responseContent;
    int contentLength;

    public SmxHttpExchange() {
        responseFields = new HttpFields();
    }

    /* ------------------------------------------------------------ */
    public int getResponseStatus() {
        if (getStatus() < STATUS_PARSING_HEADERS) {
            throw new IllegalStateException("Response not received");
        }
        return responseStatus;
    }

    /* ------------------------------------------------------------ */
    public HttpFields getResponseFields() {
        if (getStatus() < STATUS_PARSING_CONTENT) {
            throw new IllegalStateException("Headers not complete");
        }
        return responseFields;
    }

    /* ------------------------------------------------------------ */
    public String getResponseContent() throws UnsupportedEncodingException {
        if (responseContent != null) {
            return responseContent.toString(encoding);
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    public Reader getResponseReader() throws UnsupportedEncodingException {
        if (responseContent != null) {
            return new InputStreamReader(new ByteArrayInputStream(responseContent.toByteArray()), encoding);
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    public byte[] getResponseData() throws UnsupportedEncodingException {
        if (responseContent != null) {
            return responseContent.toByteArray();
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    public String getResponseEncoding() throws UnsupportedEncodingException {
        return encoding;
    }

    /* ------------------------------------------------------------ */
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException {
        responseStatus = status;
    }

    /* ------------------------------------------------------------ */
    protected void onResponseHeader(Buffer name, Buffer value) throws IOException {
        if (responseFields != null) {
            responseFields.add(name, value);
        }
        int header = HttpHeaders.CACHE.getOrdinal(value);
        switch (header) {
        case HttpHeaders.CONTENT_LANGUAGE_ORDINAL:
            contentLength = BufferUtil.toInt(value);
            break;
        case HttpHeaders.CONTENT_TYPE_ORDINAL:
            String mime = StringUtil.asciiToLowerCase(value.toString());
            int i = mime.indexOf("charset=");
            if (i > 0) {
                mime = mime.substring(i + 8);
                i = mime.indexOf(';');
                if (i > 0) {
                    mime = mime.substring(0, i);
                }
            }
            if (mime != null && mime.length() > 0) {
                encoding = mime;
            }
            break;
        default:
            break;
        }
    }

    /* ------------------------------------------------------------ */
    protected void onResponseContent(Buffer content) throws IOException {
        if (responseContent == null) {
            responseContent = new ByteArrayOutputStream(contentLength);
        }
        content.writeTo(responseContent);
    }

}
