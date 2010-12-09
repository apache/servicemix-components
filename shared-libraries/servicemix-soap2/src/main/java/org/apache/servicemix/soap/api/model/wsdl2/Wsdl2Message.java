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
package org.apache.servicemix.soap.api.model.wsdl2;

import org.apache.servicemix.soap.api.model.Message;
import org.apache.ws.commons.schema.XmlSchemaElement;

public interface Wsdl2Message extends Message {

    public enum ContentModel {
        NONE,
        ANY,
        OTHER,
        ELEMENT;
        
        public static ContentModel parse(String str) {
            if ("#none".equals(str)) {
                return NONE;
            } else if ("#any".equals(str)) {
                return ANY;
            } else if ("#other".equals(str)) {
                return OTHER;
            } else if ("#element".equals(str)) {
                return ELEMENT;
            } else {
                return null;
            }
        }
    }
    
    public ContentModel getContentModel();
    
    public XmlSchemaElement getElementDeclaration();
}
