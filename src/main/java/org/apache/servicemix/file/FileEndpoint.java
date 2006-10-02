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
package org.apache.servicemix.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.ProviderEndpoint;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * An endpoint which receives a message and writes the content to a file.
 *
 * @org.apache.xbean.XBean element="endpoint"
 */
public class FileEndpoint extends ProviderEndpoint {
    private static final Log log = LogFactory.getLog(FileEndpoint.class);

    private File directory;
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private String tempFilePrefix = "servicemix-";
    private String tempFileSuffix = ".xml";
    private boolean autoCreateDirectory = true;


    public void process(MessageExchange exchange) throws Exception {
        OutputStream out = null;
        try {
            NormalizedMessage message = exchange.getMessage("in");
            String name = marshaler.getOutputName(exchange, message);
            File newFile = null;
            System.out.println("Creating file in directory: " + directory.getCanonicalPath());
            if (name == null) {
                newFile = File.createTempFile(tempFilePrefix, tempFileSuffix, directory);
            }
            else {
                newFile = new File(directory, name);
            }
            out = new BufferedOutputStream(new FileOutputStream(newFile));
            marshaler.writeMessage(exchange, message, out, name);
            done(exchange);
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    log.error("Caught exception while closing stream on error: " + e, e);
                }
            }
        }
    }
    
    public void validate() throws DeploymentException {
        super.validate();

        if (directory == null) {
            throw new DeploymentException("You must specify the directory property");
        }
        if (isAutoCreateDirectory()) {
            directory.mkdirs();
        }
        if (!directory.isDirectory()) {
            throw new DeploymentException("The directory property must be a directory but was: " + directory);
        }
    }


    // Properties
    //-------------------------------------------------------------------------
    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public FileMarshaler getMarshaler() {
        return marshaler;
    }

    public void setMarshaler(FileMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public String getTempFilePrefix() {
        return tempFilePrefix;
    }

    public void setTempFilePrefix(String tempFilePrefix) {
        this.tempFilePrefix = tempFilePrefix;
    }

    public String getTempFileSuffix() {
        return tempFileSuffix;
    }

    public void setTempFileSuffix(String tempFileSuffix) {
        this.tempFileSuffix = tempFileSuffix;
    }

    public boolean isAutoCreateDirectory() {
        return autoCreateDirectory;
    }

    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

}