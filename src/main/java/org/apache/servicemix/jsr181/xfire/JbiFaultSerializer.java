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
package org.apache.servicemix.jsr181.xfire;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.MessageSerializer;
import org.codehaus.xfire.exchange.OutMessage;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.util.jdom.StaxSerializer;
import org.jdom.Element;

public class JbiFaultSerializer implements MessageSerializer {

    public JbiFaultSerializer() {
    }
    
    public void readMessage(InMessage message, MessageContext context) throws XFireFault {
        throw new UnsupportedOperationException();
    }

    public void writeMessage(OutMessage message, XMLStreamWriter writer, MessageContext context) throws XFireFault {
        try {
            XFireFault fault = (XFireFault) message.getBody();
            if (fault.hasDetails()) {
                Element detail = fault.getDetail();
                StaxSerializer serializer = new StaxSerializer();
                List details = detail.getContent();
                for (int i = 0; i < details.size(); i++) {
                    serializer.writeElement((Element) details.get(i), writer);
                }
            } else {
                writer.writeStartElement("stack");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                fault.printStackTrace(pw);
                pw.close();
                writer.writeCData(sw.toString());
                writer.writeEndElement(); // stack
            }
        } catch (XMLStreamException e) {
            throw new XFireRuntimeException("Couldn't create fault.", e);
        }
    }

}
