/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jbi.management.DeploymentException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunctionResolver;

import org.apache.servicemix.eip.support.AbstractSplitter;
import org.apache.servicemix.expression.JAXPNodeSetXPathExpression;
import org.apache.servicemix.expression.MessageVariableResolver;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The XPathSplitter component implements the 
 * <a href="http://www.enterpriseintegrationpatterns.com/Sequencer.html">Splitter</a>
 * pattern using an xpath expression to split the incoming xml. 
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="xpath-splitter"
 *                  description="An XPath Splitter"
 */
public class XPathSplitter extends AbstractSplitter {

    /**
     * The xpath expression to use to split 
     */
    private JAXPNodeSetXPathExpression xpathExpression = new JAXPNodeSetXPathExpression();

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#validate()
     */
    public void validate() throws DeploymentException {
        super.validate();
        // Check xpath expression
        try {
            xpathExpression.afterPropertiesSet();
        } catch (Exception e) {
            throw new DeploymentException("Error validating xpath expression", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.components.eip.AbstractSplitter#split(javax.xml.transform.Source)
     */
    protected Source[] split(Source main) throws Exception {
        Node doc = new SourceTransformer().toDOMNode(main);
        NodeList nodes = (NodeList) xpathExpression.evaluateXPath(doc);
        Source[] parts = new Source[nodes.getLength()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new DOMSource(nodes.item(i));
        }
        return parts;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#getFactory()
     */
    public XPathFactory getFactory() {
        return xpathExpression.getFactory();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#getFunctionResolver()
     */
    public XPathFunctionResolver getFunctionResolver() {
        return xpathExpression.getFunctionResolver();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#getNamespaceContext()
     */
    public NamespaceContext getNamespaceContext() {
        return xpathExpression.getNamespaceContext();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#getTransformer()
     */
    public SourceTransformer getTransformer() {
        return xpathExpression.getTransformer();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#getVariableResolver()
     */
    public MessageVariableResolver getVariableResolver() {
        return xpathExpression.getVariableResolver();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#getXPath()
     */
    public String getXPath() {
        return xpathExpression.getXPath();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#setFactory(javax.xml.xpath.XPathFactory)
     */
    public void setFactory(XPathFactory factory) {
        xpathExpression.setFactory(factory);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#setFunctionResolver(javax.xml.xpath.XPathFunctionResolver)
     */
    public void setFunctionResolver(XPathFunctionResolver functionResolver) {
        xpathExpression.setFunctionResolver(functionResolver);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#setNamespaceContext(javax.xml.namespace.NamespaceContext)
     */
    public void setNamespaceContext(NamespaceContext namespaceContext) {
        xpathExpression.setNamespaceContext(namespaceContext);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#setTransformer(org.apache.servicemix.jbi.jaxp.SourceTransformer)
     */
    public void setTransformer(SourceTransformer transformer) {
        xpathExpression.setTransformer(transformer);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.expression.JAXPXPathExpression#setVariableResolver(org.apache.servicemix.expression.MessageVariableResolver)
     */
    public void setVariableResolver(MessageVariableResolver variableResolver) {
        xpathExpression.setVariableResolver(variableResolver);
    }

    /**
     * @org.apache.xbean.Property alias="xpath"
     */
    public void setXPath(String xpath) {
        xpathExpression.setXPath(xpath);
    }

}
