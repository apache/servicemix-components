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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to execute system command.
 * 
 * @author jbonofre
 */
public class ExecUtils {
    
    private static final transient Log LOG = LogFactory.getLog(ExecUtils.class);
    
    /**
     * <p>
     * Executes a system command and return the output buffer.
     * </p>
     * 
     * @param command the system command to execute.
     * @return the command execution output buffer.
     * @throws AutoDeployException
     */
    public static String execute(String command) throws ExecException {
        LOG.info("Execute command " + command);
        String[] shellCommand = null;
        LOG.debug("Define the shell.");
        LOG.debug("Get the OS name property.");
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")) {
           LOG.debug("Microsoft Windows platform detected.");
           String comSpec = System.getProperty("ComSpec");
           if(comSpec != null) {
              LOG.debug("The ComSpec MS Windows environment variable is defined, using it: " + comSpec + " /C " + command);
              shellCommand = new String[]{ comSpec, "/C", command};
           }
           else {
              LOG.debug("The ComSpec MS Windows environment variable is not defined, found the shell command depending of the MS Windows version.");
              if(osName.startsWith("Windows 3") || osName.startsWith("Windows 95") || osName.startsWith("Windows 98") || osName.startsWith("Windows ME")) {
                 LOG.debug("MS Windows 3.1/95/98/Me detected, using: command.com /C " + command);
                 shellCommand = new String[]{ "command.com", "/C", command};
              }
              else {
                 LOG.debug("MS Windows NT/XP/Vista detected, using: cmd.exe /C " + command);
                 shellCommand = new String[]{ "cmd.exe", "/C", command};
              }
           }
        }
        else {
           LOG.debug("Unix platform detected.");
           String shell = System.getProperty("SHELL");
           if(shell != null) {
              LOG.debug("The SHELL Unix environment variable is defined, using it: " + shell + " -c " + command);
              shellCommand = new String[]{ shell, "-c", command};
           }
           else {
              LOG.debug("The SHELL Unix environment variable is not defined, using the default Unix shell: /bin/sh -c " + command);
              shellCommand = new String[]{ "/bin/sh", "-c", command};
           }
        }
        try {
           Runtime runtime = Runtime.getRuntime();
           // launch the system command
           Process process = runtime.exec(shellCommand);
           // get the error stream gobbler
           StringBuffer errorBuffer = new StringBuffer();
           StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), errorBuffer);
           // get the output stream gobbler
           StringBuffer outputBuffer = new StringBuffer();
           StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), outputBuffer);
           // start both gobblers
           errorGobbler.start();
           outputGobbler.start();
           // wait the end of the process
           int exitValue = process.waitFor();
           if(exitValue != 0) {
              // an error occurs
              LOG.error("Command " + command + " execution failed: " + errorBuffer.toString());
              throw new ExecException(errorBuffer.toString());
           }
           // command is OK
           LOG.info("Command " + command + " execution completed: " + outputBuffer.toString());
           return outputBuffer.toString();
        } catch(Exception exception) {
           LOG.error("Command " + command + " execution failed.", exception);
           throw new ExecException("Command " + command + " execution failed.", exception);
        } 
    }

}


/**
 * <p>
 * Inner class to glob stream with a thread.
 * </p>
 * 
 * @author onofre
 */
class StreamGobbler extends Thread {

   // log facility
   private final static transient Log LOG = LogFactory.getLog(StreamGobbler.class);

   InputStream in;
   StringBuffer response;

   StreamGobbler(InputStream in, StringBuffer response) {
      this.in = in;
      this.response = response;
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Thread#run()
    */
   public void run() {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(in));
         String row = null;
         while((row = reader.readLine()) != null) {
            response.append(row + "\n");
         }
      } catch(IOException ioException) {
         LOG.warn("System command stream gobbler error : " + ioException.getMessage());
      }
   }

}
