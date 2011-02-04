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
package org.apache.servicemix.ftp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.StringTokenizer;
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

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.common.locks.LockManager;
import org.apache.servicemix.common.locks.impl.SimpleLockManager;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.components.util.FileMarshaler;

/**
 * A polling endpoint which looks for a file or files in a directory
 * and sends the files into the JBI bus as messages, deleting the files
 * by default when they are processed.
 *
 * @org.apache.xbean.XBean element="poller"
 *
 * @version $Revision: 468487 $
 */
public class FtpPollerEndpoint extends PollingEndpoint implements FtpEndpointType {

    private FTPClientPool clientPool;
    private FileFilter filter;  
    private boolean deleteFile = true;
    private boolean recursive = true;
    private boolean changeWorkingDirectory;
    private FileMarshaler marshaler = new DefaultFileMarshaler();
    private LockManager lockManager;
    private ConcurrentMap<String, FtpData> openExchanges = new ConcurrentHashMap<String, FtpData>();
    private QName targetOperation;
    private URI uri;
    private boolean stateless = true;
    private URI archive;
    private boolean autoCreateDirectory = true;

    protected class FtpData {
        final String file;
        final FTPClient ftp;
        final InputStream in;
        public FtpData(String file, FTPClient ftp, InputStream in) {
            this.file = file;
            this.ftp = ftp;
            this.in = in;
        }
    }

    public FtpPollerEndpoint() {
    }

    public FtpPollerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    /**
     * Bean defining the class implementing the JBI DefaultComponent. This bean
     * must be an implementation of the
     * <code>org.apache.servicemix.common.DefaultComponent</code> class.
     *
     * @param component the <code>component</code> implementation to use
     */
    public FtpPollerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public void poll() throws Exception {
        pollFileOrDirectory(getWorkingPath());
    }

    public void validate() throws DeploymentException {
        super.validate();
        if (uri == null && (getClientPool() == null || getClientPool().getHost() == null)) {
            throw new DeploymentException("Property uri or clientPool.host must be configured");
        }
        if (uri != null && getClientPool() != null && getClientPool().getHost() != null) {
            throw new DeploymentException("Properties uri and clientPool.host can not be configured at the same time");
        }
        if (changeWorkingDirectory && recursive) {
            throw new DeploymentException("changeWorkingDirectory='true' can not be set when recursive='true'");
        }
        if (archive != null && archive.getPath() == null) {
            throw new DeploymentException("Archive specified without path information.");
        }            
        if (archive != null) {
            if (!deleteFile) {
                throw new DeploymentException("Archive shouldn't be specified unless deleteFile='true'");
            }
        }
    }

    @Override
	public synchronized void activate() throws Exception {
		if (uri == null && clientPool != null) {
			String str = "ftp://" + clientPool.getHost();
			if (clientPool.getPort() >= 0) {
				str += ":" + clientPool.getPort();
			}
			str += "/";
			uri = new URI(str);
		}
		super.activate();
	}

	public void start() throws Exception {
        if (lockManager == null) {
            lockManager = createLockManager();
        }
        if (clientPool == null) {
            clientPool = createClientPool();
        }
        if (uri != null) {
            clientPool.setHost(uri.getHost());
            clientPool.setPort(uri.getPort());
            if (uri.getUserInfo() != null) {
                String[] infos = uri.getUserInfo().split(":");
                clientPool.setUsername(infos[0]);
                if (infos.length > 1) {
                    clientPool.setPassword(infos[1]);
                }
            }
        } 
        
        // borrow client from pool
        FTPClient ftp = borrowClient();
        String folderName = "";
        try {
            StringTokenizer strTok = null;
            if (isAutoCreateDirectory() && !ftp.changeWorkingDirectory(getWorkingPath())) {
                // it seems the folder isn't there, so create it
                strTok = new StringTokenizer(getWorkingPath(), "/");
                
                while (strTok.hasMoreTokens()) {
                    folderName += '/';
                    folderName += strTok.nextToken();
                    if (!ftp.changeWorkingDirectory(folderName)) {
                        if (ftp.makeDirectory(folderName)) {
                            // the folder now exists
                        } else {
                            // unable to create the folder
                            throw new IOException("The defined folder " + getWorkingPath() + " doesn't exist on the server and it can't be created automatically.");
                        }
                    }
                }
            }
            folderName = "";
            if (getArchivePath() != null) {
                if (isAutoCreateDirectory() && !ftp.changeWorkingDirectory(getArchivePath())) {
                    // it seems the folder isn't there, so create it
                    strTok = new StringTokenizer(getArchivePath(), "/");

                    while (strTok.hasMoreTokens()) {
                        folderName += '/';
                        folderName += strTok.nextToken();
                        if (!ftp.changeWorkingDirectory(folderName)) {
                            if (ftp.makeDirectory(folderName)) {
                                // the folder now exists
                            } else {
                                // unable to create the folder
                                throw new IOException("The defined archive folder " + getArchivePath() + " doesn't exist on the server and it can't be created automatically.");
                            }
                        }
                    }
                }
            }
        } finally {
            // give back the client
            returnClient(ftp);
        }

        super.start();
    }

