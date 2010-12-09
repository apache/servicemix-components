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
package org.apache.servicemix.xmpp;

public abstract class XMPPConstants {

    public static final int DEFAULT_XMPP_PORT = 5222;
    public static final int DEFAULT_PROXY_PORT = 3128;

    /**
     * checks whether an attribute is set
     *
     * @param attribute the attribute to check for null and empty string
     * @return  true if not null and not empty
     */
    public static boolean isSet(String attribute) {
        return attribute != null && attribute.trim().length()>0;
    }

    /**
     * hidden constructor
     */
    private XMPPConstants() {
    }
}
