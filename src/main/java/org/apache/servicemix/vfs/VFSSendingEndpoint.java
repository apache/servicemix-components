/**
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
package org.apache.servicemix.vfs;

import java.io.IOException;
import java.io.OutputStream;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;

/**
 * An endpoint which receives messages from the NMR and writes the message to 
 * the virtual file system.
 *
 * @org.apache.xbean.XBean element="sender"
 * 
 * @author lhein
 */
public class VFSSendingEndpoint extends ProviderEndpoint implements VFSEndpointType {
    private static final Log log = LogFactory.getLog(VFSSendingEndpoint.class);

    private FileObject directory;
    private FileObjectEditor editor = new FileObjectEditor();
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private String uniqueFileName = "ServiceMix";
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();
        
        if (directory == null) {
            directory = editor.getFileObject();
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        NormalizedMessage message =exchange.getMessage("in");
        OutputStream out = null;
        try {
            String name = marshaler.getOutputName(exchange, message);
            if (name == null) {
                throw new MessagingException("No output name available. Cannot output message!");
            }
            directory.close(); // remove any cached informations
            FileObject newFile = directory.resolveFile(name);
            newFile.close(); // remove any cached informations
            FileContent content = newFile.getContent();
            content.close();
            if (content != null) {
                out = content.getOutputStream();
            }
            if (out == null) {
                throw new MessagingException("No output stream available for output name: " + name);
            }
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
    
    public String getPath() {
        return editor.getPath();
    }

    public void setPath(String path) {
        editor.setPath(path);
    }

    public FileSystemManager getFileSystemManager() {
        return editor.getFileSystemManager();
    }

    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        editor.setFileSystemManager(fileSystemManager);
    }

    public FileMarshaler getMarshaler() {
        return marshaler;
    }

    public void setMarshaler(FileMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public String getUniqueFileName() {
        return uniqueFileName;
    }

    /**
     * Sets the name used to make a unique name if no file name is available on the message.
     *
     * @param uniqueFileName the new value of the unique name to use for generating unique names
     */
    public void setUniqueFileName(String uniqueFileName) {
        this.uniqueFileName = uniqueFileName;
    }
}
