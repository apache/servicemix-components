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

import javax.jbi.messaging.MessagingException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A simple implementation of {@link ErrorHandler} which just counts the number of warnings, errors and fatal errors.
 *
 * @version $Revision: 430194 $
 */
public class CountingErrorHandler implements MessageAwareErrorHandler {
    private int warningCount;
    private int errorCount;
    private int fatalErrorCount;

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
    }

    /*  (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e) throws SAXException {
        ++errorCount;
    }

    /*  (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
        ++fatalErrorCount;
    }
    
    /*  (non-Javadoc)
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#capturesMessages()
     */
    public boolean capturesMessages() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#getMessagesAs(java.lang.Class)
     */
    public Object getMessagesAs(Class format) throws MessagingException {
        throw new MessagingException("Unsupported message format: " + format.getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.components.validation.MessageAwareErrorHandler#supportsMessageFormat(java.lang.Class)
     */
    public boolean supportsMessageFormat(Class format) {
        return false;
    }
    
}