    protected LockManager createLockManager() {
        return new SimpleLockManager();
    }
    
    private String getArchivePath() {
        return (archive != null && archive.getPath() != null) ? archive.getPath() : null;
    }

    private String getWorkingPath() {
        return (uri != null && uri.getPath() != null) ? uri.getPath() : ".";
    }

    // Properties
    //-------------------------------------------------------------------------
    /**
     * @return the clientPool
     */
    public FTPClientPool getClientPool() {
        return clientPool;
    }

    /**
     * Set a custom FTPClientPool.  If this property has not been set, the FTP client pool will be created based on the information
     * provided in the URI.
     *
     * @param clientPool the <code>clientPool</code> implementation to use
     */
    public void setClientPool(FTPClientPool clientPool) {
        this.clientPool = clientPool;
    }

    /**
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Configures the endpoint from a URI.
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    public FileFilter getFilter() {
        return filter;
    }

    /**
     * Sets the filter to select which files have to be processed.  When not set, all files will be picked up by the poller.
     */
    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    public boolean isDeleteFile() {
        return deleteFile;
    }

    /**
     * Delete the file after it has been succesfully processed?  Defaults to <code>true</code>
     *
     * @param deleteFile
     */
    public void setDeleteFile(boolean deleteFile) {
        this.deleteFile = deleteFile;
    }

    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Specifies whether subdirectories should be polled.  Defaults to <code>true</code>
     *
     * @param recursive is a boolean specifying whether subdirectories should be polled.  Default value is true.
     * .
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public FileMarshaler getMarshaler() {
        return marshaler;
    }

    /**
     * Set a custom FileMarshaler implementation to control how the file contents is being translated into a JBI message.
     * The default implementation reads XML contents from the file.
     *
     * @param marshaler the <code>marshaler</code> implementation to use
     */
    public void setMarshaler(FileMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public QName getTargetOperation() {
        return targetOperation;
    }

    /**
     * Set the operation to be invoked on the target service.
     *
     * @param targetOperation a QName specifiying the name of the target operation
     */
    public void setTargetOperation(QName targetOperation) {
        this.targetOperation = targetOperation;
    }

    /**
     * When set to <code>true</code>, the poller will do an explicit <code>cwd</code> into the directory to be polled.
     * Default to <code>false</code>.  Recursive polling will not be possible if this feature is enabled.
     *
     * @param changeWorkingDirectory is a boolean specifying if the endpoint can change the working directory
     * .
     */
    public void setChangeWorkingDirectory(boolean changeWorkingDirectory) {
        this.changeWorkingDirectory = changeWorkingDirectory;
    }

    public boolean isStateless() {
        return stateless;
    }

    /**
     * When set to <code>false</code>
     *
     * @param stateless is a boolean specifying whether the polled file should be sent asynchronous or synchronous to the nmr.  Default value is true.
     * .
     */
    public void setStateless(boolean stateless) {
        this.stateless = stateless;
    }
    
    /**
     * Specifies if the endpoint should create the target directory, if it does
     * not already exist. If you set this to <code>false</code> and the
     * directory does not exist, the endpoint will not do anything. Default
     * value is <code>true</code>.
     * 
     * @param autoCreateDirectory a boolean specifying if the endpoint creates
     *            directories.
     */
    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    public boolean isAutoCreateDirectory() {
        return autoCreateDirectory;
    }
    
    /**
     * Specifies a directory relative to the polling directory to which
     * processed files are archived.
     * 
     * @param archive a <code>URI</code> object for the archive directory
     */
    public void setArchive(URI archive) {
        this.archive = archive;
    }

    public URI getArchive() {
        return archive;
    }

    /**
     * Set a custom LockManager implementation for keeping track of which files are already being processed.
     * The default implementation is a simple, in-memory lock management system.
     * 
     * @param lockManager the <code>LockManager</code> implementation to use
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    protected void pollFileOrDirectory(String fileOrDirectory) throws Exception {
        FTPClient ftp = borrowClient();
        try {
            logger.debug("Polling directory " + fileOrDirectory);
            pollFileOrDirectory(ftp, fileOrDirectory, isRecursive());
        } finally {
            returnClient(ftp);
        }
    }

    protected void pollFileOrDirectory(FTPClient ftp, String fileOrDirectory, boolean processDir) throws Exception {
        FTPFile[] files = listFiles(ftp, fileOrDirectory);
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (".".equals(name) || "..".equals(name)) {
                continue; // ignore "." and ".."
            }
            String file = fileOrDirectory + "/" + name;
            // This is a file, process it
            if (!files[i].isDirectory()) {
                if (getFilter() == null || getFilter().accept(new File(file))) {
                    pollFile(file); // process the file
                }
                // Only process directories if processDir is true
            } else if (processDir) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Polling directory " + file);
                }
                pollFileOrDirectory(ftp, file, isRecursive());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping directory " + file);
                }
            }
        }
    }

    private FTPFile[] listFiles(FTPClient ftp, String directory) throws IOException {
        if (changeWorkingDirectory) {
            ftp.changeWorkingDirectory(directory);
            return ftp.listFiles();
        } else {
            return ftp.listFiles(directory);
        }
    }

    protected void pollFile(final String file) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scheduling file " + file + " for processing");
        }
        getExecutor().execute(new Runnable() {
            public void run() {
                final Lock lock = lockManager.getLock(file);
                if (lock.tryLock()) {
                    processFileNow(file);
                }
            }
        });
    }

    protected void processFileNow(String file) {
        FTPClient ftp = null;
        try {
            ftp = borrowClient();
            if (logger.isDebugEnabled()) {
                logger.debug("Processing file " + file);
            }
            if (isFileExistingOnServer(ftp, file)) {
                // Process the file. If processing fails, an exception should be thrown.
                processFile(ftp, file);
                ftp = null;
            } else {
                //avoid processing files that have been deleted on the server
                logger.debug("Skipping " + file + ": the file no longer exists on the server");
            }
        } catch (Exception e) {
            logger.error("Failed to process file: " + file + ". Reason: " + e, e);
        } finally {
            if (ftp != null) {
                returnClient(ftp);
            }
        }
    }

    /**
     * checks if file specified exists on server
     * 
     * @param ftp       the ftp client
     * @param file      the full file path
     * @return          true if found on server
     */
    private boolean isFileExistingOnServer(FTPClient ftp, String file) throws IOException {
        boolean foundFile = false;
        int lastIndex = file.lastIndexOf("/");
        String directory = ".";
        String rawName = file;
        if (lastIndex > 0) { 
            directory = file.substring(0, lastIndex);
            rawName = file.substring(lastIndex+1);
        }

        FTPFile[] files = listFiles(ftp, directory);
        if (files.length > 0) {
            for (FTPFile f : files) {
                if (f.getName().equals(rawName)) {
                    foundFile = true;
                    break;
                }
            }
        }

        return foundFile;
    }
    
    protected void processFile(FTPClient ftp, String file) throws Exception {
        InputStream in = ftp.retrieveFileStream(file);
        InOnly exchange = getExchangeFactory().createInOnlyExchange();
        configureExchangeTarget(exchange);
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);

        if (getTargetOperation() != null) { 
            exchange.setOperation(getTargetOperation()); 
        }

        marshaler.readMessage(exchange, message, in, file);
        if (stateless) {
            exchange.setProperty(FtpData.class.getName(), new FtpData(file, ftp, in));
        } else {
            this.openExchanges.put(exchange.getExchangeId(), new FtpData(file, ftp, in));
        }
        send(exchange);
    }



