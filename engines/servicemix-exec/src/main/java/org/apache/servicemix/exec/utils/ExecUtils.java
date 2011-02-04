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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.servicemix.exec.marshaler.ExecRequest;
import org.apache.servicemix.exec.marshaler.ExecResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to execute system command.
 * 
 * @author jbonofre
 */
public class ExecUtils {

	private final static Logger logger = LoggerFactory.getLogger(ExecUtils.class);

	/**
	 * <p>
	 * Executes a command and returns the output and error buffer and also the
	 * return value.
	 * </p>
	 * 
	 * @param execRequest the exec request.
	 * @return the execution response.
	 * @throws ExecException in case of execution failure.
	 */
	public static ExecResponse execute(ExecRequest execRequest) throws ExecException {

		ExecResponse execResponse = new ExecResponse();
		
		String exec = execRequest.getCommand();
		for (String argument:execRequest.getArguments()) {
		    exec = exec + " " + argument;
		}
		
		logger.info("Execute command " + exec);
		String[] shellCommand = null;
		logger.debug("Define the shell.");
		logger.debug("Get the OS name property.");
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			logger.debug("Microsoft Windows platform detected.");
			String comSpec = System.getProperty("ComSpec");
			if (comSpec != null) {
				logger.debug("The ComSpec MS Windows environment variable is defined, using it: " + comSpec + " /C " + exec);
				shellCommand = new String[] { comSpec, "/C", exec };
			} else {
				logger.debug("The ComSpec MS Windows environment variable is not defined, found the shell command depending of the MS Windows version.");
				if (osName.startsWith("Windows 3")
						|| osName.startsWith("Windows 95")
						|| osName.startsWith("Windows 98")
						|| osName.startsWith("Windows ME")) {
					logger.debug("MS Windows 3.1/95/98/Me detected, using: command.com /C " + exec);
					shellCommand = new String[] { "command.com", "/C", exec };
				} else {
					logger.debug("MS Windows NT/XP/Vista detected, using: cmd.exe /C " + exec);
					shellCommand = new String[] { "cmd.exe", "/C", exec };
				}
			}
		} else {
			logger.debug("Unix platform detected.");
			String shell = System.getProperty("SHELL");
			if (shell != null) {
				logger.debug("The SHELL Unix environment variable is defined, using it: " + shell + " -c " + exec);
				shellCommand = new String[] { shell, "-c", exec };
			} else {
				logger.debug("The SHELL Unix environment variable is not defined, using the default Unix shell: /bin/sh -c " + exec);
				shellCommand = new String[] { "/bin/sh", "-c", exec };
			}
		}
		try {
			// remember the start time
			execResponse.setStartTime(System.currentTimeMillis());
			
			// launch the system command
			Process process = Runtime.getRuntime().exec(shellCommand);
			
			// get and start the error stream gobbler
			StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), execResponse.getErrorData());
			errorGobbler.start();

			// get and start the output stream gobbler
			StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), execResponse.getOutputData());
			outputGobbler.start();
			
			// wait the end of the process
			int exitValue = process.waitFor();
			
			// remember the end time
			execResponse.setEndTime(System.currentTimeMillis());
			
			// store the exit code
			execResponse.setExitCode(exitValue);
			
			if (exitValue != 0) {
				// an error occured
				logger.error("Command " + exec + " execution failed with return code " + exitValue + " : " + execResponse.getErrorData().toString());
			} else {
				// command was successful
				logger.debug("Command " + exec + " execution completed: " + execResponse.getOutputData().toString());
			}
		} catch (Exception exception) {
			logger.error("Command " + exec + " execution failed.", exception);
			throw new ExecException("Command " + exec + " execution failed.", exception);
		}

		// return the exec response
		return execResponse;
	}
}

/**
 * <p>
 * Inner class to glob stream with a thread.
 * </p>
 * 
 * @author jbonofre
 */
class StreamGobbler extends Thread {

	private final Logger logger = LoggerFactory.getLogger(StreamGobbler.class);

	InputStream in;
	StringBuffer response;

	StreamGobbler(InputStream in, StringBuffer response) {
		this.in = in;
		this.response = response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			String row = null;
			while ((row = reader.readLine()) != null) {
				response.append(row);
				response.append('\n');
			}
		} catch (IOException ioException) {
			logger.warn("System command stream gobbler error : " + ioException.getMessage());
		}
	}

}
