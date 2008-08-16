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

import java.io.*;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;

/**
 * An endpoint which receives messages from the NMR and writes the message to the file system.
 *
 * @org.apache.xbean.XBean element="sender"
 *
 * @version $Revision: $
*/
public class FileSenderEndpoint extends ProviderEndpoint implements FileEndpointType {

    private File directory;
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private String tempFilePrefix = "servicemix-";
    private String tempFileSuffix = ".xml";
    private boolean autoCreateDirectory = true;
    private boolean append = true;


    public FileSenderEndpoint() {
        append = false;
    }

    public FileSenderEndpoint(FileComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
        append = false;
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

    protected void processInOnly(MessageExchange exchange, NormalizedMessage in) throws Exception {
        OutputStream out = null;
        File newFile = null;
        boolean success = false;
        try {
            String name = marshaler.getOutputName(exchange, in);
            if (name == null) {
                newFile = File.createTempFile(tempFilePrefix, tempFileSuffix, directory);
            } else {
                newFile = new File(directory, name);
            }
            if (!newFile.getParentFile().exists() && isAutoCreateDirectory()) {
                newFile.getParentFile().mkdirs();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Writing to file: " + newFile.getCanonicalPath());
            }
            out = new BufferedOutputStream(new FileOutputStream(newFile, append));
            marshaler.writeMessage(exchange, in, out, name);
            success = true;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("Caught exception while closing stream on error: " + e, e);
                }
            }
            //cleaning up incomplete files after things went wrong
            if (!success) {
                logger.debug("An error occurred while writing file " + newFile.getCanonicalPath() + ", deleting the invalid file");
                if (!newFile.delete()) {
                    logger.warn("Unable to delete the file " + newFile.getCanonicalPath() + " after an error had occurred");
                }
            }
        }
    }

    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        /** TODO list the files? */
        super.processInOut(exchange, in, out);
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
            * Returns the <code>File</code> object for the directory where the 
            * endpoint writes files.
            */
    public File getDirectory() {
        return directory;
    }

    /**
            * Specifies the directory where the endpoint writes files.
            *
            * @param directory a <code>File</code> object representing the directory
            * @org.apache.xbean.Property description="the relative path of the directory to which the endpoint writes files"
            */
    public void setDirectory(File directory) {
        this.directory = directory;
    }

    /**
            * Returns the object responsible for marshaling message data into 
            * files.
            */
    public FileMarshaler getMarshaler() {
        return marshaler;
    }

    /**
            * Specifies a <code>FileMarshaler</code> object that will marshal 
            * message data from the NMR into a file. The default file 
            * marshaller can write valid XML data. <code>FileMarshaler</code> 
            * objects are implementations of 
            * <code>org.apache.servicemix.components.util.FileMarshaler</code>.
            *
            * @param marshaler a <code>FileMarshaler</code> object that can write message data to the file system
            * @org.apache.xbean.Property description="the bean defining the class used to marshal messages to the file system"
            */
    public void setMarshaler(FileMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    /**
           * Returns the string attached to the begining of generated temporary 
           * file names.
           */
    public String getTempFilePrefix() {
        return tempFilePrefix;
    }

    /**
            * Specifies a string to attach to the begining of generated 
            * temporary file names. Temporary file names are generated when the 
            * endpoint cannot determine the name of the file from the message.
            *
            * @param tempFilePrefix a string to prefix to generated file names
            * @org.apache.xbean.Property description="a string that will be attached to the begining of any temporary file names. Temporary file names are generated when the endpoint cannot determine the name of the file from the message data."
            */         
    public void setTempFilePrefix(String tempFilePrefix) {
        this.tempFilePrefix = tempFilePrefix;
    }

    /**
           * Returns the string attached to the end of generated temporary 
           * file names.
           */
    public String getTempFileSuffix() {
        return tempFileSuffix;
    }

    /**
            * Specifies a string to append to generated temporary file names. 
            * Temporary file names are generated when the endpoint cannot 
            * determine the name of the file from the message.
            *
            * @param tempFileSuffix a string to append to generated file names
            * @org.apache.xbean.Property description="a string that will be appended to any temporary file names. Temporary file names are generated when the endpoint cannot determine the name of the file from the message data."
            */         
    public void setTempFileSuffix(String tempFileSuffix) {
        this.tempFileSuffix = tempFileSuffix;
    }

    /** Returns wheter the endpoint should create the target directory  
            * if it does not exist.
            */
    public boolean isAutoCreateDirectory() {
        return autoCreateDirectory;
    }

    /**
            * Specifies if the endpoint should create the target directory if 
            * it does not exist.  If you set this to 
            * <code>false</code> and the directory does not exist, the endpoint 
            * will not do anything.
            *
            * @param autoCreateDirectory a boolean specifying if the endpoint creates directories
            * @org.apache.xbean.Property description="specifies if directories are created. The defualt is <code>true</code>."
            */
    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    /**
           * Returns whether the endpoint will append data to an existing file. 
           */
    public boolean isAppend() {
        return append;
    }

    /**
            * Specifies if the endpoint appends data to existing files or if 
            * it will overwrite existing files. The default is for the endpoint 
            * to overwrite existing files. Setting this to <code>true</code> 
            * instructs the endpoint to append data.
            *
            * @param append a boolean specifying if the endpoint appends data to existing files
            * @org.apache.xbean.Property description="specifies if data is appended to existing files. The defualt is <code>false</code>."
            */
    public void setAppend(boolean append) {
        this.append = append;
    }

}