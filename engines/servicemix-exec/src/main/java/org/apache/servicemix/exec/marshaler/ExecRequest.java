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
package org.apache.servicemix.exec.marshaler;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <p>
 * Represents an exec request including command and arguments.
 * </p>
 * 
 * @author jbonofre
 */
@XmlRootElement(namespace="http://servicemix.apache.org/exec")
public class ExecRequest {
    
    private String command; // the system command
    private List<String> arguments = new LinkedList<String>(); // the command arguments
    
    public String getCommand() {
        return this.command;
    }
    
    /**
     * <p>
     * This attribute defines the system command to execute. 
     * </p>
     * 
     * @param command the system command.
     */
    @XmlElement
    public void setCommand(String command) {
        this.command = command;
    }
    
    public List<String> getArguments() {
        return this.arguments;
    }
    
    /**
     * <p>
     * This attribute defines the system command arguments.
     * </p>
     * 
     * @param arguments the system command arguments.
     */
    @XmlElement(name="argument")
    @XmlElementWrapper(name="arguments")
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

}
