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
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
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
		MessageExchange me =
        	context.getDeliveryChannel().createExchangeFactory().createExchange(getDefaultMep());
        NormalizedMessage in = me.createMessage();

//        InputStream copy = copyInputStream(request.getInputStream());

		in.setContent(marshal(request.getInputStream()));
        me.setMessage(in, "in");
        return me;
	}

	public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (outMsg.getContent() != null) {
            unmarshal(response.getOutputStream(), outMsg.getContent());
        }
    }
	
//	/**
//	 * Copy the input stream in an attempt to get around 'stream is closed error'.
//	 *
//	 * @param in
//	 * @return
//	 * @throws IOException
//	 */
//	private InputStream copyInputStream(InputStream in) throws IOException {
//		InputStreamReader input = new InputStreamReader(in);
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		OutputStreamWriter output = new OutputStreamWriter(baos);
//
//        char[] buffer1 = new char[1024*2];
//        int i = 0;
//        while (-1 != (i = input.read(buffer1))) {
//            output.write(buffer1, 0, i);
//        }
//        output.flush();
//
//        InputStream newIn = new ByteArrayInputStream(baos.toByteArray());
//		return newIn;
//	}

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
