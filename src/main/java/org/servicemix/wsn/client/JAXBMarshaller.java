package org.servicemix.wsn.client;

import java.io.StringWriter;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;

import org.servicemix.jbi.jaxp.StringSource;
import org.servicemix.jbi.messaging.DefaultMarshaler;

public class JAXBMarshaller extends DefaultMarshaler {

	private JAXBContext context;
	
	public JAXBMarshaller(JAXBContext context) {
		this.context = context;
	}

	public JAXBContext getContext() {
		return context;
	}

	public void setContext(JAXBContext context) {
		this.context = context;
	}
	
    protected Object defaultUnmarshal(MessageExchange exchange, NormalizedMessage message) {
        try {
        	Source content = message.getContent();
        	return context.createUnmarshaller().unmarshal(content);
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }

    protected Source asContent(NormalizedMessage message, Object body) {
    	try {
	    	StringWriter writer = new StringWriter();
	    	context.createMarshaller().marshal(body, writer);
	    	return new StringSource(writer.toString());
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
}
