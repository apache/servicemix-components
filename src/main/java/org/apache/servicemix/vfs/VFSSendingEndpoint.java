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
    private static final Log logger = LogFactory.getLog(VFSSendingEndpoint.class);

    private FileObject file;
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private String path;
    private FileSystemManager fileSystemManager;
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();
        
        if (file == null) {
            file = FileObjectResolver.resolveToFileObject(getFileSystemManager(), getPath());
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
            file.close(); // remove any cached informations
            FileObject newFile = file.resolveFile(name);
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
                    logger.error("Caught exception while closing stream on error: " + e, e);
                }
            }
        }
    }
    
    /**
     * Specifies a <code>String</code> object representing the path of the 
     * file/folder to be polled.<br /><br />
     * <b><u>Examples:</u></b><br />
     * <ul>
     *  <li>file:///home/lhein/pollFolder</li>
     *  <li>zip:file:///home/lhein/pollFolder/myFile.zip</li>
     *  <li>jar:http://www.myhost.com/files/Examples.jar</li>
     *  <li>jar:../lib/classes.jar!/META-INF/manifest.mf</li>
     *  <li>tar:gz:http://anyhost/dir/mytar.tar.gz!/mytar.tar!/path/in/tar/README.txt</li>
     *  <li>tgz:file://anyhost/dir/mytar.tgz!/somepath/somefile</li>
     *  <li>gz:/my/gz/file.gz</li>
     *  <li>http://myusername@somehost/index.html</li>
     *  <li>webdav://somehost:8080/dist</li>
     *  <li>ftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz</li>
     *  <li>sftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz</li>
     *  <li>smb://somehost/home</li>
     *  <li>tmp://dir/somefile.txt</li>
     *  <li>res:path/in/classpath/image.png</li>
     *  <li>ram:///any/path/to/file.txt</li>
     *  <li>mime:file:///your/path/mail/anymail.mime!/filename.pdf</li>
     * </ul>
     * 
     * For further details have a look at {@link http://commons.apache.org/vfs/filesystems.html}.
     * <br /><br />
     * 
     * @param path a <code>String</code> object that represents a file/folder/vfs
     */
    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    /**
     * sets the file system manager
     * 
     * @param fileSystemManager the file system manager
     */
    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public FileSystemManager getFileSystemManager() {
        return this.fileSystemManager;
    }

    /**
     * Specifies a <code>FileMarshaler</code> object that will marshal file data
     * into the NMR. The default file marshaller can read valid XML data.
     * <code>FileMarshaler</code> objects are implementations of
     * <code>org.apache.servicemix.components.util.FileMarshaler</code>.
     * 
     * @param marshaler a <code>FileMarshaler</code> object that can read data
     *            from the file system.
     */
    public void setMarshaler(FileMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public FileMarshaler getMarshaler() {
        return marshaler;
    }
}
