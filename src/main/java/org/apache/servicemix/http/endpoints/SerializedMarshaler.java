package org.apache.servicemix.http.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * 
 * @author bsnyder
 * @org.apache.xbean.XBean element="serializedMarshaler"
 */
public class SerializedMarshaler extends DefaultHttpConsumerMarshaler {
	
	public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
		MessageExchange me =
        	context.getDeliveryChannel().createExchangeFactory().createExchange(getDefaultMep());
        NormalizedMessage in = me.createMessage(); 
		in.setContent(marshal(request.getInputStream()));
        me.setMessage(in, "in");
        return me;
	}

	public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (outMsg.getContent() != null) {
            unmarshal(response.getOutputStream(), outMsg.getContent());
        }
    }

    /**
     * Marshal the byte content of the input stream to an xml source
     * @param is - input stream to read the object from
     * @return xml source
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Source marshal(InputStream is) throws IOException, ClassNotFoundException {
		Object obj = new ObjectInputStream(is).readObject();
		Writer w = new StringWriter();
		XStream xstream = new XStream(new DomDriver());
		xstream.toXML(obj, w);
		return new StringSource(w.toString());
	}

    /**
     * Unmarshal the xml content to the specified output stream
     * @param os - output stream to unmarshal to
     * @param content - xml source
     * @throws TransformerException
     * @throws IOException
     */
    private void unmarshal(OutputStream os, Source content) throws TransformerException, IOException {
        SourceTransformer transform = new SourceTransformer();
        XStream xstream = new XStream(new DomDriver());

        Object obj = xstream.fromXML(transform.toString(content));
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(obj);
    }
}
