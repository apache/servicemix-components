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
package org.apache.servicemix.common.tools.wsdl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

/**
 * <p>
 * Contains informations related to a schema.
 * </p>
 *  
 * @author gnodet
 */
public class Schema {

    private Element root;
    private String namespace;
    private List<String> imports;
    private List<URI> sourceUris;
    
    /**
     * <p>
     * Adds a reference to an imported namespace.
     * </p>
     *
     * @param namespace the namespace to reference
     */
    public void addImport(String namespace) {
        if (imports == null) {
            imports = new ArrayList<String>();
        }
        imports.add(namespace);
    }
    
    /**
     * <p>
     * Gets the imported namespaces list.
     * </p>
     *
     * @return Returns the imports.
     */
    public List<String> getImports() {
        return imports;
    }

    /**
     * <p>
     * Sets the imported namespaces list.
     * </p>
     *
     * @param imports The imports to set.
     */
    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    /**
     * <p>
     * Gets the root element.
     * </p>
     *
     * @return Returns the root.
     */
    public Element getRoot() {
        return root;
    }

    /**
     * <p>
     * Sets the root element.
     * </p>
     *
     * @param root The root to set.
     */
    public void setRoot(Element root) {
        this.root = root;
    }

    /**
     * <p>
     * Gets the default namespace.
     * </p>
     *
     * @return Returns the namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * <p>
     * Sets the default namespace.
     * </p>
     *
     * @param namespace The namespace to set.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * <p>
     * Gets the source URIs list.
     * </p>
     *
     * @return Returns the source URIs list.
     */
    public List<URI> getSourceUris() {
        return sourceUris;
    }

    /**
     * <p>
     * Add a new URI in the source URIs list.
     * </p>
     *
     * @param sourceUri The sourceUri to set.
     */
    public void addSourceUri(URI sourceUri) {
    	if (sourceUris == null) {
    		sourceUris = new ArrayList<URI>();
    	}
        sourceUris.add(sourceUri);
    }

}
