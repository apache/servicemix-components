package org.apache.servicemix.saxon;

import java.util.Iterator;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import net.sf.saxon.Configuration;

import org.apache.servicemix.common.ProviderEndpoint;

public abstract class SaxonEndpoint extends ProviderEndpoint {

    public static String RESULT_BYTES = "bytes";
    public static String RESULT_STRING = "string";
    public static String RESULT_DOM = "dom";
    
    private Configuration configuration;
    private boolean copyProperties = true;
    private boolean copyAttachments = true;
    private boolean copySubject = true;
    private String result = RESULT_DOM;

    /**
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * @return the copyAttachments
     */
    public boolean isCopyAttachments() {
        return copyAttachments;
    }

    /**
     * @param copyAttachments the copyAttachments to set
     */
    public void setCopyAttachments(boolean copyAttachments) {
        this.copyAttachments = copyAttachments;
    }

    /**
     * @return the copyProperties
     */
    public boolean isCopyProperties() {
        return copyProperties;
    }

    /**
     * @param copyProperties the copyProperties to set
     */
    public void setCopyProperties(boolean copyProperties) {
        this.copyProperties = copyProperties;
    }

    /**
     * @return the copySubject
     */
    public boolean isCopySubject() {
        return copySubject;
    }

    /**
     * @param copySubject the copySubject to set
     */
    public void setCopySubject(boolean copySubject) {
        this.copySubject = copySubject;
    }

    // Interface methods
    // -------------------------------------------------------------------------
    
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, this means that another component has requested our service
        // As this exchange is active, this is either an in or a fault (out are send by this component)
        if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            // Exchange has been aborted with an exception
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            // Exchange is active
            } else {
                // Check here if the mep is supported by this component
                if (exchange instanceof InOut == false) {
                   throw new UnsupportedOperationException("Unsupported MEP: " + exchange.getPattern());
                }
                // In message
                if (exchange.getMessage("in") != null) {
                    NormalizedMessage in = exchange.getMessage("in");
                    // Transform the content and send it back
                    NormalizedMessage out = exchange.createMessage();
                    copyPropertiesAndAttachments(in, out);
                    transform(exchange, in, out);
                    exchange.setMessage(out, "out");
                    send(exchange);
                // Fault message
                } else if (exchange.getFault() != null) {
                    done(exchange);
                // This is not compliant with the default MEPs
                } else {
                    throw new IllegalStateException("Provider exchange is ACTIVE, but no in or fault is provided");
                }
            }
        // Unsupported role: this should never happen has we never create exchanges
        } else {
            throw new IllegalStateException("Unsupported role: " + exchange.getRole());
        }
    }
    
    // Implementation methods
    // -------------------------------------------------------------------------
    /**
     * If enabled the properties and attachments are copied to the destination message
     */
    protected void copyPropertiesAndAttachments(NormalizedMessage source, NormalizedMessage dest) throws Exception {
        if (isCopyProperties()) {
            for (Iterator it = source.getPropertyNames().iterator(); it.hasNext();) {
                String name = (String) it.next();
                dest.setProperty(name, source.getProperty(name));
            }
        }
        if (isCopyAttachments()) {
            for (Iterator it = source.getAttachmentNames().iterator(); it.hasNext();) {
                String name = (String) it.next();
                dest.addAttachment(name, source.getAttachment(name));
            }
        }
        if (isCopySubject()) {
            dest.setSecuritySubject(source.getSecuritySubject());
        }
    }
    
    protected abstract void transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception;
    
}
