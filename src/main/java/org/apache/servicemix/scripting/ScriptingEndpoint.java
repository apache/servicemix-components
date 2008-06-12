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
import java.io.Reader;
import java.util.Map;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.springframework.core.io.Resource;

/**
 * @org.apache.xbean.XBean element="endpoint"
 * @author lhein
 */
public class ScriptingEndpoint extends ProviderEndpoint implements ScriptingEndpointType {
    private static Log logger = LogFactory.getLog(ScriptingEndpoint.class);

    public static final String KEY_CONTEXT = "componentContext";
    public static final String KEY_ENDPOINT = "endpoint";
    public static final String KEY_IN_MSG = "inMessage";
    public static final String KEY_IN_EXCHANGE = "exchange";
    public static final String KEY_CHANNEL = "deliveryChannel";
    public static final String KEY_LOGGER = "log";
    public static final String KEY_COMPONENT_NAMESPACE = "componentNamespace";
    public static final String KEY_USER_BINDINGS = "bindings";
    public static final String KEY_ENDPOINTNAME = "endpointname";
    public static final String KEY_SERVICENAME = "servicename";
    public static final String KEY_INTERFACENAME = "interfacename";
    public static final String KEY_SCRIPT = "script";

    public static final String LANGUAGE_AUTODETECT = "autodetect";

    private ScriptEngineManager manager = null;
    private ScriptEngine engine = null;
    private ScriptingMarshalerSupport marshaler = new DefaultScriptingMarshaler();
    private Resource script;
    private String language = LANGUAGE_AUTODETECT;
    private String logResourceBundle;
    private Logger scriptLogger;
    private boolean disableOutput = false;
    private Map<String, Object> bindings;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#start()
     */
    public void start() throws Exception {
        super.start();
        try {
            // lazy instatiation
            this.manager = new ScriptEngineManager();

            if (script == null) {
                throw new IllegalArgumentException("Property script must be set");
            } else {
                // initialize the script engine
                if (this.language.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
                    // detect language by file extension
                    this.engine = this.manager.getEngineByExtension(FilenameUtils.getExtension(script
                        .getFilename()));
                    if (this.engine == null) {
                        throw new RuntimeException("There is no script engine registered for extension "
                                                   + FilenameUtils.getExtension(script.getFilename()));
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
        }

        // The component acts as a provider, this means that another component
        // has requested our
        // service
        // As this exchange is active, this is either an in or a fault (out are
        // send by this
        // component)
        else if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            }
            // Exchange has been aborted with an exception
            else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            }
            // Fault message
            else if (exchange.getFault() != null) {
                done(exchange);
            } else {
                Bindings scriptBindings = engine.createBindings();

                scriptBindings.put(KEY_IN_EXCHANGE, exchange);

                // exec script engine code to do its thing for this
                scriptBindings.put(KEY_CONTEXT, getContext());
                scriptBindings.put(KEY_IN_EXCHANGE, exchange);
                scriptBindings.put(KEY_IN_MSG, exchange.getMessage("in"));
                scriptBindings.put(KEY_ENDPOINT, this);
                scriptBindings.put(KEY_CHANNEL, getChannel());
                scriptBindings.put(KEY_ENDPOINTNAME, getEndpoint());
                scriptBindings.put(KEY_SERVICENAME, getService());
                scriptBindings.put(KEY_INTERFACENAME, getInterfaceName());
                scriptBindings.put(KEY_LOGGER, getScriptLogger());

                try {
                    scriptBindings.put(KEY_SCRIPT, getScript().getFile().getAbsolutePath());
                } catch (IOException ioex) {
                    scriptBindings.put(KEY_SCRIPT, getScript());
                }

                scriptBindings.put(KEY_USER_BINDINGS, bindings);
                scriptBindings.put(KEY_COMPONENT_NAMESPACE, scriptBindings);

                // call back method for custom marshaler to inject it's own
                // beans
                this.marshaler.registerUserBeans(this, exchange, scriptBindings);

                // get the input stream to the script code
                InputStream is = null;
                try {
                    // gets the input stream to the script code
                    is = this.marshaler.getScriptCode(this, exchange);
                } catch (IOException ioex) {
                    logger
                        .error("Unable to load script in marshaler: " + this.marshaler.getClass().getName(),
                               ioex);
                    // io error getting the script code
                    try {
                        is = this.script.getInputStream();
                    } catch (IOException i2) {
                        logger.error("Unable to load the script " + script.getFilename(), i2);
                        throw new MessagingException("Unable to load the script " + script.getFilename());
                    }
                }

                // create a reader for the stream
                Reader reader = new InputStreamReader(is);

                try {
                    // execute the script
                    this.engine.eval(reader, scriptBindings);

                    exchange = (MessageExchange)scriptBindings.get(KEY_IN_EXCHANGE);

                    // on InOut exchanges we always do answer
                    if (exchange instanceof InOut) {
                        if (!isDisableOutput()) {
                            send(exchange);
                        }
                    } else {
                        // if InOnly exchange then we only answer if user wants to
                        done(exchange);
                    }
                } catch (ScriptException ex) {
                    logger.error("Error executing the script: " + ex.getFileName() + " at line: "
                                 + ex.getLineNumber() + " and column: " + ex.getColumnNumber(), ex);
                    fail(exchange, ex);
                } catch (NullPointerException ex) {
                    logger.error("Error executing the script: " + script.getFilename()
                                 + ". A unexpected NullPointerException occured.", ex);
                    fail(exchange, ex);
                }
            }
        }

        // Unknown role
        else {
            throw new MessagingException("ScriptingEndpoint.process(): Unknown role: " + exchange.getRole());
        }
    }

    /**
     * @return the script
     */
    public Resource getScript() {
        return this.script;
    }

    /**
     * @param script the script to set
     */
    public void setScript(Resource script) {
        this.script = script;
    }

    /**
     * @return the marshaler
     */
    public ScriptingMarshalerSupport getMarshaler() {
        return this.marshaler;
    }

    /**
     * @param marshaler the marshaler to set
     */
    public void setMarshaler(ScriptingMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    /**
     * @return the disableOutput
     */
    public boolean isDisableOutput() {
        return this.disableOutput;
    }

    /**
     * @param disableOutput the disableOutput to set
     */
    public void setDisableOutput(boolean disableOutput) {
        this.disableOutput = disableOutput;
    }

    /**
     * @return the bindings
     */
    public Map<String, Object> getBindings() {
        return this.bindings;
    }

    /**
     * @param bindings the bindings to set
     */
    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
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

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#done(javax.jbi.messaging.MessageExchange)
     */
    protected void done(MessageExchange messageExchange) throws MessagingException {
        super.done(messageExchange);
    }

    /**
     * 
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

    /**
     * *
     * 
     * @return Returns the language.
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /** * @return Returns the logResourceBundle.
     */
    public String getLogResourceBundle() {
        return this.logResourceBundle;
    }

    /**
     * @param logResourceBundle The logResourceBundle to set.
     */
    public void setLogResourceBundle(String logResourceBundle) {
        this.logResourceBundle = logResourceBundle;
    }

    /**
     * returns the script logger
     * @return  the script logger
     * @throws MessagingException
     */
    public Logger getScriptLogger() throws MessagingException {
        if (scriptLogger == null) {
            scriptLogger = createScriptLogger();
        }
        return scriptLogger;
    }

    /**
     * Sets the logger to use if the script decides to log
     *
     * @param scriptLogger
     */
    public void setScriptLogger(Logger scriptLogger) {
        this.scriptLogger = scriptLogger;
    }
}
