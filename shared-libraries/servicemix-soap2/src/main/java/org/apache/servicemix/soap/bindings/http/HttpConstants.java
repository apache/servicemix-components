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
package org.apache.servicemix.soap.bindings.http;

import java.net.URI;

public class HttpConstants {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    
    
    //{input/output serialization} constants
    public static final String SERIAL_APP_URLENCODED = "application/x-www-form-urlencoded";
    public static final String SERIAL_APP_XML = "application/xml";
    public static final String SERIAL_MULTIPART_FORMDATA = "multipart/form-data";

    // operation styles
    public static final URI STYLE_RPC = URI.create("http://www.w3.org/2006/01/wsdl/style/rpc");
    public static final URI STYLE_IRI = URI.create("http://www.w3.org/2006/01/wsdl/style/iri");
    public static final URI STYLE_MULTIPART = URI.create("http://www.w3.org/2006/01/wsdl/style/multipart");
    
    public static final String QUERY_SEP_AMPERSAND = "&";

    // CGI headers 
    public static final String AUTH_TYPE = "AUTH_TYPE";
    public static final String CONTENT_LENGTH = "CONTENT_LENGTH";
    public static final String CONTENT_TYPE = "CONTENT_TYPE";
    public static final String DOCUMENT_ROOT = "DOCUMENT_ROOT";
    public static final String PATH_INFO = "PATH_INFO";
    public static final String PATH_TRANSLATED = "PATH_TRANSLATED";
    public static final String QUERY_STRING = "QUERY_STRING";
    public static final String REMOTE_ADDRESS = "REMOTE_ADDR";
    public static final String REMOTE_HOST = "REMOTE_HOST";
    public static final String REMOTE_USER = "REMOTE_USER";
    public static final String REQUEST_METHOD = "REQUEST_METHOD";
    public static final String REQUEST_URI = "REQUEST_URI";
    public static final String SCRIPT_NAME = "SCRIPT_NAME";
    public static final String SERVER_NAME = "SERVER_NAME";
    public static final String SERVER_PORT = "SERVER_PORT";
    public static final String SERVER_PROTOCOL = "SERVER_PROTOCOL";
    
}
