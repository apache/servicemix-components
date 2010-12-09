/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.vfs.comparator;

import java.util.Comparator;

import org.apache.commons.vfs.FileObject;

/**
 * <p>
 * Simple file object comparator working on file size.
 * </p>
 * 
 * @author jbonofre
 */
public class SizeComparator implements Comparator<FileObject> {

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(FileObject file1, FileObject file2) {
        try {
            return ((int)file1.getContent().getSize()-(int)file2.getContent().getSize());
        } catch (Exception e) {
            return 0;
        }
    }

}
