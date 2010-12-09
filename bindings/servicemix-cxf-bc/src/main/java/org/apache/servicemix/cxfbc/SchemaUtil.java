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
package org.apache.servicemix.cxfbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.CatalogXmlSchemaURIResolver;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;

import static org.apache.cxf.helpers.CastUtils.cast;

public final class SchemaUtil {
    private final Map<String, Element> schemaList;
    private final Map<String, String> catalogResolved = new HashMap<String, String>();
    private final Bus bus;

    public SchemaUtil(final Bus b, final Map<String, Element> s) {
        this.bus = b;
        this.schemaList = s;
    }
    public void getSchemas(final Definition def, final ServiceInfo serviceInfo) {
        SchemaCollection schemaCol = serviceInfo.getXmlSchemaCollection();
        getSchemas(def, schemaCol, serviceInfo);
    }
    public void getSchemas(final Definition def, 
                           SchemaCollection schemaCol, 
                           ServiceInfo serviceInfo) {
        getSchemas(def, schemaCol, serviceInfo.getSchemas());
    }

    public void getSchemas(final Definition def, 
                           final SchemaCollection schemaCol,
                           List<SchemaInfo> schemas) {
        List<Definition> defList = new ArrayList<Definition>();
        parseImports(def, defList);
        extractSchema(def, schemaCol, schemas);
        // added
        getSchemaList(def);
        
        Map<Definition, Definition> done = new IdentityHashMap<Definition, Definition>();
        done.put(def, def);
        for (Definition def2 : defList) {
            if (!done.containsKey(def2)) {
                extractSchema(def2, schemaCol, schemas);
                // added
                getSchemaList(def2);
                done.put(def2, def2);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void extractSchema(Definition def, SchemaCollection schemaCol, List<SchemaInfo> schemaInfos) {
        Types typesElement = def.getTypes();
        if (typesElement != null) {
            int schemaCount = 1;
            for (Object obj : typesElement.getExtensibilityElements()) {
                org.w3c.dom.Element schemaElem = null;
                if (obj instanceof Schema) {
                    Schema schema = (Schema)obj;
                    schemaElem = schema.getElement();
                } else if (obj instanceof UnknownExtensibilityElement) {
                    org.w3c.dom.Element elem = ((UnknownExtensibilityElement)obj).getElement();
                    if (elem.getLocalName().equals("schema")) {
                        schemaElem = elem;
                    }
                }
                if (schemaElem != null) {
                    synchronized (schemaElem.getOwnerDocument()) {
                        //for (Object prefix : def.getNamespaces().keySet()) {
                        Map<String, String> nameSpaces = CastUtils.cast(def.getNamespaces());
                        for (Entry<String, String> ent : nameSpaces.entrySet()) {
                            String prefix = ent.getKey();
                            String ns = nameSpaces.get(prefix);
                            if ("".equals(prefix)) {
                                if (!schemaElem.hasAttribute("xmlns")) {
                                    Attr attr = 
                                        schemaElem.getOwnerDocument()
                                            .createAttributeNS(javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI, 
                                                               "xmlns");
                                    attr.setValue(ns);
                                    schemaElem.setAttributeNodeNS(attr);
                                }
                            } else if (!schemaElem.hasAttribute("xmlns:" + prefix)) {
                                String namespace = javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                                Attr attr = 
                                    schemaElem.getOwnerDocument().createAttributeNS(namespace, 
                                                                                    "xmlns:" + prefix);
                                attr.setValue(ns);
                                schemaElem.setAttributeNodeNS(attr);
                            }
                        }
                        String systemId = def.getDocumentBaseURI() + "#types" + schemaCount;
    
                        schemaCol.setBaseUri(def.getDocumentBaseURI());
                        CatalogXmlSchemaURIResolver schemaResolver =
                            new CatalogXmlSchemaURIResolver(bus);
                        schemaCol.setSchemaResolver(schemaResolver);
                        
                        XmlSchema xmlSchema = schemaCol.read(schemaElem, systemId);
                        catalogResolved.putAll(schemaResolver.getResolvedMap());
                        SchemaInfo schemaInfo = new SchemaInfo(xmlSchema.getTargetNamespace());
                        schemaInfo.setSchema(xmlSchema);
                        schemaInfo.setSystemId(systemId);
                        schemaInfo.setElement(schemaElem);
                        schemaInfos.add(schemaInfo);
                        schemaCount++;
                    }
                }
            }
        }
    }

    private void parseImports(Definition def, List<Definition> defList) {
        List<Import> importList = new ArrayList<Import>();

        Collection<List<Import>> ilist = cast(def.getImports().values());
        for (List<Import> list : ilist) {
            importList.addAll(list);
        }
        for (Import impt : importList) {
            if (!defList.contains(impt.getDefinition())) {
                defList.add(impt.getDefinition());
                parseImports(impt.getDefinition(), defList);
            }
        }
    }

    // Workaround for getting the elements
    private void getSchemaList(Definition def) {
        Types typesElement = def.getTypes();
        if (typesElement != null) {
            Iterator ite = typesElement.getExtensibilityElements().iterator();
            while (ite.hasNext()) {
                Object obj = ite.next();
                if (obj instanceof Schema) {
                    Schema schema = (Schema)obj;
                    addSchema(schema.getDocumentBaseURI(), schema);
                }
            }
        }
    }

    private void addSchema(String docBaseURI, Schema schema) {
        //String docBaseURI = schema.getDocumentBaseURI();
        Element schemaEle = schema.getElement();
        if (schemaList.get(docBaseURI) == null) {
            schemaList.put(docBaseURI, schemaEle);
        } else if (schemaList.get(docBaseURI) != null && schemaList.containsValue(schemaEle)) {
            // do nothing
        } else {
            String tns = schema.getDocumentBaseURI() + "#"
                         + schema.getElement().getAttribute("targetNamespace");
            if (schemaList.get(tns) == null) {
                schemaList.put(tns, schema.getElement());
            }
        }

        //handle imports
        Map<String, List> imports = CastUtils.cast(schema.getImports());
        if (imports != null && imports.size() > 0) {
            for (Entry<String, List> ent : imports.entrySet()) {
                String importNamespace = ent.getKey();
                List<SchemaImport> schemaImports = CastUtils.cast(imports.get(importNamespace));
                
                for (SchemaImport schemaImport : schemaImports) {
                    Schema tempImport = schemaImport.getReferencedSchema();                   
                    String key = schemaImport.getSchemaLocationURI();
                    if (importNamespace == null && tempImport != null) {
                        importNamespace = tempImport.getDocumentBaseURI();
                    }
                    
                    if (tempImport != null && !catalogResolved.containsKey(key)) {                 
                        key = tempImport.getDocumentBaseURI();
                    }
                    
                    if (tempImport != null
                        && !isSchemaParsed(key, importNamespace)
                        && !schemaList.containsValue(tempImport.getElement())) {
                        addSchema(key, tempImport);
                    }
                    if (tempImport != null) {
                      //keep this imported schema inline
                      inlineTransformer(key, tempImport.getElement(), schema.getElement(), false);
                    }
                }

            }
        }
        //handle includes
        List<SchemaReference> includes = CastUtils.cast(schema.getIncludes());
        if (includes != null && includes.size() > 0) {
            String includeNamespace = schema.getElement().getAttribute("targetNamespace");

            for (SchemaReference schemaInclude : includes) {
                Schema tempInclude = schemaInclude.getReferencedSchema();
                String key = tempInclude.getDocumentBaseURI();
                if (includeNamespace == null && tempInclude != null) {
                    includeNamespace = tempInclude.getDocumentBaseURI();
                }

                if (tempInclude != null && !catalogResolved.containsKey(key)) {
                    key = tempInclude.getDocumentBaseURI();
                }

                if (tempInclude != null && !isSchemaParsed(key, includeNamespace)
                        && !schemaList.containsValue(tempInclude.getElement())) {
                    addSchema(key, tempInclude);
                }
                if (tempInclude != null) {
                  //keep this included schema inline
                  inlineTransformer(key, tempInclude.getElement(), schema.getElement(), true);
                }
            }

        }
    }

    private boolean isSchemaParsed(String baseUri, String ns) {
        if (schemaList.get(baseUri) != null) {
            Element ele = schemaList.get(baseUri);
            String tns = ele.getAttribute("targetNamespace");
            if (ns.equals(tns)) {
                return true;
            }
        }
        return false;
    }
    
    private void inlineTransformer(String key, Element inlineSchema, Element outerSchema, boolean isInclude) {
        NodeList nl = null;
        if (isInclude) {
            nl = outerSchema.getElementsByTagNameNS(
                    "http://www.w3.org/2001/XMLSchema", "include");
        } else {
            nl = outerSchema.getElementsByTagNameNS(
                    "http://www.w3.org/2001/XMLSchema", "import");
        }
        for (int j = 0; j < nl.getLength(); j++) {
        
            String schemaLocation = ((Element)nl.item(j)).getAttribute("schemaLocation");
            
            if (schemaLocation != null && getXsdFileName(schemaLocation, "/").
                    equals(getXsdFileName(key, "/"))) {
                                            
                outerSchema.removeChild(nl.item(j));
                for (int i = 0; i < inlineSchema.getChildNodes().getLength(); i++) {
                    outerSchema.appendChild(
                            outerSchema.getOwnerDocument().importNode(
                                inlineSchema.getChildNodes().item(i), true));
                }
                outerSchema.setPrefix("xs");
                outerSchema.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
            }
        }
    }
    
    private String getXsdFileName(String path, String sep) {
        String name = "";
        if (path.lastIndexOf(sep) >= 0) {
            name = path.substring(path.lastIndexOf(sep) + 1);
        } else {
            name = path;
        }
        return name;
    }
}
