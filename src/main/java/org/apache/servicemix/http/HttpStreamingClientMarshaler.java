/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.http;

import java.io.IOException;
import java.io.OutputStream;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

/**
 * A class which marshalls a client HTTP request to a NMS message
 *
 * @version $Revision$
 */
public class HttpStreamingClientMarshaler extends HttpClientMarshaler {

    public void toNMS(NormalizedMessage normalizedMessage, HttpMethod method) throws Exception {
        addNmsProperties(normalizedMessage, method);

        normalizedMessage.setContent(new StreamSource(method.getResponseBodyAsStream()));
    }

    public void fromNMS(PostMethod method, MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception, TransformerException {
        addHttpHeaders(method, exchange);
        method.setRequestEntity(new StreamingRequestEntity(normalizedMessage.getContent()));
    }
    
    public class StreamingRequestEntity implements RequestEntity {

        private Source source;
        
        public StreamingRequestEntity(Source source) {
            this.source = source;
        }
        
        public boolean isRepeatable() {
            return false;
        }

        public void writeRequest(OutputStream out) throws IOException {
            try {
                StreamResult result = new StreamResult(out);
                sourceTransformer.toResult(source, result);
            } catch (Exception e) {
                throw (IOException) new IOException("Could not write request").initCause(e);
            }
        }

        public long getContentLength() {
            // not known so we send negative value
            return -1;
        }

        public String getContentType() {
            return "text/xml;";
        }
        
    }

}
