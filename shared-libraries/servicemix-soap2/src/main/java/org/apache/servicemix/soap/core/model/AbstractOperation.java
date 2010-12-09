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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.model.Message;
import org.apache.servicemix.soap.api.model.Operation;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class AbstractOperation<T extends Message> implements Operation<T> {

    private QName name;
    private T input;
    private T output;
    private List<T> faults;
    private URI mep;
    
    public AbstractOperation() {
        faults = new ArrayList<T>();
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
     * @return the input
     */
    public T getInput() {
        return input;
    }
    /**
     * @param input the input to set
     */
    public void setInput(T input) {
        this.input = input;
    }
    /**
     * @return the output
     */
    public T getOutput() {
        return output;
    }
    /**
     * @param output the output to set
     */
    public void setOutput(T output) {
        this.output = output;
    }
    /**
     * @return the mep
     */
    public URI getMep() {
        return mep;
    }
    /**
     * @param mep the mep to set
     */
    public void setMep(URI mep) {
        this.mep = mep;
    }
    /**
     * @return the faults
     */
    public List<T> getFaults() {
        return faults;
    }
    /**
     * @param fault the fault to add
     */
    public void addFault(T fault) {
        faults.add(fault);
    }

    
}
