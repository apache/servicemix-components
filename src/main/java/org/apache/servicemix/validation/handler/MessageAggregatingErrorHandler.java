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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jbi.messaging.MessagingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.validation.ValidationEndpoint;

/**
 * An implementation of {@link ErrorHandler} which aggregates all warnings and
 * error messages into a StringBuffer.
 *
 * @version $Revision: 359186 $
 */
public class MessageAggregatingErrorHandler implements MessageAwareErrorHandler {
    
    private static final String OPEN_CDATA = "<![CDATA[";
    private static final String CLOSE_CDATA = "]]>";
    private static final String OPEN_ERROR = ValidationEndpoint.TAG_ERROR_START;
    private static final String CLOSE_ERROR = ValidationEndpoint.TAG_ERROR_END;
    private static final String OPEN_FATAL = ValidationEndpoint.TAG_FATAL_START;
    private static final String CLOSE_FATAL = ValidationEndpoint.TAG_FATAL_END;
    private static final String OPEN_WARNING = ValidationEndpoint.TAG_WARNING_START;
    private static final String CLOSE_WARNING = ValidationEndpoint.TAG_WARNING_END;
    
    private String openRootElement;
    private String closeRootElement;

    /**
     * Number of warnings.
     */
    private int warningCount;
    
    /**
     * Number of errors.
     */
    private int errorCount;
    
    /**
     * Number of fatal errors.
     */
    private int fatalErrorCount;
    
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
     * Variable to hold the warning/error messages from the validator
     */
    private StringBuffer messages = new StringBuffer();
    
    private SourceTransformer sourceTransformer = new SourceTransformer();
    
    /**
     * Constructor.
     * 
     * @param rootElement
     *      The root element name of the fault xml message 
     * @param namespace
     *      The namespace for the fault xml message
     * @param includeStackTraces
     *      Include stracktraces in the final output
     */
    public MessageAggregatingErrorHandler(String rootPath, String namespace, boolean includeStackTraces) throws IllegalArgumentException {
        if (rootPath == null || rootPath.trim().length() == 0) {
            throw new IllegalArgumentException("rootPath must not be null or an empty string");
        }
        this.rootPath = rootPath;
        this.namespace = namespace;
        this.includeStackTraces = includeStackTraces;
        createRootElementTags();
    }

    /**
     * Creates the root element tags for later use down to n depth.
     * Note: the rootPath here is of the form:
     * 
     *      <code>rootElementName/elementName-1/../elementName-n</code>
     * 
     * The namespace will be appended to the root element if it is not
     * null or empty.
     */
    private void createRootElementTags() {
        /* 
         * since the rootPath is constrained to be not null or empty
         * then we have at least one path element.
         */
        String[] pathElements = rootPath.split("/");

        StringBuffer openRootElementSB = new StringBuffer().append("<").append(pathElements[0]);
        StringBuffer closeRootElementSB = new StringBuffer();
        
        if (namespace != null && namespace.trim().length() > 0) {
            openRootElementSB.append(" xmlns=\"").append(namespace).append("\">"); 
        } else {
            openRootElementSB.append(">");
        }
        
        if (pathElements.length > 0) {
            int j = pathElements.length - 1;
            for (int i = 1; i < pathElements.length; i++, j--) {
                openRootElementSB.append("<").append(pathElements[i]).append(">");
                closeRootElementSB.append("</").append(pathElements[j]).append(">");
            }
        }
        
        // create the closing root element tag
        closeRootElementSB.append("</").append(pathElements[0]).append(">");
        
        openRootElement = openRootElementSB.toString();
        closeRootElement = closeRootElementSB.toString();
    }
    
    /*  (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#hasErrors()
     */
    public boolean hasErrors() {
        return getErrorCount() > 0 || getFatalErrorCount() > 0;
    }

    /*  (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#getWarningCount()
     */
    public int getWarningCount() {
        return warningCount;
    }

    /*  (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#getErrorCount()
     */
    public int getErrorCount() {
        return errorCount;
    }

    /*  (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#getFatalErrorCount()
     */
    public int getFatalErrorCount() {
        return fatalErrorCount;
    }

    /*  (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException e) throws SAXException {
        ++warningCount;

        // open warning and CDATA tags
        messages.append(OPEN_WARNING).append(OPEN_CDATA);
        
        // append the fatal error message
        appendErrorMessage(e);
        
        // close CDATA and warning tags
        messages.append(CLOSE_CDATA).append(CLOSE_WARNING);
    }

    /*  (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e) throws SAXException {
        ++errorCount;

        // open fatal error and CDATA tags
        messages.append(OPEN_ERROR).append(OPEN_CDATA);
        
        // append the error message
        appendErrorMessage(e);
        
        // close CDATA and error tags
        messages.append(CLOSE_CDATA).append(CLOSE_ERROR);
    }

    /*  (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
        ++fatalErrorCount;
        
        // open fatal error and CDATA tags
        messages.append(OPEN_FATAL).append(OPEN_CDATA);
        
        // append the fatal error message
        appendErrorMessage(e);
        
        // close CDATA and fatal error tags
        messages.append(CLOSE_CDATA).append(CLOSE_FATAL);
    }

    /**
     * Append the error message or stacktrace to the messages attribute.
     * 
     * @param e
     */
    private void appendErrorMessage(SAXParseException e) {
        if (includeStackTraces) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            messages.append(sw.toString());
        } else {
            messages.append(e.getLocalizedMessage());
        }
    }
    
    /*  (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#capturesMessages()
     */
    public boolean capturesMessages() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#getMessagesAs(java.lang.Class)
     */
    public Object getMessagesAs(Class format) throws MessagingException {
        if (format == DOMSource.class) {
            return getDOMSource();
        } else if (format == StringSource.class) {
            return getStringSource();
        } else if (format == String.class) {
            return getMessagesWithRootElement();
        }
        throw new MessagingException("Unsupported message format: " + format.getName());
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#supportsMessageFormat(java.lang.Class)
     */
    public boolean supportsMessageFormat(Class format) {
        if (format == DOMSource.class) {
            return true;
        } else if (format == StringSource.class) {
            return true;
        } else if (format == String.class) {
            return true;
        }
        return false;
    }
    
    /**
     * Return the messages encapsulated with the root element.
     * 
     * @return
     */
    private String getMessagesWithRootElement() {
        return new StringBuffer().append(openRootElement).append(messages).append(closeRootElement).toString();
    }
    
    /**
     * Get the error messages as a String Source.
     * 
     * @return
     */
    private StringSource getStringSource() {
        return new StringSource(getMessagesWithRootElement());
    }
    
    /**
     * Get the error messages as a DOMSource.
     * 
     * @return
     * @throws MessagingException
     */
    private DOMSource getDOMSource() throws MessagingException {
        try {
            return sourceTransformer.toDOMSource(getStringSource());
        } catch (ParserConfigurationException e) {
            throw new MessagingException("Failed to create DOMSource for Schema validation messages: " + e, e);
        } catch (IOException e) {
            throw new MessagingException("Failed to create DOMSource for Schema validation messages: " + e, e);
        } catch (SAXException e) {
            throw new MessagingException("Failed to create DOMSource for Schema validation messages: " + e, e);
        } catch (TransformerException e) {
            throw new MessagingException("Failed to create DOMSource for Schema validation messages: " + e, e);
        }
    }
    
}
