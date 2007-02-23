package org.apache.servicemix.http.endpoints;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        InputStream copy = copyInputStream(request.getInputStream());
        
		String xmlRequest = marshal(copy);
		in.setContent(new StringSource(xmlRequest));
        me.setMessage(in, "in");
        return me;
	}

	public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		super.sendOut(exchange, outMsg, request, response);
	}
	
	/**
	 * Copy the input stream in an attempt to get around 'stream is closed error'. 
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private InputStream copyInputStream(InputStream in) throws IOException {   
		InputStreamReader input = new InputStreamReader(in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter output = new OutputStreamWriter(baos);
		
        char[] buffer1 = new char[1024*2];
        int i = 0;
        while (-1 != (i = input.read(buffer1))) {
            output.write(buffer1, 0, i);
        }
        output.flush();
        
        InputStream newIn = new ByteArrayInputStream(baos.toByteArray());
		return newIn;
	}
	
	private String marshal(InputStream is) throws IOException, ClassNotFoundException {
		Object obj = new ObjectInputStream(is).readObject();
		Writer w = new StringWriter();
		XStream xstream = new XStream(new DomDriver());
		xstream.toXML(obj, w);
		return w.toString();
	}

}
