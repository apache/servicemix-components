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
package org.apache.servicemix.soap.core.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.model.wsdl1.Wsdl1Message;
import org.apache.servicemix.soap.api.model.wsdl1.Wsdl1Part;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class AbstractWsdl1Message<T extends Wsdl1Part> extends AbstractMessage implements Wsdl1Message<T> {

    private QName name;
    private String messageName;
    private final Collection<T> parts;
    
    public AbstractWsdl1Message() {
        parts = new ArrayList<T>();
    }
    
    /**
     * @return the messageName
     */
    public String getMessageName() {
        return messageName;
    }
    /**
     * @param messageName the messageName to set
     */
    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }
    /**
     * @return the name
     */
    public QName getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(QName name) {
        this.name = name;
    }
    /**
     * @return the parts
     */
    public Collection<T> getParts() {
        return parts;
    }
    /**
     * @param part the part to add
     */
    public void addPart(T part) {
        parts.add(part);
    }
    
}
