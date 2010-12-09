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
package org.apache.servicemix.pdf;

import java.io.FileOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.pdf.marshaler.JaxbPdfComposerMarshaler;
import org.apache.servicemix.pdf.marshaler.PdfComposerDataField;
import org.apache.servicemix.pdf.marshaler.PdfComposerMarshalerSupport;
import org.apache.servicemix.pdf.marshaler.PdfComposerRequest;
import org.apache.servicemix.soap.util.DomUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

/**
 * <p>
 * Represents a PDF Composer endpoint.
 * </p>
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="composer"
 */
public class PdfComposerEndpoint extends ProviderEndpoint {
    
    public final static String DEFAULT_WSDL = "servicemix-pdf-composer.wsdl"; // the default abstract WSDL
    
    private String template; // the template can be static (define in the endpoint descriptor) or provided in the "in" message
    private String templatesDir = "."; // the location where to looking for template files
    private String outputDir = null; // in case of InOnly exchange, write file to the output directory
    private Resource wsdl; // the abstract WSDL describing the endpoint behavior
    private PdfComposerMarshalerSupport marshaler = new JaxbPdfComposerMarshaler();
    
    public String getTemplate() {
        return this.template;
    }
    
    /**
     * <p>
     * This attribute specifies the template to use if it's not defined in the incoming message.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <code>null</code>. 
     * 
     * @param template the default template file path.
     */
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public String getTemplatesDir() {
        return this.templatesDir;
    }
    
    /**
     * <p>
     * This attribute specifies the directory where to looking for template files.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <code>.</code>.
     * 
     * @param templatesDir the basedir path.
     */
    public void setTemplatesDir(String templatesDir) {
        this.templatesDir = templatesDir;
    }
    
    public String getOutputDir() {
        return this.outputDir;
    }
    
    /**
     * <p>
     * This attribute specifies the output directory where the PDF composer generates resulting PDF files.
     * It's used when the incoming message exchange is in-only, in the case of in-out, the resulting PDF
     * is send in out message.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <code>null</code>.
     * 
     * @param outputDir the PDF output directory.
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
    
    public Resource getWsdl() {
        return this.wsdl;
    }
    
    /**
     * <p>
     * This attribute specifies the abstract WSDL describing the endpoint behavior.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <code>null</code>.
     * 
     * @param wsdl the WSDL <code>Resource</code>.
     */
    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }
    
    public PdfComposerMarshalerSupport getMarshaler() {
        return this.marshaler;
    }
    
    /**
     * <p>
     * With this method you can specify a marshaler class which provides the
     * logic for converting a message into a PDF Composer request. This class has
     * to implement the interface <code>PdfComposerMarshalerSupport</code>. If you don't
     * specify a marshaler, the <code>DefaultPdfComposerMarshaler</code> will be used.
     * </p>
     * 
     * @param marshaller
     */
    public void setMarshaler(PdfComposerMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        try {
            if (wsdl == null) {
                // the user hasn't provided the WSDL, we use the default one
                wsdl = new ClassPathResource(DEFAULT_WSDL);
            }

            // load the WSDL in the endpoint
            description = DomUtil.parse(wsdl.getInputStream());
            definition = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);

            // cleanup WSDL to be sure thats it's an abstract one
            // cleanup services
            QName[] qnames = (QName[]) definition.getServices().keySet().toArray(new QName[0]);
            for (int i = 0; i < qnames.length; i++) {
                definition.removeService(qnames[i]);
            }
            // cleanup binding
            qnames = (QName[]) definition.getBindings().keySet().toArray(new QName[0]);
            for (int i = 0; i < qnames.length; i++) {
                definition.removeBinding(qnames[i]);
            }
        } catch (Exception e) {
            throw new DeploymentException("Can't load WSDL.", e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, it means that another components
        // requests our service.
        // As this exchange is active, this is either an "in" or a "fault" (out
        // is sent by this component)
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            // exchange is finished
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // exchange has been aborted with an exception
            return;
        } else {
            // exchange is active
            handleProviderExchange(exchange);
        }
    }
    
    /**
     * <p>
     * Handles the message exchange (provider role) using the marshaler.
     * </p>
     * 
     * @param exchange the <code>MessageExchange</code>.
     * @throws Exception
     */
    protected void handleProviderExchange(MessageExchange exchange) throws Exception {
        // fault message
        if (exchange.getFault() != null) {
            done(exchange);
            return;
        }
        
        // check if we have an InOnly MEP and outputDirectory attribute define
        if (exchange instanceof InOnly && outputDir == null) {
            throw new IllegalStateException("InOnly message exchange received but the outputDir attribute is not defined. The outputDir attribute is mandatory in case of InOnly MEP.");
        }
        
        // check if we have an in message 
        if (exchange.getMessage("in") == null) {
            throw new MessagingException("No \"in\" message.");
        }
        
        // get the in message
        NormalizedMessage in = exchange.getMessage("in");
        // unmarshal the PDF Composer request in the "in" message
        PdfComposerRequest request = marshaler.unmarshal(in);
        
        // if the template is not contained in the request, fall back to the default one
        String templateToUse = request.getTemplate();
        if (templateToUse == null) {
            templateToUse = this.template;
        }
        if (templateToUse == null) {
            throw new IllegalArgumentException("No template found in the \"in\" message or in the endpoint descriptor.");
        }
        if (templatesDir != null) {
            templateToUse = templatesDir + "/" + templateToUse;
        }
        
        // load PDF template
        PdfReader templateReader = new PdfReader(templateToUse);
        
        // create a stamper to populate the target document
        PdfStamper stamper;
        PipedInputStream stream = new PipedInputStream();
        if (exchange instanceof InOut) {
            // when we have an InOut, we use a pipe
            stamper = new PdfStamper(templateReader, new PipedOutputStream(stream));
        } else {
            // when we have an InOnly, we directly write a file in the output directory
            FileOutputStream fileStream = new FileOutputStream(outputDir + "/test.pdf");
            stamper = new PdfStamper(templateReader, fileStream);
        }
        
        // get the acrofields
        AcroFields fields = stamper.getAcroFields();
        // replace the field
        for (PdfComposerDataField field:request.getData()) {
            fields.setField(field.getName(), field.getValue());
        }
        
        // close the stamper
        stamper.setFormFlattening(true);
        stamper.close();
        
        if (exchange instanceof InOut) {
            // create "out" message
            NormalizedMessage out = exchange.createMessage();
            // set the "out" message content with the piped stream
            out.setContent(new StreamSource(stream));
        
            // set the "out" message of the exchange
            exchange.setMessage(out, "out");
        } else {
            // the exchange is InOnly, Robust InOnly, In Optional Out
            // set the exchange as DONE
            exchange.setStatus(ExchangeStatus.DONE);
        }

        // send back the exchange
        send(exchange);
    }

}
