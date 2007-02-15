package org.apache.servicemix.jms.endpoint;

import java.util.Map;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;

public class DefaultProviderMarshaler implements JmsProviderMarshaler {

    private Map<String, Object> jmsProperties;
    private SourceTransformer transformer = new SourceTransformer();
    
    /**
     * @return the jmsProperties
     */
    public Map<String, Object> getJmsProperties() {
        return jmsProperties;
    }

    /**
     * @param jmsProperties the jmsProperties to set
     */
    public void setJmsProperties(Map<String, Object> jmsProperties) {
        this.jmsProperties = jmsProperties;
    }

    public Message createMessage(MessageExchange exchange, NormalizedMessage in, Session session) throws Exception {
        TextMessage text = session.createTextMessage();
        text.setText(transformer.contentToString(in));
        if (jmsProperties != null) {
            for (Map.Entry<String, Object> e : jmsProperties.entrySet()) {
                text.setObjectProperty(e.getKey(), e.getValue());
            }
        }
        return text;
    }

    public Object getDestination(MessageExchange exchange) {
        // TODO Auto-generated method stub
        return null;
    }

}
