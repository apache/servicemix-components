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
package org.apache.servicemix.camel;

/**
 * See the <a href="http://www.w3.org/TR/wsdl20-extensions/">WSDL 2.0 Extensions</a> for a description
 * of message exchange patterns.
 * 
 * @version $Revision: 1.1 $
 */
public class MEPConstants {
    public static final String PROPERTY = "org.apache.camel.MessageExchangePattern";

    public static final String IN_ONLY = "http://www.w3.org/ns/wsdl/in-only";
    public static final String ROBUST_IN_ONLY = "http://www.w3.org/ns/wsdl/robust-in-only";
    public static final String IN_OUT = "http://www.w3.org/ns/wsdl/in-out";
    public static final String IN_OPTIONAL_OUT = "http://www.w3.org/ns/wsdl/in-optional-out";
    public static final String OUT_ONLY = "http://www.w3.org/ns/wsdl/out-only";
    public static final String ROBUST_OUT_ONLY = "http://www.w3.org/ns/wsdl/robust-out-only";
    public static final String OUT_IN = "http://www.w3.org/ns/wsdl/out-in";
    public static final String OUT_OPTIONAL_IN = "http://www.w3.org/ns/wsdl/out-optional_in";
}
