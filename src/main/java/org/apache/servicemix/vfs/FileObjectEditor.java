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

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;

/**
 * A bean editor to make it easier to create new file system objects using VFS
 */
public class FileObjectEditor {
    private String path;
    private FileSystemManager fileSystemManager;

    /**
     * returns the path to the file object to resolve
     * 
     * @return  the path as string object
     */
    public String getPath() {
        return this.path;
    }

    /**
     * sets the path to the file object to be polled
     * 
     * @param path      the path as string
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * returns the file system manager 
     * 
     * @return  the file system manager
     */
    public FileSystemManager getFileSystemManager() {
        return this.fileSystemManager;
    }

    /**
     * sets the file system manager object
     * 
     * @param fileSystemManager the file system manager
     */
    public void setFileSystemManager(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    /**
     * returns the file object to use 
     * 
     * @return  the file object
     * @throws IOException      on resolving errors
     */
    public FileObject getFileObject() throws IOException {
        FileObject answer = null;
        
        try {
            if (fileSystemManager == null) {
                fileSystemManager = VFS.getManager();
            }
            if (path == null) {
                throw new IllegalArgumentException("You must specify a path property");
            }
            
            answer = fileSystemManager.resolveFile(path);
            if (answer == null) {
                throw new IOException("Could not resolve file: " + path);
            }
            
            try {
                answer.createFolder();
            }
            catch (FileSystemException e) {
                throw new IOException("Failed to create folder: " + e);
            }
        }
        catch (FileSystemException e) {
            throw new IOException("Failed to initialize file system manager: " + e);
        }
        
        return answer;
    }
}
