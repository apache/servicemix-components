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
package org.apache.servicemix.eip.patterns;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implementation of the 
 * <a href="http://www.enterpriseintegrationpatterns.com/DataEnricher.html">'Content-Enricher'</a> 
 * Pattern. 
 *  
 * @org.apache.xbean.XBean element="content-enricher"
 *                  description="A Content Enricher"
 */
public class ContentEnricher extends EIPEndpoint {

    /**
     * The address of the target endpoint
     */
    private ExchangeTarget target;
    
    /**
     * the target to enrich the request
     */
    private ExchangeTarget enricherTarget;
    
    /**
     * the QName of the resulting root node
     */
    private QName enricherElementName = new QName("enricher");
    /**
     * the QName of the element which contains the 'IN Message'
     * within the response message
     */
    private QName requestElementName = new QName("request");
    /**
     * the QName of the element which contains the message 
     * which was produced by the enricherTarget within the 
     * response message
     */
    private QName resultElementName = new QName("result");
    
    /**
     * returns the QName of the resulting root node
     * @return QName of the resulting root node
     */
    public QName getEnricherElementName() {
		return enricherElementName;
	}

    /**
     * Sets the QName of the resulting root node
     * @param enricherElementName QName of the resulting root node
     */
	public void setEnricherElementName(QName enricherElementName) {
		this.enricherElementName = enricherElementName;
	}

	/**
	 * Returns the QName of the element which contains the 'IN Message'
     * within the response message
     * 
	 * @return QName 
	 */
	public QName getRequestElementName() {
		return requestElementName;
	}

	/**
	 * Sets the QName of the element which contains the 'IN Message'
     * within the response message
     * 
	 * @param requestElementName QName
	 */
	public void setRequestElementName(QName requestElementName) {
		this.requestElementName = requestElementName;
	}

	/**
	 * Returns the QName of the element which contains the message 
     * which was produced by the enricherTarget within the 
     * response message
     * 
	 * @return QName
	 */
	public QName getResultElementName() {
		return resultElementName;
	}

	/**
	 * Sets the QName of the element which contains the message 
     * which was produced by the enricherTarget within the 
     * response message
     * 
	 * @param resultElementName QName
	 */
	public void setResultElementName(QName resultElementName) {
		this.resultElementName = resultElementName;
	}

    protected void processAsync(MessageExchange exchange) throws Exception {
        throw new IllegalStateException();
    }

    protected void processSync(MessageExchange exchange) throws Exception {
        throw new IllegalStateException();
    }

    public void process(MessageExchange exchange) throws Exception {
    	    	
        if (exchange instanceof InOnly == false &&
                exchange instanceof RobustInOnly == false) {
                fail(exchange, 
                		new UnsupportedOperationException(
                				"Use an InOnly or RobustInOnly MEP"));
        }
    	
        // Skip done exchanges
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        // Handle error exchanges
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }
        	        
    	InOut enricherTargetME = getExchangeFactory().createInOutExchange();
        enricherTarget.configureTarget(enricherTargetME, getContext());
        MessageUtil.transferInToIn(exchange, enricherTargetME);
                
        sendSync(enricherTargetME);

        if (enricherTargetME.getStatus() == ExchangeStatus.ERROR) {
            fail(exchange, enricherTargetME.getError());
            return;
        }
     
        Document document = combineToDOMDocument(exchange.getMessage("in"), 
        		enricherTargetME.getMessage("out"));
                
        done(enricherTargetME);

    	MessageExchange outExchange = 
    		getExchangeFactory().createInOnlyExchange();
    	NormalizedMessage out = outExchange.createMessage();
    	target.configureTarget(outExchange, getContext());
        out.setContent(new DOMSource(document));
        
        outExchange.setMessage(out, "in");
                
        sendSync(outExchange);
        done(exchange);
        
    }

    /**
     * Combines two NormalizedMessages to one DOM Document. The
     * element Names are specified via the following properties:
     * enricherElementName, requestElementName, resultElementName
     * 
     * Example:
     *    Content of Message1 :
     *    
     *        <hello/>
     *        
     *    Content of Message 2:
     *    
     *    	  <message2/>
     * 
     *    Result of this method a DOM Document containing the following:
     *    
     *    <enricher>
     *      <request>
     *      	<hello/>
     *      </request>
     *      <result>
     *      	<message2/>
     *      </result>
     *    </enricher>
     * 
     */
	private Document combineToDOMDocument(NormalizedMessage requestMessage, NormalizedMessage targetResultMessage) 
		throws Exception, ParserConfigurationException {
		
        Node originalDocumentNode = getDOMNode(requestMessage.getContent());
        Node targetResultNode = getDOMNode(targetResultMessage.getContent());
                
        Document document = new SourceTransformer().createDocument();
        Element enricherElement = createChildElement(enricherElementName, document);
        Element requestElement = createChildElement(requestElementName, document);
        
        Node node = document.importNode(originalDocumentNode, true);
        requestElement.appendChild(node);
        enricherElement.appendChild(requestElement);
        document.appendChild(enricherElement);
        
        Element resultElement = createChildElement(resultElementName, document);
        
        Node node2 = document.importNode(targetResultNode, true);
        
        resultElement.appendChild(node2);
        enricherElement.appendChild(resultElement);
		return document;
		
	}
    
    private Element createChildElement(QName name, Document document) {
        Element elem;
        if ("".equals(name.getNamespaceURI())) {
            elem = document.createElement(name.getLocalPart());   
        } else {
            elem = document.createElementNS(name.getNamespaceURI(),
            		name.getPrefix() + ":" + name.getLocalPart());
        }
        return elem;
    }
	
    private Node getDOMNode(Source source) throws Exception {
    	SourceTransformer sourceTransformer = new SourceTransformer();
    	Node node = sourceTransformer.toDOMNode(source);
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
        	node = ((Document)node).getDocumentElement();
        }
        return node;
    }
        
    public void setTarget(ExchangeTarget target) {
        this.target = target;
    }
    
    public void setEnricherTarget(ExchangeTarget enricherTarget) {
        this.enricherTarget = enricherTarget;
    }
}
