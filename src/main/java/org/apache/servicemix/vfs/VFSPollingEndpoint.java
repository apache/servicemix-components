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
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.common.locks.LockManager;
import org.apache.servicemix.common.locks.impl.SimpleLockManager;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;

/**
 * A polling endpoint that looks for a file or files in a virtual file system 
 * and sends the files to a target service (via the JBI bus), deleting the files 
 * by default when they are processed. The polling endpoint uses a file marshaler
 * to send the data as a JBI message; by default this marshaler expects XML
 * payload. For non-XML payload, e.g. plain-text or binary files, use an
 * alternative marshaler such as the 
 * <code>org.apache.servicemix.components.util.BinaryFileMarshaler</code>
 * 
 * @org.apache.xbean.XBean element="poller"
 * 
 * @author lhein
 */
public class VFSPollingEndpoint extends PollingEndpoint implements VFSEndpointType {
    private static final Log logger = LogFactory.getLog(VFSPollingEndpoint.class);
    
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private FileObject file;
    private FileSelector selector;
    private Set<FileObject> workingSet = new CopyOnWriteArraySet<FileObject>();
    private boolean deleteFile;
    private boolean recursive;
    private String path;
    private FileSystemManager fileSystemManager;
    private LockManager lockManager;
    private ConcurrentMap<String, InputStream> openExchanges = new ConcurrentHashMap<String, InputStream>();
    
    /**
     * default constructor
     */
    public VFSPollingEndpoint() {
    }

    /**
     * creates a VFS polling endpoint
     * 
     * @param serviceUnit       the service unit
     * @param service           the service name
     * @param endpoint          the endpoint name
     */
    public VFSPollingEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    /**
     * creates a VFS polling endpoint
     * 
     * @param component         the default component
     * @param endpoint          the endpoint
     */
    public VFSPollingEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();

        // clear the set of already processed files
        this.workingSet.clear();
        
        // re-create the openExchanges map
        this.openExchanges = new ConcurrentHashMap<String, InputStream>();
        
        // resolve the path to a FileObject
        if (file == null) {
            file = FileObjectResolver.resolveToFileObject(getFileSystemManager(), getPath());
        }
        
