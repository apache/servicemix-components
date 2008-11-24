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
package org.apache.servicemix.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.jbi.marshaler.PojoMarshaler;
import org.springframework.core.io.Resource;

/**
 * @org.apache.xbean.XBean element="endpoint"
 * @author lhein
 */
public class ScriptingEndpoint extends ProviderEndpoint implements ScriptingEndpointType {
    public static final String KEY_CHANNEL = "deliveryChannel";
    public static final String KEY_COMPONENT_NAMESPACE = "componentNamespace";
    public static final String KEY_CONTEXT = "componentContext";
    public static final String KEY_ENDPOINT = "endpoint";
    public static final String KEY_ENDPOINTNAME = "endpointname";
    public static final String KEY_IN_EXCHANGE = "exchange";
    public static final String KEY_IN_MSG = "inMessage";
    public static final String KEY_OUT_EXCHANGE = "outExchange";
    public static final String KEY_OUT_MSG = "outMessage";
    public static final String KEY_INTERFACENAME = "interfacename";
    public static final String KEY_LOGGER = "log";
    public static final String KEY_SCRIPT = "script";
    public static final String KEY_SERVICENAME = "servicename";
    public static final String KEY_USER_BINDINGS = "bindings";
    public static final String LANGUAGE_AUTODETECT = "autodetect";

    private static Log logger = LogFactory.getLog(ScriptingEndpoint.class);

    private Map<String, Object> bindings;
    private boolean disableOutput;
    private boolean copyProperties;
    private boolean copyAttachments;
    private ScriptEngine engine;
    private String language = LANGUAGE_AUTODETECT;
    private String logResourceBundle;
    private ScriptEngineManager manager;
    private ScriptingMarshalerSupport marshaler = new DefaultScriptingMarshaler();
    private Resource script;
    private Logger scriptLogger;
    private CompiledScript compiledScript;

    private QName targetInterface;
    private QName targetOperation;
    private QName targetService;
    private String targetEndpoint;
    private String targetUri;
    
