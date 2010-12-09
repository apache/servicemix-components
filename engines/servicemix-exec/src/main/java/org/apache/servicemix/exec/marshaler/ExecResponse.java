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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * <p>
 * Container for execution result data.
 * </p>
 * 
 * @author jbonofre
 */
@XmlRootElement(namespace="http://servicemix.apache.org/exec")
public class ExecResponse {
    
    private int exitCode;
    private long startTime;
    private long endTime;
    private long executionDuration;
    
    private StringBuffer outputData;
    private StringBuffer errorData;
    
    /**
     * <p>
     * Default constructor.
     * </p>
     */
    public ExecResponse() {
        this.outputData = new StringBuffer();
        this.errorData = new StringBuffer();
        this.exitCode = -1;
    }
    
    public int getExitCode() {
        return this.exitCode;
    }
    
    /**
     * <p>
     * This attribute defines the system command execution exit code.
     * </p>
     * 
     * @param exitCode the system command execution exit code.
     */
    @XmlElement
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    public long getStartTime() {
        return this.startTime;
    }
    
    /**
     * <p>
     * This attribute defines the system command execution start time (timestamp).
     * </p>
     * 
     * @param startTime the system command execution start time. 
     */
    @XmlElement
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return this.endTime;
    }
    
    /**
     * <p>
     * This attribute defines the system command execution end time (timestamp).
     * </p>
     * 
     * @param endTime the system command execution end time.
     */
    @XmlElement
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public long getExecutionDuration() {
        return this.executionDuration;
    }

    /**
     * <p>
     * This attribute defines the system command execution duration.
     * </p>
     * 
     * @param executionDuration the system command execution duration.
     */
    @XmlElement
    public void setExecutionDuration(long executionDuration) {
        this.executionDuration = executionDuration;
    }
    
    public StringBuffer getOutputData() {
        return this.outputData;
    }
    
    /**
     * <p>
     * This attribute defines the system command execution output buffer.
     * </p>
     * 
     * @param outputData the system command execution output buffer.
     */
    @XmlElement
    public void setOutputData(StringBuffer outputData) {
        this.outputData = outputData;
    }
    
    public StringBuffer getErrorData() {
        return this.errorData;
    }
    
    /**
     * <p>
     * This attribute defines the system command execution error buffer.
     * </p>
     * 
     * @param errorData the system command execution error buffer.
     */
    @XmlElement
    public void setErrorData(StringBuffer errorData) {
        this.errorData = errorData;
    }

}
