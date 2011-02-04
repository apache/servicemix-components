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
package org.apache.servicemix.common.xbean;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;

import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.util.DOMUtil;
import org.apache.xbean.classloader.JarFileClassLoader;
import org.apache.xbean.spring.context.SpringApplicationContext;
import org.apache.xbean.spring.context.SpringXmlPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * <p>
 * An advanced xml preprocessor that will create a default class loader for the SU if none
 * is configured.
 * </p>
 * 
 * @author gnodet
 * @author jbonofre
 */
public class ClassLoaderXmlPreprocessor implements SpringXmlPreprocessor {

    private final Logger logger = LoggerFactory.getLogger(ClassLoaderXmlPreprocessor.class);

    public static final String CLASSPATH_XML = "classpath.xml";
    public static final String LIB_DIR = "/lib";
    
    private final File root;
    private final ServiceMixComponent component;
    
    public ClassLoaderXmlPreprocessor(File root) {
        this(root, null);
    }

    public ClassLoaderXmlPreprocessor(File root, ServiceMixComponent component) {
        this.root = root;
        this.component = component;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.xbean.spring.context.SpringXmlPreprocessor#preprocess(org.apache.xbean.spring.context.SpringApplicationContext, org.springframework.beans.factory.xml.XmlBeanDefinitionReader, org.w3c.dom.Document)
     */
    public void preprocess(SpringApplicationContext applicationContext, XmlBeanDefinitionReader reader, Document document) {
        // determine the classLoader
        ClassLoader classLoader;
        logger.debug("Get the classpath elements from xbean descriptor.");
        NodeList classpathElements = document.getDocumentElement().getElementsByTagName("classpath");
        if (classpathElements.getLength() == 0) {
            // Check if a classpath.xml file exists in the root of the SU
            logger.debug("Check if a classpath.xml file exists in the SU root.");
            List<URL> classpathUrls = getResources(CLASSPATH_XML);
            if (classpathUrls.size() == 1) {
                DocumentBuilder builder = null;
                try {
                    builder = DOMUtil.getBuilder();
                    Document doc = builder.parse(classpathUrls.get(0).toString());
                    classLoader = getClassLoader(applicationContext, reader, doc);
                } catch (Exception e) {
                    throw new FatalBeanException("Unable to load classpath.xml file", e);
                } finally {
                    DOMUtil.releaseBuilder(builder);
                }
            } else {
                try {
                    URL[] urls = getDefaultLocations();
                    ClassLoader parentLoader = getParentClassLoader(applicationContext);
                    classLoader = new JarFileClassLoader(applicationContext.getDisplayName(), urls, parentLoader);
                    // assign the class loader to the xml reader and the
                    // application context
                } catch (Exception e) {
                    throw new FatalBeanException("Unable to create default classloader for SU", e);
                }
            }
        } else {
            classLoader = getClassLoader(applicationContext, reader, document);
        }
        reader.setBeanClassLoader(classLoader);
        applicationContext.setClassLoader(classLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * <p>
     * Replaces a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.
     * </p>
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     * 
     * @param text text to search and replace in, may be null.
     * @param searchString the String to search for, may be null.
     * @param replacement the String to replace it with, may be null.
     * @param max maximum number of value to replace, or <code>-1</code> if no maximum.
     * @return the text with any replacements processed, <code>null</code> if null String input.
     */
    private static String replaceString(String text, String searchString, String replacement, int max) {
        if (text == null || text.length() == 0
                || searchString == null || searchString.length() == 0
                || replacement == null
                || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == -1) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : (max > 64 ? 64 : max);
        StringBuffer buffer = new StringBuffer(text.length() + increase);
        while (end != -1) {
            buffer.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buffer.append(text.substring(start));
        return buffer.toString();
    }
    
    /**
     * <p>
     * Get the URLs for a classpath location. This method supports standard
     * relative file location, <code>file:</code> URL location (including entries
     * regexp filter support), <code>jar:</code> URL location (including entries
     * regexp filter support).
     * </p>
     * 
     * @param location the location where to get the URLs.
     * @return the URLs list.
     */
    protected List<URL> getResources(String location) {
        // step 1: replace system properties in the location
        Properties systemProperties = System.getProperties();
        for (Iterator systemPropertiesIterator = systemProperties.keySet().iterator(); systemPropertiesIterator.hasNext(); ) {
            String property = (String) systemPropertiesIterator.next();
            String value = systemProperties.getProperty(property);
            location = ClassLoaderXmlPreprocessor.replaceString(location, "${" + property + "}", value, -1);
        }
        // step 2: apply regexp search for file: or jar: protocol based URL
        if (location.startsWith("jar:")) {
            return this.getJarResources(location);
        } else if (location.startsWith("file:")) {
            return this.getFileResources(location);
        } else {
            // relative location
            List<URL> urls = new LinkedList<URL>();
            URI uri = root.toURI().resolve(location);
            File file = new File(uri);
            if (file.canRead()) {
                try {
                    urls.add(file.toURL());
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Malformed resource location " + uri + ".", e);
                }
            }
            return urls;
        }
    }
    
    /**
     * <p>
     * Get the URLs for a jar: protocol based location. This method supports
     * regexp to add several entries.
     * </p>
     * 
     * <pre>
     * jar:file:/path/to/my.ear!/entry.jar
     * jar:file:/path/to/my.ear!/en*.jar
     * </pre>
     * 
     * @param location the jar location.
     * @return the jar location URLs.
     */
    protected List<URL> getJarResources(String location) {
        logger.debug("Get jar resources from " + location);
        List<URL> urls = new LinkedList<URL>();
        // get the !/ separator index
        int separatorIndex = location.indexOf("!/");
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("The jar URL " + location + " is not valid. !/ separator not found.");
        }
        // extract the jar location
        String jarLocation = location.substring(4, separatorIndex);
        // extract the entry location
        String entryLocation = location.substring(separatorIndex + 2);
        if (jarLocation == null || jarLocation.length() == 0
                || entryLocation == null || entryLocation.length() == 0) {
            throw new IllegalArgumentException("The jar URL " + location + " is not valid. Jar URL or entry not found.");
        }
        // contruct the jar URL
        JarInputStream jarInputStream;
        try {
            jarInputStream = new JarInputStream(new URL(jarLocation).openStream());
        } catch (Exception e) {
            throw new IllegalArgumentException("The jar URL " + jarLocation + " is not valid.", e);
        }
        // iterate into the entries
        try {
            ZipEntry entry = jarInputStream.getNextEntry();
            while (entry != null) {
                if (entry.getName().matches(entryLocation)) {
                    // the entry matches the regexp, construct the entry URL
                    String entryUrl = "jar:" + jarLocation + "!/" + entry.getName();
                    // add the entry URL into the URLs list
                    urls.add(new URL(entryUrl));
                }
                entry = jarInputStream.getNextEntry();
            }
        } catch (IOException ioException) {
            throw new IllegalArgumentException("Can't read jar entries.", ioException);
        }
        return urls;
    }
    
    /**
     * <p>
     * Get the URLs for a file: protocol based location. This methods supports
     * regexp to add several entries.
     * </p>
     * 
     * <pre>
     * file:/path/to/my.jar
     * file:/path/to/my*.jar
     * </pre>
     * 
     * @param location
     * @return
     */
    protected List<URL> getFileResources(String location) {
        logger.debug("Get file resources from " + location);
        List<URL> urls = new LinkedList<URL>();
        int starIndex = location.indexOf("*");
        if (starIndex == -1) {
            // no regexp pattern found
            try {
                urls.add(new URL(location));
            } catch (MalformedURLException malformedURLException) {
                throw new IllegalArgumentException("Invalid URL " + location + ".", malformedURLException);
            }
        } else {
            // user has defined a regexp file name filter
            int lastSeparatorIndex = location.lastIndexOf("/");
            if (starIndex < lastSeparatorIndex) {
                throw new IllegalArgumentException("Regexp is supported only on file name, not on directory.");
            }
            String dirPath = location.substring(0, lastSeparatorIndex);
            File dir = new File(dirPath);
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("The regexp basedir " + dirPath + " is not a directory.");
            }
            File[] entries = dir.listFiles();
            String fileNameRegexp = location.substring(lastSeparatorIndex);
            for (int i = 0; i < entries.length; i++) {
                File entry = entries[i];
                if (entry.getName().matches(fileNameRegexp)) {
                    try {
                        urls.add(new URL(dirPath + "/" + entry.getName()));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("The URL " + dirPath + "/" + entry.getName() + " is invalid.", e);
                    }
                }
            }
        }
        return urls;
    }
     
    protected URL[] getDefaultLocations() {
        try {
            File[] jars = new File(root, LIB_DIR).listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase();
                    return name.endsWith(".jar") || name.endsWith(".zip");
                }
            });
            URL[] urls = new URL[jars != null ? jars.length + 1 : 1];
            urls[0] = root.toURL();
            if (jars != null) {
                for (int i = 0; i < jars.length; i++) {
                    urls[i+1] = jars[i].toURL();
                }
            }
            return urls;
        } catch (MalformedURLException e) {
            throw new FatalBeanException("Unable to get default classpath locations", e);
        }
    }
    
    protected ClassLoader getClassLoader(SpringApplicationContext applicationContext, XmlBeanDefinitionReader reader, Document document) {
        // determine the classLoader
        ClassLoader classLoader;
        NodeList classpathElements = document.getDocumentElement().getElementsByTagName("classpath");
        if (classpathElements.getLength() < 1) {
            classLoader = getParentClassLoader(applicationContext);
        } else if (classpathElements.getLength() > 1) {
            throw new FatalBeanException("Expected only classpath element but found " + classpathElements.getLength());
        } else {
            Element classpathElement = (Element) classpathElements.item(0);
            
            // Delegation mode
            boolean inverse = false;
            String inverseAttr = classpathElement.getAttribute("inverse");
            if (inverseAttr != null && "true".equalsIgnoreCase(inverseAttr)) {
                inverse = true;
            }

            // build hidden classes
            List<String> hidden = new ArrayList<String>();
            NodeList hiddenElems = classpathElement.getElementsByTagName("hidden");
            for (int i = 0; i < hiddenElems.getLength(); i++) {
                Element hiddenElement = (Element) hiddenElems.item(i);
                String pattern = ((Text) hiddenElement.getFirstChild()).getData().trim();
                hidden.add(pattern);
            }

            // build non overridable classes
            List<String> nonOverridable = new ArrayList<String>();
            NodeList nonOverridableElems = classpathElement.getElementsByTagName("nonOverridable");
            for (int i = 0; i < nonOverridableElems.getLength(); i++) {
                Element nonOverridableElement = (Element) nonOverridableElems.item(i);
                String pattern = ((Text) nonOverridableElement.getFirstChild()).getData().trim();
                nonOverridable.add(pattern);
            }

            // build the classpath
            List<String> classpath = new ArrayList<String>();
            NodeList locations = classpathElement.getElementsByTagName("location");
            for (int i = 0; i < locations.getLength(); i++) {
                Element locationElement = (Element) locations.item(i);
                String location = ((Text) locationElement.getFirstChild()).getData().trim();
                classpath.add(location);
            }
            
            // Add shared libraries
            List<String> sls = new ArrayList<String>();
            NodeList libraries = classpathElement.getElementsByTagName("library");
            for (int i = 0; i < libraries.getLength(); i++) {
                Element locationElement = (Element) libraries.item(i);
                String library = ((Text) locationElement.getFirstChild()).getData().trim();
                sls.add(library);
            }

            // Add components
            List<String> components = new ArrayList<String>();
            NodeList componentList = classpathElement.getElementsByTagName("component");
            for (int i = 0; i < componentList.getLength(); i++) {
                Element locationElement = (Element) componentList.item(i);
                String component = ((Text) locationElement.getFirstChild()).getData().trim();
                components.add(component);
            }

            // convert the paths to URLS
            URL[] urls;
            if (classpath.size() != 0) {
                List<URL> urlsList = new LinkedList<URL>();
                for (ListIterator<String> iterator = classpath.listIterator(); iterator.hasNext();) {
                    String location = iterator.next();
                    List<URL> locationUrls = getResources(location);
                    for (URL url : locationUrls) {
                        urlsList.add(url);
                    }
                }
                urls = urlsList.toArray(new URL[urlsList.size()]);
            } else {
                urls = getDefaultLocations();
            }

            // create the classloader
            List<ClassLoader> parents = new ArrayList<ClassLoader>();
            parents.add(getParentClassLoader(applicationContext));
            for (String library : sls) {
                ClassLoader cl = this.component.getContainer().getSharedLibraryClassLoader(library);
                if (cl == null) {
                    throw new IllegalStateException("No such shared library: " + library);
                }
                parents.add(cl);
            }
            for (String component : components) {
                ClassLoader cl = this.component.getContainer().getComponentClassLoader(component);
                if (cl == null) {
                    throw new IllegalStateException("No such component: " + component);
                }
                parents.add(cl);
            }
            classLoader = new JarFileClassLoader(applicationContext.getDisplayName(),
                                                 urls, 
                                                 parents.toArray(new ClassLoader[parents.size()]),
                                                 inverse,
                                                 hidden.toArray(new String[hidden.size()]),
                                                 nonOverridable.toArray(new String[nonOverridable.size()]));

            // remove the classpath element so Spring doesn't get confused
            document.getDocumentElement().removeChild(classpathElement);
        }
        return classLoader;
    }

    protected ClassLoader getParentClassLoader(SpringApplicationContext applicationContext) {
        ClassLoader classLoader = applicationContext.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }
    
}
