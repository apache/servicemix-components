/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.mail.utils;

/**
 * @author lhein
 */
public enum MailContentType {
    TEXT_PLAIN  ("text/plain", "text/plain"),
    TEXT_HTML   ("text/html", "text/html"),  
    TEXT_XML    ("text/xml", "text/xml"),
    MULTIPART   ("multipart/*", "multipart/*"),
    MULTIPART_MIXED       ("multipart/mixed", "multipart/mixed"),
    MULTIPART_ALTERNATIVE ("multipart/alternative", "multipart/alternative"),
    UNKNOWN     ("unknown", "text/plain");
    
    private String key;
    private String mimeType;
    
    /**
     * creates a mail content type enum object
     * 
     * @param key       the key
     * @param mimeType  the mime type
     */
    private MailContentType(String key, String mimeType) {
        this.key = key;
        this.mimeType = mimeType;
    }
    
    /**
     * @return Returns the key.
     */
    public String getKey() {
        return this.key;
    }
    
    /** 
     * @return Returns the mimeType.
     */
    public String getMimeType() {
        return this.mimeType;
    }
    
    /**
     * returns the enum type for a value
     * 
     * @param value     the string value
     * @return          the enum matching this value or UNKNOWN if unrecognized
     */
    public static MailContentType getEnumForValue(String value) {
        for (MailContentType ct : values()) {
            if (ct.getMimeType().equalsIgnoreCase(value)) {
                return ct;
            }
        }
        return MailContentType.UNKNOWN;
    }
}
