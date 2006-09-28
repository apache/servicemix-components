package org.apache.servicemix.saxon;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.saxon.Configuration;

import org.apache.servicemix.common.ProviderEndpoint;
import org.apache.servicemix.expression.Expression;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.w3c.dom.Document;

public abstract class SaxonEndpoint extends ProviderEndpoint {

    public static String RESULT_BYTES = "bytes";
    public static String RESULT_STRING = "string";
    public static String RESULT_DOM = "dom";
    
    private Configuration configuration;
    private boolean copyProperties = true;
    private boolean copyAttachments = true;
    private boolean copySubject = true;
    private String result = RESULT_DOM;
    private Resource resource;
    private Expression expression;
    private Resource wsdlResource;

    /**
     * @return the wsdlResource
     */
    public Resource getWsdlResource() {
        return wsdlResource;
    }

    /**
     * @param wsdlResource the wsdlResource to set
     */
    public void setWsdlResource(Resource wsdlResource) {
        this.wsdlResource = wsdlResource;
    }

    /**
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * @param expression the expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * @return the resource
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }

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
    
    public void activate() throws Exception {
        if (wsdlResource != null) {
            setDescription(parse(wsdlResource));
        }
        super.activate();
    }
    
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

    protected Resource getDynamicResource(MessageExchange exchange, NormalizedMessage in) throws Exception {
        Object res = getExpression().evaluate(exchange, in);
        if (res == null) {
            return null;
        }
        if (res instanceof Resource) {
            return (Resource) res;
        }
        if (res instanceof File) {
            return new FileSystemResource((File) res);
        }
        if (res instanceof URL) {
            return new UrlResource((URL) res);
        }
        if (res instanceof URI) {
            return new UrlResource(((URI) res).toURL());
        }
        return new DefaultResourceLoader().getResource(res.toString());
    }
    
    protected Document parse(Resource res) throws Exception {
        URL url = null;
        try {
            res.getURL();
        } catch (IOException e) {
            // Ignore
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(res.getInputStream(), url != null ? url.toExternalForm() : null);
    }
}
