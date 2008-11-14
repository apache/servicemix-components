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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.common.locks.LockManager;
import org.apache.servicemix.common.locks.impl.SimpleLockManager;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;

/**
 * A polling endpoint that looks for a file or files in a directory and sends
 * the files to a target service (via the JBI bus), deleting the files by
 * default when they are processed. The polling endpoint uses a file marshaler
 * to send the data as a JBI message; be default this marshaler expects XML
 * payload. For non-XML payload, e.g. plain-text or binary files, use an
 * alternative marshaler such as the
 * <code>org.apache.servicemix.components.util.BinaryFileMa
 * @org.apache.xbean.XBean element="poller"
 *
 * @version $Revision$
 */
public class FilePollerEndpoint extends PollingEndpoint implements FileEndpointType {

    private File file;
    private FileFilter filter;
    private boolean deleteFile = true;
    private boolean recursive = true;
    private boolean autoCreateDirectory = true;
    private File archive;
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private LockManager lockManager;
    private ConcurrentMap<String, InputStream> openExchanges = new ConcurrentHashMap<String, InputStream>();

    public FilePollerEndpoint() {
    }

    public FilePollerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public FilePollerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();
        
        // re-create the openExchanges map
        this.openExchanges = new ConcurrentHashMap<String, InputStream>();
    }
    
    public void poll() throws Exception {
        pollFileOrDirectory(file);
    }

    public void validate() throws DeploymentException {
        super.validate();
        if (file == null) {
            throw new DeploymentException("You must specify a file property");
        }
        if (isAutoCreateDirectory() && !file.exists()) {
            file.mkdirs();
        }
        if (archive != null) {
            if (!deleteFile) {
                throw new DeploymentException("Archive shouldn't be specified unless deleteFile='true'");
            }
            if (isAutoCreateDirectory() && !archive.exists()) {
                archive.mkdirs();
            }
            if (!archive.isDirectory()) {
                throw new DeploymentException("Archive should refer to a directory");
            }
        }
        if (lockManager == null) {
            lockManager = createLockManager();
        }
    }
    
    protected LockManager createLockManager() {
        return new SimpleLockManager();
    }


    // Properties
	// -------------------------------------------------------------------------

    /**
	 * Specifies the file or directory to be polled. If it is a directory, all
	 * files in the directory or its sub-directories will be processed by the
	 * endpoint. If it is a file, only files matching the filename will be
	 * processed."
	 * 
	 * @param file
	 *            a <code>File</code> object representing the directory or file
	 *            to poll
	 */
    public void setFile(File file) {
        this.file = file;
    }
    
	public File getFile() {
		return file;
	}

    /**
	 * Bean defining the class implementing the file locking strategy. This bean
	 * must be an implementation of the
	 * <code>org.apache.servicemix.locks.LockManager</code> interface. By
	 * default, this will be set to an instances of
	 * <code>org.apache.servicemix.common.locks.impl.SimpleLockManager</code>.
	 * 
	 * @param lockManager
	 *            the <code>LockManager</code> implementation to use
	 */
	public void setLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}

	public LockManager getLockManager() {
		return lockManager;
	}
    
    /**
	 * Bean defining the class implementing the file filtering strategy. This
	 * bean must be an implementation of the <code>java.io.FileFilter</code>
	 * interface.
	 * 
	 * @param filter
	 *            a <code>FileFilter</code> implementation defining the
	 *            endpoint's filtering logic
	 * 
	 */
	public void setFilter(FileFilter filter) {
		this.filter = filter;
	}

	public FileFilter getFilter() {
		return filter;
	}

	
    /**
     * Specifies if files should be deleted after they are processed. Default value is <code>true</code>.
     * 
	 * @param deleteFile
	 *            a boolean specifying if the file should be deleted
	 *            
	 */
	public void setDeleteFile(boolean deleteFile) {
		this.deleteFile = deleteFile;
	}

    public boolean isDeleteFile() {
        return deleteFile;
    }

    
    /**
	 * Specifies if sub-directories are polled; if false then the poller will
	 * only poll the specified directory. If the endpoint is configured to poll
	 * for a specific file rather than a directory then this attribute is
	 * ignored. Default is <code>true</code>.
	 * 
	 * @param recursive
	 *            a baolean specifying if sub-directories should be polled
	 * 
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	
	public boolean isRecursive() {
		return recursive;
	}

    /**
	 * Specifies if the endpoint should create the target directory, if it does
	 * not already exist. If you set this to <code>false</code> and the directory does
	 * not exist, the endpoint will not do anything. Default value is <code>true</code>.
	 * 
	 * @param autoCreateDirectory
	 *            a boolean specifying if the endpoint creates directories.
	 * 
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}
	
	public boolean isAutoCreateDirectory() {
		return autoCreateDirectory;
	}


    /**
	 * Specifies a <code>FileMarshaler</code> object that will marshal file data
	 * into the NMR. The default file marshaller can read valid XML data.
	 * <code>FileMarshaler</code> objects are implementations of
	 * <code>org.apache.servicemix.components.util.FileMarshaler</code>.
	 * 
	 * @param marshaler
	 *            a <code>FileMarshaler</code> object that can read data from
	 *            the file system.
	 */
	public void setMarshaler(FileMarshaler marshaler) {
		this.marshaler = marshaler;
	}

	public FileMarshaler getMarshaler() {
		return marshaler;
	}
    
    /**
	 * Specifies a directory relative to the polling directory to which
	 * processed files are archived.
	 * 
	 * @param archive
	 *            a <code>File</code> object for the archive directory
	 */
	public void setArchive(File archive) {
		this.archive = archive;
	}

	public File getArchive() {
		return archive;
	}
    
    // Implementation methods
    //-------------------------------------------------------------------------


    protected void pollFileOrDirectory(File fileOrDirectory) {
        pollFileOrDirectory(fileOrDirectory, true);
    }

    protected void pollFileOrDirectory(File fileOrDirectory, boolean processDir) {
        if (!fileOrDirectory.isDirectory()) {
            pollFile(fileOrDirectory); // process the file
        } else if (processDir) {
            logger.debug("Polling directory " + fileOrDirectory);
            File[] files = fileOrDirectory.listFiles(getFilter());
            for (int i = 0; i < files.length; i++) {
                pollFileOrDirectory(files[i], isRecursive()); // self-recursion
            }
        } else {
            logger.debug("Skipping directory " + fileOrDirectory);
        }
    }

    protected void pollFile(final File aFile) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduling file " + aFile + " for processing");
        }
        getExecutor().execute(new Runnable() {
            public void run() {
                String uri = file.toURI().relativize(aFile.toURI()).toString();
                Lock lock = lockManager.getLock(uri);
                if (lock.tryLock()) {
                    processFileNow(aFile);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to acquire lock on " + aFile);
                    }
                }
            }
        });
    }

    protected void processFileNow(File aFile) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Processing file " + aFile);
            }
            if (aFile.exists()) {
                processFile(aFile);
            }
        } catch (Exception e) {
            logger.error("Failed to process file: " + aFile + ". Reason: " + e, e);
        }
    }

    protected void processFile(File file) throws Exception {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        InOnly exchange = getExchangeFactory().createInOnlyExchange();
        configureExchangeTarget(exchange);
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        marshaler.readMessage(exchange, message, stream, file.getCanonicalPath());

        //sending the file itself along as a message property and holding on to the stream we opened
        exchange.getInMessage().setProperty(FileComponent.FILE_PROPERTY, file);
        this.openExchanges.put(exchange.getExchangeId(), stream);
        
        send(exchange);
    }

    public String getLocationURI() {
        return file.toURI().toString();
    }

    public void process(MessageExchange exchange) throws Exception {
        // check for done or error
        if (this.openExchanges.containsKey(exchange.getExchangeId())) {
            InputStream stream = this.openExchanges.get(exchange.getExchangeId());
            File aFile = (File) exchange.getMessage("in").getProperty(FileComponent.FILE_PROPERTY);
            
            if (aFile == null) {
                throw new JBIException("Property org.apache.servicemix.file was removed from the exchange -- unable to delete/archive the file");
            }

            logger.debug("Releasing " + aFile.getAbsolutePath());
            //first try to close the stream 
            stream.close();
            try {
                // check for state
                if (exchange.getStatus() == ExchangeStatus.DONE) {
                    if (isDeleteFile()) {
                        if (archive != null) {
                            moveFile(aFile, archive);
                        } else {
                            if (!aFile.delete()) {
                                throw new IOException("Could not delete file " + aFile);
                            }
                        }
                    } 
                } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                    Exception e = exchange.getError();
                    if (e == null) {
                        throw new JBIException("Received an exchange with status ERROR, but no exception was set");
                    }
                    logger.warn("Message in file " + aFile + " could not be handled successfully: " + e.getMessage(), e);
                } else {
                    //we should never get an ACTIVE exchange -- the File poller only sends InOnly exchanges
                    throw new JBIException("Unexpectedly received an exchange with status ACTIVE");
                }
            } finally {
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
     * @param file      the file to unlock
     */
    private void unlockAsyncFile(File file) {
        // finally remove the file from the open exchanges list
        String uri = this.file.toURI().relativize(file.toURI()).toString();
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

    /**
     * Move a File
     * 
     * @param src
     * @param targetDirectory
     * @throws IOException 
     */
    public static void moveFile(File src, File targetDirectory) throws IOException {
        if (!src.renameTo(new File(targetDirectory, src.getName()))) {
            throw new IOException("Failed to move " + src + " to " + targetDirectory);
        }
    }
}
