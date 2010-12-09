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
package org.apache.servicemix.validation.handler;

/**
 * @author gmcdonald
 *
 */
public class MessageAggregatingErrorHandlerFactory implements
        MessageAwareErrorHandlerFactory {

    /**
     * The root element name for the fault xml message
     */
    private String rootPath;
    
    /**
     * The namespace for the fault xml message
     */
    private String namespace;
    
    /**
     * Determines whether or not to include stacktraces in the fault xml message
     */
    private boolean includeStackTraces;
    
    /**
     * @return Returns the includeStackTraces.
     */
    public boolean isIncludeStackTraces() {
        return includeStackTraces;
    }

    /**
     * @param includeStackTraces The includeStackTraces to set.
     */
    public void setIncludeStackTraces(boolean includeStackTraces) {
        this.includeStackTraces = includeStackTraces;
    }

    /**
     * @return Returns the namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace The namespace to set.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return Returns the rootPath.
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * @param rootPath The rootPath to set.
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandlerFactory#createMessageAwareErrorHandler()
     */
    public MessageAwareErrorHandler createMessageAwareErrorHandler() {
        return new MessageAggregatingErrorHandler(rootPath, namespace, includeStackTraces);
    }

}
