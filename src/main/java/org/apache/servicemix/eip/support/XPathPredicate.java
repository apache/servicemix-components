package org.apache.servicemix.eip.support;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.expression.JAXPBooleanXPathExpression;
import org.apache.servicemix.jbi.util.MessageUtil;

/**
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="xpath-predicate"
 */
public class XPathPredicate extends JAXPBooleanXPathExpression implements Predicate {

    private static final Log log = LogFactory.getLog(XPathPredicate.class);
    
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
            NormalizedMessage in = MessageUtil.copyIn(exchange);
            Boolean match = (Boolean) evaluate(exchange, in);
            return Boolean.TRUE.equals(match);
        } catch (Exception e) {
            log.warn("Could not evaluate xpath expression", e);
            return false;
        }
    }

}