        // create a lock manager
        if (lockManager == null) {
            lockManager = createLockManager();
        }
    }
    
    /**
     * returns the lock manager
     * 
     * @return  the lock manager
     */
    protected LockManager createLockManager() {
        return new SimpleLockManager();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#getLocationURI()
     */
    @Override
    public String getLocationURI() {
        // return a URI that unique identify this endpoint
        return getService() + "#" + getEndpoint();
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        // check for done or error
        if (this.openExchanges.containsKey(exchange.getExchangeId())) {
            InputStream stream = this.openExchanges.get(exchange.getExchangeId());
            FileObject aFile = (FileObject)exchange.getMessage("in").getProperty(VFSComponent.VFS_PROPERTY);

            if (aFile == null) {
                throw new JBIException(
                                       "Property org.apache.servicemix.vfs was removed from the exchange -- unable to delete/archive the file");
            }

            logger.debug("Releasing " + aFile.getName().getPathDecoded());
        
            // first try to close the stream
            stream.close();
            try {
                // check for state
                if (exchange.getStatus() == ExchangeStatus.DONE) {
                    if (isDeleteFile()) {
                        if (!aFile.delete()) {
                            throw new IOException("Could not delete file " + aFile.getName().getPathDecoded());
                        }
                    }
                } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                    Exception e = exchange.getError();
                    if (e == null) {
                        throw new JBIException(
                                               "Received an exchange with status ERROR, but no exception was set");
                    }
                    logger.warn("Message in file " + aFile.getName().getPathDecoded() + " could not be handled successfully: "
                                + e.getMessage(), e);
                } else {
                    // we should never get an ACTIVE exchange -- the File poller
                    // only sends InOnly exchanges
                    throw new JBIException("Unexpectedly received an exchange with status ACTIVE");
                }
            } finally {
                // remove file from set of already processed files
                workingSet.remove(aFile);
                // remove the open exchange
                openExchanges.remove(exchange.getExchangeId());
                // unlock the file
                unlockAsyncFile(aFile);
            }
        } else {
            // strange, we don't know this exchange
            logger.debug("Received unknown exchange. Will be ignored...");
            return;
        }
    }
    
    /**
     * unlock the file
     * 
     * @param file the file to unlock
     */
    private void unlockAsyncFile(FileObject file) {
        // finally remove the file from the open exchanges list
        String uri = file.getName().getURI().toString();
        Lock lock = lockManager.getLock(uri);
        if (lock != null) {
            try {
                lock.unlock();
            } catch (Exception ex) {
                // can't release the lock
                logger.error(ex);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#poll()
     */
    @Override
    public void poll() throws Exception {
        // SM-192: Force close the file, so that the cached informations are cleared
        if (file != null) {
            file.close();
            pollFileOrDirectory(file);
        }        
    }
    
    /**
     * polls a file which is not clear to be a file or folder
     * 
     * @param fileOrDirectory   the file or folder object
     * @throws Exception        on IO errors
     */
    protected void pollFileOrDirectory(FileObject fileOrDirectory) throws Exception {
        pollFileOrDirectory(fileOrDirectory, true);
    }
    
    /**
     * recursive method for processing a file or a folder
     * 
     * @param fileOrDirectory   the file or folder object
     * @param processDir        flag if processing should act recursive
     * @throws Exception        on IO errors
     */
    protected void pollFileOrDirectory(FileObject fileOrDirectory, boolean processDir) throws Exception {
        // check if it is a file object
        if (fileOrDirectory.getType().equals(FileType.FILE)) {
            // process the file
            pollFile(fileOrDirectory); 
        } else if (processDir) {
            // process the folder
            logger.debug("Polling directory " + fileOrDirectory.getName().getPathDecoded());
            
            FileObject[] files = null;
            if (selector != null) {
                files = fileOrDirectory.findFiles(selector);
            } else {
                files = fileOrDirectory.getChildren();
            }
            // process each file inside folder
            for (FileObject f : files) {
                // self-recursion
                pollFileOrDirectory(f, isRecursive()); 
            }
        } else {
            logger.debug("Skipping directory " + fileOrDirectory.getName().getPathDecoded());
        }
    }
    
    /**
     * polls a file object
     * 
     * @param aFile     the file object
     * @throws Exception        on IO errors
     */
    protected void pollFile(final FileObject aFile) throws Exception {
        // check if file is fully available
        if (!isFullyAvailable(aFile)) {
            return;
        }
        // try to add to set of processed files
        if (workingSet.add(aFile)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Scheduling file " + aFile.getName().getPathDecoded() + " for processing");
            }
            
            // execute processing in another thread
            getExecutor().execute(new Runnable() {
                public void run() {
                    String uri = aFile.getName().getURI().toString();
                    Lock lock = lockManager.getLock(uri);
                    if (lock.tryLock()) {
                        processFileNow(aFile);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unable to acquire lock on " + aFile.getName().getURI());
                        }
                    }
                }
            });
        }
    }

    /**
     * processes a file
     * 
     * @param aFile     the file to process
     */
    protected void processFileNow(FileObject aFile) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Processing file " + aFile.getName().getURI());
            }
            
            if (aFile.exists()) {
                processFile(aFile);
            }
        } catch (Exception e) {
            logger.error("Failed to process file: " + aFile.getName().getURI() + ". Reason: " + e, e);
        }
    }

    /**
     * does the real processing logic
     * 
     * @param file              the file to process
     * @throws Exception        on processing errors
     */
    protected void processFile(FileObject file) throws Exception {
        // SM-192: Force close the file, so that the cached informations are cleared
        file.close();
        
        String name = file.getName().getURI();
        FileContent content = file.getContent();
        content.close();
       
        InputStream stream = content.getInputStream();
        if (stream == null) {
            throw new IOException("No input available for file!");
        }
        
        InOnly exchange = getExchangeFactory().createInOnlyExchange();
        configureExchangeTarget(exchange);
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        marshaler.readMessage(exchange, message, stream, name);
        
        // sending the file itself along as a message property and holding on to
        // the stream we opened
        exchange.getInMessage().setProperty(VFSComponent.VFS_PROPERTY, file);
        this.openExchanges.put(exchange.getExchangeId(), stream);

        send(exchange);
    }
    
    /**
     * checks if a file is available 
     * 
     * @param aFile     the file to check
     * @return          true if available
     */
    private boolean isFullyAvailable(FileObject aFile) {
        try {
            if (aFile.getContent() != null) {
                long size_old = aFile.getContent().getSize();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
                long size_new = aFile.getContent().getSize();
                return (size_old == size_new);
            }    
        } catch (Exception ex) {
            // ignore
        }
        // default to true
        return true;
    }
    
    /**
     * Specifies if files should be deleted after they are processed. Default
     * value is <code>true</code>.
     * 
     * @param deleteFile a boolean specifying if the file should be deleted
     */
    public void setDeleteFile(boolean deleteFile) {
        this.deleteFile = deleteFile;
    }

    public boolean isDeleteFile() {
        return deleteFile;
    }

    /**
     * Bean defining the class implementing the file locking strategy. This bean
     * must be an implementation of the
     * <code>org.apache.servicemix.locks.LockManager</code> interface. By
     * default, this will be set to an instances of
     * <code>org.apache.servicemix.common.locks.impl.SimpleLockManager</code>.
     * 
     * @param lockManager the <code>LockManager</code> implementation to use
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public LockManager getLockManager() {
        return lockManager;
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
    
    /**
     * Specifies a <code>FileSelector</code> object.
     * 
     * @param selector  a <code>FileSelector</code> object 
     */    
    public void setSelector(FileSelector selector) {
        this.selector = selector;
    }
    
    public FileSelector getSelector() {
        return selector;
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
     * The set of FTPFiles that this component is currently working on
     *
     * @return  a set of in-process file objects
     */
    public Set<FileObject> getWorkingSet() {
        return workingSet;
    }
    
    /** 
     * @return Returns the recursive.
     */
    public boolean isRecursive() {
        return this.recursive;
    }

    /**
     * @param recursive The recursive to set.
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

}
