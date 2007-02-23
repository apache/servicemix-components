package org.apache.servicemix.http.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * 
 * @author bsnyder
 * @org.apache.xbean.XBean element="serializedMarshaler"
 */
public class SerializedMarshaler extends DefaultHttpConsumerMarshaler {
	
	public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
        URI inOnlyMepUri = MessageExchangeSupport.IN_ONLY;
		MessageExchange me = 
        	context.getDeliveryChannel().createExchangeFactory().createExchange(inOnlyMepUri);
        NormalizedMessage in = me.createMessage();

		String xmlRequest = marshal(request.getInputStream());
		in.setContent(new StringSource(xmlRequest));
        me.setMessage(in, "in");
        return me;
	}
	
	protected String marshal(InputStream is) throws IOException, ClassNotFoundException {
		Object obj = new ObjectInputStream(is).readObject();
		Writer w = new StringWriter();
		XStream xstream = new XStream(new DomDriver());
		xstream.toXML(obj, w);
		return w.toString();
	}

}
