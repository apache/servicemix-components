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
package org.apache.servicemix.jsr181.xfire;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.aegis.type.DefaultTypeMappingRegistry;
import org.codehaus.xfire.aegis.type.TypeMappingRegistry;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.annotations.WebAnnotations;
import org.codehaus.xfire.annotations.commons.CommonsWebAttributes;
import org.codehaus.xfire.jaxws.JAXWSServiceFactory;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.xmlbeans.XmlBeansTypeRegistry;

public final class ServiceFactoryHelper {
    
    public static final String TM_DEFAULT = "default";
    public static final String TM_XMLBEANS = "xmlbeans";
    public static final String TM_JAXB2 = "jaxb2";
    
    public static final String AN_JSR181 = "jsr181";
    public static final String AN_JAVA5 = "java5";
    public static final String AN_COMMONS = "commons";
    public static final String AN_NONE = "none";

    private static final Map<String, TypeMappingRegistry> KNOWN_TYPE_MAPPINGS;
    private static final Map<String, WebAnnotations> KNOWN_ANNOTATIONS;
    
    private ServiceFactoryHelper() {
    }
    
    static {
        KNOWN_TYPE_MAPPINGS = new HashMap<String, TypeMappingRegistry>();
        KNOWN_TYPE_MAPPINGS.put(TM_DEFAULT, new DefaultTypeMappingRegistry(true));
        KNOWN_TYPE_MAPPINGS.put(TM_XMLBEANS, new XmlBeansTypeRegistry());
        try {
            Class cl = Class.forName("org.codehaus.xfire.jaxb2.JaxbTypeRegistry");
            TypeMappingRegistry tr = (TypeMappingRegistry) cl.newInstance();
            KNOWN_TYPE_MAPPINGS.put(TM_JAXB2, tr);
        } catch (Throwable e) {
            // we are in jdk 1.4, do nothing
        }
        
        KNOWN_ANNOTATIONS = new HashMap<String, WebAnnotations>();
        KNOWN_ANNOTATIONS.put(AN_COMMONS, new CommonsWebAttributes());
        try {
            Class cl = Class.forName("org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations");
            WebAnnotations wa = (WebAnnotations) cl.newInstance();
            KNOWN_ANNOTATIONS.put(AN_JAVA5, wa);
        } catch (Throwable e) {
            // we are in jdk 1.4, do nothing
        }
    }
    
    @SuppressWarnings("unchecked")
    public static ObjectServiceFactory findServiceFactory(
                        XFire xfire,
                        Class serviceClass,
                        String annotations, 
                        String typeMapping) throws Exception {
        // jsr181 is synonymous to java5
        if (annotations != null && AN_JSR181.equals(annotations)) {
            annotations = AN_JAVA5;
        }
        // Determine annotations
        WebAnnotations wa = null;
        String selectedAnnotations = null;
        if (annotations != null) {
            selectedAnnotations = annotations;
            if (!annotations.equals(AN_NONE)) {
                wa = (WebAnnotations) KNOWN_ANNOTATIONS.get(annotations);
                if (wa == null) {
                    throw new Exception("Unrecognized annotations: " + annotations);
                }
            }
        } else {
            for (Iterator it = KNOWN_ANNOTATIONS.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                WebAnnotations w = (WebAnnotations) entry.getValue();
                if (w.hasWebServiceAnnotation(serviceClass)) {
                    selectedAnnotations = (String) entry.getKey();
                    wa = w;
                    break;
                }
            }
        }
        // Determine TypeMappingRegistry
        TypeMappingRegistry tm = null;
        String selectedTypeMapping = null;
        if (typeMapping == null) {
            selectedTypeMapping = (wa == null) ? TM_DEFAULT : TM_JAXB2;
        } else {
            selectedTypeMapping = typeMapping;
        }
        tm = (TypeMappingRegistry) KNOWN_TYPE_MAPPINGS.get(selectedTypeMapping);
        if (tm == null) {
            throw new Exception("Unrecognized typeMapping: " + typeMapping);
        }
        // Create factory
        ObjectServiceFactory factory = null;
        if (wa == null) {
            factory = new ObjectServiceFactory(xfire.getTransportManager(),
                                               new AegisBindingProvider(tm));
        } else if (selectedAnnotations.equals(AN_JAVA5) 
                   && selectedTypeMapping.equals(TM_JAXB2)) {
            try {
                factory = new JAXWSServiceFactory(xfire.getTransportManager());
            } catch (Exception e) {
                factory = new AnnotationServiceFactory(wa, 
                        xfire.getTransportManager(), 
                        new AegisBindingProvider(tm));
            }
        } else {
            factory = new AnnotationServiceFactory(wa, 
                                                   xfire.getTransportManager(), 
                                                   new AegisBindingProvider(tm));
        }
        // Register only JBI transport in the factory
        factory.getSoap11Transports().clear();
        factory.getSoap12Transports().clear();
        factory.getSoap11Transports().add(JbiTransport.JBI_BINDING);
        return factory;
    }

}
