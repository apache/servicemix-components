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
import javax.jbi.management.DeploymentException;
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
    private static final Log log = LogFactory.getLog(VFSPollingEndpoint.class);
    
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private FileObjectEditor editor = new FileObjectEditor();
    private FileObject file;
    private FileSelector selector;
    private Set<FileObject> workingSet = new CopyOnWriteArraySet<FileObject>();
    private boolean deleteFile = true;
    private boolean recursive = true;
    private LockManager lockManager;
    private ConcurrentMap<String, InputStream> openExchanges = new ConcurrentHashMap<String, InputStream>();
    
    public VFSPollingEndpoint() {
    }

    public VFSPollingEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public VFSPollingEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();

        this.workingSet.clear();
        
        // re-create the openExchanges map
        this.openExchanges = new ConcurrentHashMap<String, InputStream>();
        
        if (file == null) {
            file = editor.getFileObject();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        super.validate();
        
        if (lockManager == null) {
            lockManager = createLockManager();
        }
    }

    protected LockManager createLockManager() {
        return new SimpleLockManager();
    }
    
    public String getLocationURI() {
        return getPath();
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
                //workingSet.remove(aFile);
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
    
    protected void pollFileOrDirectory(FileObject fileOrDirectory) throws Exception {
        pollFileOrDirectory(fileOrDirectory, true);
    }
    
    protected void pollFileOrDirectory(FileObject fileOrDirectory, boolean processDir) throws Exception {
        if (fileOrDirectory.getType().equals(FileType.FILE)) {
            pollFile(fileOrDirectory); // process the file
        } else if (processDir) {
            logger.debug("Polling directory " + fileOrDirectory.getName().getPathDecoded());
            FileObject[] files = null;
            if (selector != null) {
                files = fileOrDirectory.findFiles(selector);
            } else {
                files = fileOrDirectory.getChildren();
            }
            for (FileObject f : files) {
                pollFileOrDirectory(f, isRecursive()); // self-recursion
            }
        } else {
            logger.debug("Skipping directory " + fileOrDirectory.getName().getPathDecoded());
        }
    }
    
    protected void pollFile(final FileObject aFile) throws Exception {
        if (workingSet.add(aFile)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Scheduling file " + aFile.getName().getPathDecoded() + " for processing");
            }
            
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
     * sets the path of the file object
     * 
     * @param path      the path        
     */
    public void setPath(String path) {
        editor.setPath(path);
    }

    public String getPath() {
        return editor.getPath();
    }

    /**
     * sets the file system manager
     * 
     * @param fileSystemManager the file system manager
     */
    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        editor.setFileSystemManager(fileSystemManager);
    }

    public FileSystemManager getFileSystemManager() {
        return editor.getFileSystemManager();
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
