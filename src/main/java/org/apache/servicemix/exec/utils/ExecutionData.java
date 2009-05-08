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
package org.apache.servicemix.exec.utils;

/**
 * helper object storing several important data for use after execution
 * 
 * @author lhein
 */
public class ExecutionData {
	private StringBuffer outputData;
	private StringBuffer errorData;
	
	private int exitCode;
	
	private long startTime;
	private long endTime;
	private long executionDuration;
	
	/**
	 * constructs an execution data object
	 */
	public ExecutionData() {
		this.outputData = new StringBuffer();
		this.errorData = new StringBuffer();
		this.exitCode = -1;
	}

	/**
	 * @return the outputData
	 */
	public StringBuffer getOutputData() {
		return this.outputData;
	}

	/**
	 * @param outputData the outputData to set
	 */
	public void setOutputData(StringBuffer outputData) {
		this.outputData = outputData;
	}

	/**
	 * @return the errorData
	 */
	public StringBuffer getErrorData() {
		return this.errorData;
	}

	/**
	 * @param errorData the errorData to set
	 */
	public void setErrorData(StringBuffer errorData) {
		this.errorData = errorData;
	}

	/**
	 * @return the exitCode
	 */
	public int getExitCode() {
		return this.exitCode;
	}

	/**
	 * @param exitCode the exitCode to set
	 */
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return this.startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return this.endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
		// calculate the execution duration
		if (this.startTime > 0 && this.endTime >= this.startTime) {
			this.executionDuration = this.endTime - this.startTime;
		}
	}

	/**
	 * @return the executionDuration
	 */
	public long getExecutionDuration() {
		return this.executionDuration;
	}
}
