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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * A marshaler that handles Java serialized content from the InputStream of the HttpServletRequest object and to the
 * OutputStream of the HttpServletResponse object. This class is intended to handle requests initiated by the Spring <a
 * href="http://www.springframework.org/docs/api/org/springframework/remoting/httpinvoker/package-summary.html">httpinvoker
 * package</a> so the marshaled/unmarshaled XML invocation will be Spring <a
 * href="http://www.springframework.org/docs/api/org/springframework/remoting/support/RemoteInvocation.html">RemoteInvocation</a>/
 * <a href="http://www.springframework.org/docs/api/org/springframework/remoting/support/RemoteInvocationResult.html">
 * RemoteInvocationResult</a> objects respectively.
 * 
 * <p>
 * This class makes no assumptions about how XML should be marshaled/unmarshaled. I.e., there is currently no way to
 * customize the marshaled XML invocation. So this marshaler will need to pass the XML to a component that can transform
 * it into some custom XML. The servicemix-saxon component can handle this very easily via XLST.
 * 
 * @author bsnyder, aco
 * @org.apache.xbean.XBean element="serializedMarshaler"
 */
public class SerializedMarshaler extends DefaultHttpConsumerMarshaler {

    private static Log log = LogFactory.getLog(SerializedMarshaler.class);

    public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
        MessageExchange me = context.getDeliveryChannel().createExchangeFactory().createExchange(getDefaultMep());
        NormalizedMessage in = me.createMessage();
        in.setContent(marshal(request.getInputStream()));
        me.setMessage(in, "in");
        return me;
    }

    public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request,
                    HttpServletResponse response) throws Exception {
        if (outMsg.getContent() != null) {
            unmarshal(response.getOutputStream(), outMsg.getContent());
        }
    }

    /**
     * Marshal the byte content of the input stream to an XML source. This method is marshaling the contents of the
     * Spring <a
     * href="http://www.springframework.org/docs/api/org/springframework/remoting/support/RemoteInvocation.html">RemoteInvocation</a>
     * object. Below is an example of what this method emits:
     * 
     * <pre>
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&lt;org.springframework.remoting.support.RemoteInvocation&gt;
     *   &lt;methodName&gt;login&lt;/methodName&gt;
     *     &lt;parameterTypes&gt;
     *       &lt;java-class&gt;java.lang.String&lt;/java-class&gt;
     *       &lt;java-class&gt;java.lang.String&lt;/java-class&gt;
     *     &lt;/parameterTypes&gt;
     *     &lt;arguments&gt;
     *       &lt;string&gt;foo&lt;/string&gt;
     *       &lt;string&gt;bar&lt;/string&gt;
     *     &lt;/arguments&gt;
     *   &lt;/org.springframework.remoting.support.RemoteInvocation&gt;
     * </pre>
     * 
     * @param is -
     *            input stream to read the object from
     * @return xml source
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Source marshal(InputStream is) throws IOException, ClassNotFoundException {
        Object obj = new ObjectInputStream(is).readObject();
        Writer w = new StringWriter();
        XStream xstream = new XStream(new DomDriver());
        xstream.toXML(obj, w);
        String request = w.toString();

        if (log.isDebugEnabled()) {
            log.debug("Remote invocation request: " + request);
        }

        return new StringSource(request);
    }

    /**
     * Unmarshal the XML content to the specified output stream. This method is unmarshaling XML into the Spring 
     * <a href="http://www.springframework.org/docs/api/org/springframework/remoting/support/RemoteInvocationResult.html">
     * RemoteInvocationResult</a> object. Below is an example of the XML expected by this method:
     * 
     * <pre>
     * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
     * &lt;org.springframework.remoting.support.RemoteInvocationResult&gt;
     *   &lt;value class=&quot;com.example.foo.bar.Baz&quot;&gt;
     *     &lt;firstName&gt;myfirstname&lt;/firstName&gt;
     *     &lt;lastName&gt;mylastname&lt;/lastName&gt;
     *     &lt;phone&gt;12312312&lt;/phone&gt;
     *   &lt;/value&gt;
     * &lt;/org.springframework.remoting.support.RemoteInvocationResult&gt;
     * </pre>
     * 
     * @param os -
     *            output stream to unmarshal to
     * @param content -
     *            XML source
     * @throws TransformerException
     * @throws IOException
     */
    private void unmarshal(OutputStream os, Source content) throws TransformerException, IOException {
        SourceTransformer transform = new SourceTransformer();
        XStream xstream = new XStream(new DomDriver());
        String result = transform.toString(content);

        if (log.isDebugEnabled()) {
            log.debug("Remote invocation result: " + result);
        }

        Object obj = xstream.fromXML(result);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(obj);
    }
}
