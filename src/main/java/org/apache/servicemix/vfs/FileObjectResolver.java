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
package org.apache.servicemix.vfs;

import java.io.IOException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;

/**
 * class for resolving a path to a FileObject
 * 
 * @author lhein
 */
public class FileObjectResolver {

    /**
     * returns the file object to use 
     * 
     * @param manager   the file system manager
     * @param path      the path 
     * @return  the file object
     * @throws IllegalArgumentException on wrong parameters
     * @throws IOException      on resolving errors
     */
    public static FileObject resolveToFileObject(FileSystemManager manager, String path) throws IllegalArgumentException, IOException {
        FileObject answer = null;
        
        try {
            if (manager == null) {
                manager = VFS.getManager();
            }
            if (path == null) {
                throw new IllegalArgumentException("You must specify a path property");
            }
            
            answer = manager.resolveFile(path);
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
