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
package org.apache.servicemix.truezip;

// import java.io.BufferedInputStream;
// import java.io.File;
// import java.io.FileFilter;
// import java.io.FileInputStream;
// import java.io.IOException;
// import java.io.InputStream;

import java.io.BufferedInputStream;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;
import org.apache.servicemix.locks.LockManager;
import org.apache.servicemix.locks.impl.SimpleLockManager;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;

/**
 * A polling endpoint which looks for a file or files in a directory and sends
 * the files into the JBI bus as messages, deleting the files by default when
 * they are processed.
 * 
 * @org.apache.xbean.XBean element="poller"
 * 
 * @version $Revision$
 */
public class TrueZipPollerEndpoint extends PollingEndpoint implements TrueZipEndpointType {

    private File file;

    private FileFilter filter;

    private boolean deleteFile = true;

    private boolean recursive = true;

    private boolean autoCreateDirectory = true;

    private FileMarshaler marshaler = new DefaultFileMarshaler();

    private LockManager lockManager;

    public TrueZipPollerEndpoint() {
    }

    public TrueZipPollerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public TrueZipPollerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
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
        if (lockManager == null) {
            lockManager = createLockManager();
        }
    }

    protected LockManager createLockManager() {
        return new SimpleLockManager();
    }

    // Properties
    // -------------------------------------------------------------------------
    public java.io.File getFile() {
        return file;
    }

    /**
     * Sets the file to poll, which can be a directory or a file.
     * 
     * @param file
     */
    public void setFile(java.io.File file) {
        this.file = new File(file);
    }

    /**
     * @return the lockManager
     */
    public LockManager getLockManager() {
        return lockManager;
    }

    /**
     * @param lockManager
     *            the lockManager to set
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public FileFilter getFilter() {
        return filter;
    }

    /**
     * Sets the optional filter to choose which files to process
     */
    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    /**
     * Returns whether or not we should delete the file when its processed
     */
    public boolean isDeleteFile() {
        return deleteFile;
    }

    public void setDeleteFile(boolean deleteFile) {
        this.deleteFile = deleteFile;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean isAutoCreateDirectory() {
        return autoCreateDirectory;
    }

    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    public FileMarshaler getMarshaler() {
        return marshaler;
    }

    public void setMarshaler(FileMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    protected void pollFileOrDirectory(File fileOrDirectory) {
        pollFileOrDirectory(fileOrDirectory, true);
    }

    protected void pollFileOrDirectory(File fileOrDirectory, boolean processDir) {
        if (!fileOrDirectory.isDirectory()) {
            pollFile(fileOrDirectory); // process the file
        } else if (processDir) {
            logger.debug("Polling directory " + fileOrDirectory);
            File[] files = (File[]) fileOrDirectory.listFiles(getFilter());
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
                    try {
                        processFileAndDelete(aFile);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unable to acquire lock on " + aFile);
                    }
                }
            }
        });
    }

    protected void processFileAndDelete(File aFile) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Processing file " + aFile);
            }
            if (aFile.exists()) {
                processFile(aFile);
                if (isDeleteFile()) {
                    if (!aFile.delete()) {
                        throw new IOException("Could not delete file " + aFile);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process file: " + aFile + ". Reason: " + e, e);
        }
    }

    protected void processFile(File aFile) throws Exception {
        String name = aFile.getCanonicalPath();
        InputStream in = new BufferedInputStream(new FileInputStream(aFile));
        InOnly exchange = getExchangeFactory().createInOnlyExchange();
        configureExchangeTarget(exchange);
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        marshaler.readMessage(exchange, message, in, name);
        sendSync(exchange);
        in.close();
    }

    public String getLocationURI() {
        return file.toURI().toString();
    }

    public void process(MessageExchange exchange) throws Exception {
        // Do nothing. In our case, this method should never be called
        // as we only send synchronous InOnly exchange
    }
}
