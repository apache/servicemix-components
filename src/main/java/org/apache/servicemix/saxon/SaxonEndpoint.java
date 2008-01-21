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
package org.apache.servicemix.saxon;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import net.sf.saxon.Configuration;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.expression.Expression;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public abstract class SaxonEndpoint extends ProviderEndpoint {

    public static final String RESULT_BYTES = "bytes";
    public static final String RESULT_STRING = "string";
    public static final String RESULT_DOM = "dom";
    
    private Configuration configuration;
    private boolean copyProperties = true;
    private boolean copyAttachments = true;
    private boolean copySubject = true;
    private String result = RESULT_DOM;
    private Resource resource;
    private Expression expression;
    private Resource wsdlResource;
    private SourceTransformer sourceTransformer = new SourceTransformer();
    private Map parameters;

    /**
     * @param sourceTransformer the sourceTransformer to set
     */
    protected void setSourceTransformer(SourceTransformer sourceTransformer) {
        this.sourceTransformer = sourceTransformer;
    }

    /**
     * @return the sourceTransformer
     */
    public SourceTransformer getSourceTransformer() {
        return sourceTransformer;
    }

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

    /**
     * @return the parameters
     */
    public Map getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    // Interface methods
    // -------------------------------------------------------------------------
    
    public void activate() throws Exception {
        if (wsdlResource != null) {
            setDescription(parse(wsdlResource));
        }
        super.activate();
    }


    // Implementation methods
    // -------------------------------------------------------------------------

    /**
     * Transform the content and send it back
     */
    protected void processInOut(MessageExchange exchange, 
                                NormalizedMessage in, 
                                NormalizedMessage out) throws Exception {
        copyPropertiesAndAttachments(in, out);
        transform(exchange, in, out);
    }


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
    
    protected abstract void transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) 
        throws Exception;

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
            url = res.getURL();
        } catch (IOException e) {
            // Ignore
        }
        DocumentBuilder builder = sourceTransformer.createDocumentBuilder();
        return builder.parse(res.getInputStream(), url != null ? url.toExternalForm() : null);
    }

}
