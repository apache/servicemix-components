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
package org.apache.servicemix.validation;

import java.io.IOException;

import javax.jbi.JBIException;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jbi.FaultException;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.apache.servicemix.validation.handler.CountingErrorHandlerFactory;
import org.apache.servicemix.validation.handler.MessageAwareErrorHandler;
import org.apache.servicemix.validation.handler.MessageAwareErrorHandlerFactory;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

/**
 * @org.apache.xbean.XBean element="endpoint"
 * @author lhein
 */
public class ValidationEndpoint extends ProviderEndpoint implements
        ValidationEndpointType {

    public static final String FAULT_FLOW = "FAULT_FLOW";

    public static final String FAULT_JBI = "FAULT_JBI";

    public static final String TAG_RESULT_START = "<result>";

    public static final String TAG_RESULT_END = "</result>";

    public static final String TAG_WARNING_START = "<warning>";

    public static final String TAG_WARNING_END = "</warning>";

    public static final String TAG_ERROR_START = "<error>";

    public static final String TAG_ERROR_END = "</error>";

    public static final String TAG_FATAL_START = "<fatal>";

    public static final String TAG_FATAL_END = "</fatal>";

    private String handlingErrorMethod = "FAULT_JBI";

    private Schema schema;

    private String schemaLanguage = "http://www.w3.org/2001/XMLSchema";

    private Source schemaSource;

    private Resource schemaResource;

    private MessageAwareErrorHandlerFactory errorHandlerFactory = new CountingErrorHandlerFactory();

    private SourceTransformer sourceTransformer = new SourceTransformer();

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#start()
     */
    @Override
    public void start() throws Exception {
        super.start();

        try {
            if (schema == null) {
                SchemaFactory factory = SchemaFactory
                        .newInstance(schemaLanguage);

                if (schemaSource == null) {
                    if (schemaResource == null) {
                        throw new JBIException(
                                "You must specify a schema, schemaSource or schemaResource property");
                    }
                    if (schemaResource.getURL() == null) {
                        schemaSource = new StreamSource(schemaResource
                                .getInputStream());
                    } else {
                        schemaSource = new StreamSource(schemaResource
                                .getInputStream(), schemaResource.getURL()
                                .toExternalForm());
                    }
                }
                schema = factory.newSchema(schemaSource);
            }
        } catch (IOException e) {
            throw new JBIException("Failed to load schema: " + e, e);
        } catch (SAXException e) {
            throw new JBIException("Failed to load schema: " + e, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#stop()
     */
    @Override
    public void stop() throws Exception {
        super.stop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOnly(javax.jbi.messaging.MessageExchange,
     *      javax.jbi.messaging.NormalizedMessage)
     */
    @Override
    protected void processInOnly(MessageExchange exchange, NormalizedMessage in)
            throws Exception {
        NormalizedMessage out = exchange.createMessage();
        Fault fault = exchange.createFault();
        this.startValidation(exchange, in, out, fault);

        if (fault.getContent() != null) {
            throw new RuntimeException(sourceTransformer.contentToString(fault));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOut(javax.jbi.messaging.MessageExchange,
     *      javax.jbi.messaging.NormalizedMessage,
     *      javax.jbi.messaging.NormalizedMessage)
     */
    @Override
    protected void processInOut(MessageExchange exchange, NormalizedMessage in,
            NormalizedMessage out) throws Exception {
        Fault fault = exchange.createFault();
        this.startValidation(exchange, in, out, fault);
        if (fault.getContent() != null) {
            exchange.setFault(fault);
        }
    }

    /**
     * does the validation
     * 
     * @param exchange
     * @param in
     * @param out
     * @param fault
     * @throws Exception
     */
    public void startValidation(MessageExchange exchange, NormalizedMessage in,
            NormalizedMessage out, Fault fault) throws Exception {
        Validator validator = schema.newValidator();

        // create a new errorHandler and set it on the validator
        MessageAwareErrorHandler errorHandler = errorHandlerFactory
                .createMessageAwareErrorHandler();
        validator.setErrorHandler(errorHandler);
        DOMResult result = new DOMResult();

        fault.setContent(null);

        try {
            // Only DOMSource and SAXSource are allowed for validating
            // See
            // http://java.sun.com/j2se/1.5.0/docs/api/javax/xml/validation/Validator.html#validate(javax.xml.transform.Source,%20javax.xml.transform.Result)
            // As we expect a DOMResult as output, we must ensure that the input
            // is a DOMSource
            DOMSource src = sourceTransformer.toDOMSource(in.getContent());

            // call the validation method
            doValidation(validator, src, result);

            // check if there were errors while validating
            if (errorHandler.hasErrors()) {
                /*
                 * check if this error handler supports the capturing of error
                 * messages.
                 */
                if (errorHandler.capturesMessages()) {
                    /*
                     * In descending order of preference select a format to use.
                     * If neither DOMSource, StringSource or String are
                     * supported throw a messaging exception.
                     */
                    if (errorHandler.supportsMessageFormat(DOMSource.class)) {
                        fault.setContent((DOMSource) errorHandler
                                .getMessagesAs(DOMSource.class));
                    } else if (errorHandler
                            .supportsMessageFormat(StringSource.class)) {
                        fault.setContent(sourceTransformer
                                .toDOMSource((StringSource) errorHandler
                                        .getMessagesAs(StringSource.class)));
                    } else if (errorHandler.supportsMessageFormat(String.class)) {
                        fault.setContent(sourceTransformer
                                .toDOMSource(new StringSource(
                                        (String) errorHandler
                                                .getMessagesAs(String.class))));
                    } else {
                        throw new MessagingException(
                                "MessageAwareErrorHandler implementation "
                                        + errorHandler.getClass().getName()
                                        + " does not support a compatible error message format.");
                    }
                } else {
                    /*
                     * we can't do much here if the ErrorHandler implementation
                     * does not support capturing messages
                     */
                    StringBuffer resultString = new StringBuffer();
                    resultString.append(TAG_RESULT_START);
                    resultString.append('\n');
                    resultString.append(TAG_WARNING_START);
                    resultString.append(errorHandler.getWarningCount());
                    resultString.append(TAG_WARNING_END);
                    resultString.append('\n');
                    resultString.append(TAG_ERROR_START);
                    resultString.append(errorHandler.getErrorCount());
                    resultString.append(TAG_ERROR_END);
                    resultString.append('\n');
                    resultString.append(TAG_FATAL_START);
                    resultString.append(errorHandler.getFatalErrorCount());
                    resultString.append(TAG_FATAL_END);
                    resultString.append('\n');
                    resultString.append(TAG_RESULT_END);
                    resultString.append('\n');
                    fault.setContent(new StringSource(resultString.toString()));
                }

                if (!handlingErrorMethod.equalsIgnoreCase(FAULT_FLOW)) {
                    // HANDLE AS JBI FAULT
                    throw new FaultException(
                            "Failed to validate against schema: " + schema,
                            exchange, fault);
                } else {
                    MessageUtil.transfer(fault, out);
                }
            } else {
                // Retrieve the ouput of the validation
                // as it may have been changed by the validator
                out.setContent(new DOMSource(result.getNode(), result
                        .getSystemId()));
            }
        } catch (SAXException e) {
            throw new MessagingException(e);
        } catch (IOException e) {
            throw new MessagingException(e);
        } catch (ParserConfigurationException e) {
            throw new MessagingException(e);
        } catch (TransformerException e) {
            throw new MessagingException(e);
        }
    }

    /**
     * does the validation
     * 
     * @param validator
     * @param src
     * @param result
     * @throws SAXException
     * @throws IOException
     */
    protected void doValidation(Validator validator, DOMSource src,
            DOMResult result) throws SAXException, IOException {
        validator.validate(src, result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#fail(javax.jbi.messaging.MessageExchange,
     *      java.lang.Exception)
     */
    protected void fail(MessageExchange messageExchange, Exception e)
            throws MessagingException {
        super.fail(messageExchange, e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#send(javax.jbi.messaging.MessageExchange)
     */
    protected void send(MessageExchange messageExchange)
            throws MessagingException {
        super.send(messageExchange);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#sendSync(javax.jbi.messaging.MessageExchange)
     */
    protected void sendSync(MessageExchange messageExchange)
            throws MessagingException {
        super.sendSync(messageExchange);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#done(javax.jbi.messaging.MessageExchange)
     */
    protected void done(MessageExchange messageExchange)
            throws MessagingException {
        super.done(messageExchange);
    }

    public String getHandlingErrorMethod() {
        return handlingErrorMethod;
    }

    public void setHandlingErrorMethod(String handlingErrorMethod) {
        this.handlingErrorMethod = handlingErrorMethod;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public String getSchemaLanguage() {
        return schemaLanguage;
    }

    public void setSchemaLanguage(String schemaLanguage) {
        this.schemaLanguage = schemaLanguage;
    }

    public Source getSchemaSource() {
        return schemaSource;
    }

    public void setSchemaSource(Source schemaSource) {
        this.schemaSource = schemaSource;
    }

    public Resource getSchemaResource() {
        return schemaResource;
    }

    public void setSchemaResource(Resource schemaResource) {
        this.schemaResource = schemaResource;
    }

    public MessageAwareErrorHandlerFactory getErrorHandlerFactory() {
        return errorHandlerFactory;
    }

    public void setErrorHandlerFactory(
            MessageAwareErrorHandlerFactory errorHandlerFactory) {
        this.errorHandlerFactory = errorHandlerFactory;
    }
}