    /**
     * @return
     * @throws MessagingException
     */
    protected Logger createScriptLogger() throws MessagingException {
        if (logResourceBundle != null) {
            try {
                return getContext().getLogger(getClass().getName(), logResourceBundle);
            } catch (JBIException e) {
                throw new MessagingException(e);
            }
        } else {
            return Logger.getLogger(getClass().getName());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#done(javax.jbi.messaging.MessageExchange)
     */
    protected void done(MessageExchange messageExchange) throws MessagingException {
        super.done(messageExchange);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#fail(javax.jbi.messaging.MessageExchange,
     *      java.lang.Exception)
     */
    protected void fail(MessageExchange messageExchange, Exception e) throws MessagingException {
        super.fail(messageExchange, e);
    }

    /**
     * @return the bindings
     */
    public Map<String, Object> getBindings() {
        return this.bindings;
    }

    /**
     * *
     * 
     * @return Returns the language.
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * *
     * 
     * @return Returns the logResourceBundle.
     */
    public String getLogResourceBundle() {
        return this.logResourceBundle;
    }

    /**
     * @return the marshaler
     */
    public ScriptingMarshalerSupport getMarshaler() {
        return this.marshaler;
    }

    /**
     * @return the script
     */
    public Resource getScript() {
        return this.script;
    }

    /**
     * returns the script logger
     * 
     * @return the script logger
     * @throws MessagingException
     */
    public Logger getScriptLogger() throws MessagingException {
        if (scriptLogger == null) {
            scriptLogger = createScriptLogger();
        }
        return scriptLogger;
    }

    /**
     * @return the disableOutput
     */
    public boolean isDisableOutput() {
        return this.disableOutput;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        if (exchange == null) {
            return;
        }

        // The component acts as a consumer, this means this exchange is
        // received because
        // we sent it to another component. As it is active, this is either an
        // out or a fault
        // If this component does not create / send exchanges, you may just
        // throw an
        // UnsupportedOperationException
        if (exchange.getRole() == Role.CONSUMER) { 
            return; 
        } else if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // The component acts as a provider, this means that another component
            // has requested our
            // service
            // As this exchange is active, this is either an in or a fault (out are
            // send by this
            // component)

            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                // Exchange has been aborted with an exception
                return;
            } else if (exchange.getFault() != null) {
                // Fault message
                done(exchange);
            } else {
                Bindings scriptBindings = engine.createBindings();
                scriptBindings.put(KEY_CONTEXT, getContext());
                scriptBindings.put(KEY_IN_EXCHANGE, exchange);
                scriptBindings.put(KEY_IN_MSG, exchange.getMessage("in"));
                scriptBindings.put(KEY_ENDPOINT, this);
                scriptBindings.put(KEY_CHANNEL, getChannel());
                scriptBindings.put(KEY_ENDPOINTNAME, getEndpoint());
                scriptBindings.put(KEY_SERVICENAME, getService());
                scriptBindings.put(KEY_INTERFACENAME, getInterfaceName());
                scriptBindings.put(KEY_LOGGER, getScriptLogger());

                InOnly outExchange = null;
                
                if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                    outExchange = getExchangeFactory().createInOnlyExchange();
                    String processCorrelationId = (String)exchange.getProperty(JbiConstants.CORRELATION_ID);
                    if (processCorrelationId != null) {
                        outExchange.setProperty(JbiConstants.CORRELATION_ID, processCorrelationId);
                    }                    
                    
                    NormalizedMessage outMsg = outExchange.createMessage();
                    outExchange.setMessage(outMsg, "in");
                    
                    scriptBindings.put(KEY_OUT_EXCHANGE, outExchange);
                    scriptBindings.put(KEY_OUT_MSG, outMsg);                    
                } else {
                    NormalizedMessage outMsg = exchange.createMessage();
                    exchange.setMessage(outMsg, "out");
                    scriptBindings.put(KEY_OUT_EXCHANGE, exchange);
                    scriptBindings.put(KEY_OUT_MSG, outMsg);
                }
                
                try {
                    scriptBindings.put(KEY_SCRIPT, getScript().getFile().getAbsolutePath());
                } catch (IOException ioex) {
                    scriptBindings.put(KEY_SCRIPT, getScript());
                }

                scriptBindings.put(KEY_USER_BINDINGS, bindings);
                scriptBindings.put(KEY_COMPONENT_NAMESPACE, scriptBindings);

                // call back method for custom marshaler to inject it's own beans
                this.marshaler.registerUserBeans(this, exchange, scriptBindings);

                // get the input stream to the script code from the marshaler
                InputStream is = null;
                try {
                    is = this.marshaler.getScriptCode(this, exchange);
                } catch (IOException ioex) {
                    logger.error("Unable to load script in marshaler: "
                                    + this.marshaler.getClass().getName(), ioex);
                }
                // if the marshaler does not return a valid input stream, use the script property to load it
                if (is != null) {
                    try {
                        // execute the script
                        this.engine.eval(new InputStreamReader(is), scriptBindings);
                    } catch (ScriptException ex) {
                        logger.error("Error executing the script: " + ex.getFileName() + " at line: "
                                     + ex.getLineNumber() + " and column: " + ex.getColumnNumber(), ex);
                        throw ex;
                    }
                } else {
                    try {
                        // use the compiled script interfaces if possible
                        if (compiledScript == null && engine instanceof Compilable) {
                            compiledScript = ((Compilable) engine).compile(new InputStreamReader(this.script.getInputStream()));
                        }
                        if (compiledScript != null) {
                            // execute the script
                            compiledScript.eval(scriptBindings);
                        } else {
                            // execute the script
                            engine.eval(new InputStreamReader(this.script.getInputStream()), scriptBindings);
                        }
                    } catch (IOException ioex) {
                        logger.error("Unable to load the script " + script.getFilename(), ioex);
                        throw new MessagingException("Unable to load the script " + script.getFilename());
                    } catch (ScriptException ex) {
                        logger.error("Error executing the script: " + ex.getFileName() + " at line: "
                                     + ex.getLineNumber() + " and column: " + ex.getColumnNumber(), ex);
                        throw ex;
                    }
                }

                if (!isDisableOutput()) {
                    boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
                    
                    // on InOut exchanges we always do answer
                    if (exchange instanceof InOut || exchange instanceof InOptionalOut) {
                        if (txSync) {
                            getChannel().sendSync(exchange);
                        } else {
                            getChannel().send(exchange);
                        }
                    } else {
                        configureTarget(outExchange);
                        
                        if (txSync) {
                            getChannel().sendSync(outExchange);
                        } else {
                            getChannel().send(outExchange);
                        }
                        // if InOnly exchange then we only answer if user wants to
                        done(exchange);
                    }
                } else {
                    // if InOnly exchange then we only answer if user wants to
                    done(exchange);
                }    
            }
        } else { 
            // Unknown role
            throw new MessagingException("ScriptingEndpoint.process(): Unknown role: " + exchange.getRole());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#send(javax.jbi.messaging.MessageExchange)
     */
    protected void send(MessageExchange messageExchange) throws MessagingException {
        super.send(messageExchange);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#sendSync(javax.jbi.messaging.MessageExchange)
     */
    protected void sendSync(MessageExchange messageExchange) throws MessagingException {
        super.sendSync(messageExchange);
    }

    /**
     * @param bindings the bindings to set
     */
    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    /**
     * @param disableOutput the disableOutput to set
     */
    public void setDisableOutput(boolean disableOutput) {
        this.disableOutput = disableOutput;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @param logResourceBundle The logResourceBundle to set.
     */
    public void setLogResourceBundle(String logResourceBundle) {
        this.logResourceBundle = logResourceBundle;
    }

    /**
     * @param marshaler the marshaler to set
     */
    public void setMarshaler(ScriptingMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    /**
     * @param script the script to set
     */
    public void setScript(Resource script) {
        this.script = script;
    }

    /**
     * Sets the logger to use if the script decides to log
     * 
     * @param scriptLogger
     */
    public void setScriptLogger(Logger scriptLogger) {
        this.scriptLogger = scriptLogger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#start()
     */
    public void start() throws Exception {
        super.start();
        try {
            // lazy instatiation
            this.manager = new ScriptEngineManager(serviceUnit.getConfigurationClassLoader());

            if (script == null) {
                throw new IllegalArgumentException("Property script must be set");
            } else {
                // initialize the script engine
                if (this.language.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
                    // detect language by file extension
                    this.engine = this.manager.getEngineByExtension(getExtension(script.getFilename()));
                    if (this.engine == null) {
                        throw new RuntimeException("There is no script engine registered for extension "
                                                   + getExtension(script.getFilename()));
                    }
                } else {
                    // use predefined language from xbean
                    this.engine = this.manager.getEngineByName(this.language);
                    if (this.engine == null) {
                        throw new RuntimeException("There is no script engine for language " + this.language);
                    }
                }
            }

            // do custom startup logic
            marshaler.onStartup(this);
        } catch (Exception ex) {
            throw new JBIException(ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#stop()
     */
    @Override
    public void stop() throws Exception {
        try {
            // do custom startup logic
            marshaler.onShutdown(this);
        } catch (Exception ex) {
            throw new JBIException(ex);
        }

        super.stop();
    }

    //
    // utility
    //

    /**
     * Copies properties from one message to another that do not already exist
     *
     * @param from the message containing the properties
     * @param to the destination message where the properties are set
     */
    protected void copyProperties(NormalizedMessage from, NormalizedMessage to) {
        for (String propertyName : (Set<String>) from.getPropertyNames()) {
            // Do not copy existing properties or transient properties
            if (to.getProperty(propertyName) == null && !PojoMarshaler.BODY.equals(propertyName)) {
                Object value = from.getProperty(propertyName);
                to.setProperty(propertyName, value);
            }
        }
    }

    /**
     * Copies attachments from one message to another that do not already exist
     *
     * @param from the message with the attachments
     * @param to the destination message where the attachments are to be added
     * @throws javax.jbi.messaging.MessagingException if an attachment could not be added
     */
    protected void copyAttachments(NormalizedMessage from, NormalizedMessage to) throws MessagingException {
        for (String attachmentName : (Set<String>) from.getAttachmentNames()) {
            // Do not copy existing attachments
            if (to.getAttachment(attachmentName) == null) {
                DataHandler value = from.getAttachment(attachmentName);
                to.addAttachment(attachmentName, value);
            }
        }
    }
    
    private static final char UNIX_SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';
    public static final char EXTENSION_SEPARATOR = '.';

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfExtension(filename);
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    public static int indexOfExtension(String filename) {
        if (filename == null) {
            return -1;
        }
        int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
        int lastSeparator = indexOfLastSeparator(filename);
        return (lastSeparator > extensionPos ? -1 : extensionPos);
    }

    public static int indexOfLastSeparator(String filename) {
        if (filename == null) {
            return -1;
        }
        int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
        int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
    }
    
    /**
     * Configures the target on the newly created exchange
     * @param exchange the exchange to configure
     * @throws MessagingException if the target could not be configured
     */
    public void configureTarget(MessageExchange exchange) throws MessagingException {
        if (targetInterface == null && targetService == null && targetUri == null) {
            throw new MessagingException("target's interface, service or uri should be specified");
        }
        if (targetUri != null) {
            URIResolver.configureExchange(exchange, getContext(), targetUri);
        }
        if (targetInterface != null) {
            exchange.setInterfaceName(targetInterface);
        }
        if (targetOperation != null) {
            exchange.setOperation(targetOperation);
        }
        if (targetService != null) {
            exchange.setService(targetService);
            if (targetEndpoint != null) {
                ServiceEndpoint se = getContext().getEndpoint(targetService, targetEndpoint);
                exchange.setEndpoint(se);
            }
        }
    }

    /** * @return Returns the copyProperties.
     */
    public boolean isCopyProperties() {
        return this.copyProperties;
    }

    /**
     * @param copyProperties The copyProperties to set.
     */
    public void setCopyProperties(boolean copyProperties) {
        this.copyProperties = copyProperties;
    }

    /** * @return Returns the copyAttachments.
     */
    public boolean isCopyAttachments() {
        return this.copyAttachments;
    }

    /**
     * @param copyAttachments The copyAttachments to set.
     */
    public void setCopyAttachments(boolean copyAttachments) {
        this.copyAttachments = copyAttachments;
    }

    /** * @return Returns the targetInterface.
     */
    public QName getTargetInterface() {
        return this.targetInterface;
    }

    /**
     * @param targetInterface The targetInterface to set.
     */
    public void setTargetInterface(QName targetInterface) {
        this.targetInterface = targetInterface;
    }

    /** * @return Returns the targetOperation.
     */
    public QName getTargetOperation() {
        return this.targetOperation;
    }

    /**
     * @param targetOperation The targetOperation to set.
     */
    public void setTargetOperation(QName targetOperation) {
        this.targetOperation = targetOperation;
    }

    /** * @return Returns the targetService.
     */
    public QName getTargetService() {
        return this.targetService;
    }

    /**
     * @param targetService The targetService to set.
     */
    public void setTargetService(QName targetService) {
        this.targetService = targetService;
    }

    /** * @return Returns the targetEndpoint.
     */
    public String getTargetEndpoint() {
        return this.targetEndpoint;
    }

    /**
     * @param targetEndpoint The targetEndpoint to set.
     */
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    /** * @return Returns the targetUri.
     */
    public String getTargetUri() {
        return this.targetUri;
    }

    /**
     * @param targetUri The targetUri to set.
     */
    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }
}