    public String getLocationURI() {
        return uri.toString();
    }

    public void process(MessageExchange exchange) throws Exception {
        FtpData data;
        if (stateless) {
            data = (FtpData) exchange.getProperty(FtpData.class.getName());
        } else {
            data = this.openExchanges.remove(exchange.getExchangeId());
        }
        // check for done or error
        if (data != null) {
            logger.debug("Releasing " + data.file);
            try {
                // Close ftp related stuff
                data.in.close();
                data.ftp.completePendingCommand();
                // check for state
                if (exchange.getStatus() == ExchangeStatus.DONE) {
                    if (isDeleteFile()) {
                        if (getArchivePath() != null) {
                            // build a unique archive file name
                            String newPath = String.format("%s/%d_%s", getArchivePath(), System.currentTimeMillis(), data.file.substring(data.file.lastIndexOf('/')+1));
                            data.ftp.rename(data.file, newPath);
                        } else {
                            if (!data.ftp.deleteFile(data.file)) {
                                throw new IOException("Could not delete file " + data.file);
                            }
                        }
                    }
                } else {
                    Exception e = exchange.getError();
                    if (e == null) {
                        e = new JBIException("Unknown error");
                    }
                    throw e;
                }
            } finally {
                // unlock the file
                unlockAsyncFile(data.file);
                // release ftp client
                returnClient(data.ftp);
            }
        } else {
            // strange, we don't know this exchange
            logger.debug("Received unknown exchange. Will be ignored...");
        }
    }

    /**
     * unlock the file
     *
     * @param file      the file to unlock
     */
    private void unlockAsyncFile(String file) {
        // finally remove the file from the open exchanges list
        Lock lock = lockManager.getLock(file);
        if (lock != null) {
            try {
                lock.unlock();
            } catch (Exception ex) {
                // can't release the lock
                logger.error("Can't release the lock", ex);
            } 
            lockManager.removeLock(file);
        }
    }
    
    protected FTPClientPool createClientPool() throws Exception {
        FTPClientPool pool = new FTPClientPool();
        pool.afterPropertiesSet();
        return pool;
    }

    protected FTPClient borrowClient() throws JBIException {
        try {
            return (FTPClient) getClientPool().borrowClient();
        } catch (Exception e) {
            throw new JBIException(e);
        }
    }

    protected void returnClient(FTPClient client) {
        if (client != null) {
            try {
                getClientPool().returnClient(client);
            } catch (Exception e) {
                logger.error("Failed to return client to pool: " + e, e);
            }
        }
    }

}
