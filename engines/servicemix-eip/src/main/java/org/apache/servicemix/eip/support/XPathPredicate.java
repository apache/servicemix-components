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
package org.apache.servicemix.eip.support;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.namespace.NamespaceContext;

import org.apache.servicemix.expression.JAXPBooleanXPathExpression;
import org.apache.servicemix.expression.MessageVariableResolver;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A predicate that verify if the xpath expression evaluated on
 * the content of the input message is true or not. 
 *
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="xpath-predicate"
 */
public class XPathPredicate extends JAXPBooleanXPathExpression implements Predicate {

    private final Logger logger = LoggerFactory.getLogger(XPathPredicate.class);
    
    public XPathPredicate() {
    }
    
    public XPathPredicate(String xpath) throws Exception {
        super(xpath);
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.components.eip.RoutingRule#matches(javax.jbi.messaging.MessageExchange)
     */
    public boolean matches(MessageExchange exchange) {
        try {
            NormalizedMessage in = exchange.getMessage("in");
            Boolean match = (Boolean) evaluate(exchange, in);
            return Boolean.TRUE.equals(match);
        } catch (Exception e) {
            logger.warn("Could not evaluate xpath expression", e);
            return false;
        }
    }

    /**
     * The variable resolver.  The default one will enable the use of properties on the message, exchange,
     * as well as making system properties and environment properties available.
     */
    @Override
    public void setVariableResolver(MessageVariableResolver messageVariableResolver) {
        super.setVariableResolver(messageVariableResolver);
    }

    /**
     * Ensure re-readability of the content if the expression also needs to
     * access the content.
     */
    @Override
    public void setUseMessageContent(boolean b) {
        super.setUseMessageContent(b);
    }

    /**
     * The xpath expression used to evaluate the predicate.
     */
    @Override
    public void setXPath(String s) {
        super.setXPath(s);
    }

    /**
     * @org.apache.xbean.Property hidden="true"
     */
    @Override
    public void setTransformer(SourceTransformer sourceTransformer) {
        super.setTransformer(sourceTransformer);
    }

    /**
     * The XPath factory.  If no factory is explicitely configured, a defaut one will be created
     * using <code>XPathFactory.newInstance()</code>.
     */
    @Override
    public void setFactory(XPathFactory xPathFactory) {
        super.setFactory(xPathFactory);
    }

    /**
     * The function resolver.
     */
    @Override
    public void setFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        super.setFunctionResolver(xPathFunctionResolver);
    }

    /**
     * The namespace context to use when evaluating the xpath expression
     */
    @Override
    public void setNamespaceContext(NamespaceContext namespaceContext) {
        super.setNamespaceContext(namespaceContext);
    }
}
