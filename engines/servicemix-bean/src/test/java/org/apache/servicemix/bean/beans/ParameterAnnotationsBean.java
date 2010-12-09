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
package org.apache.servicemix.bean.beans;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.bean.Property;
import org.apache.servicemix.bean.XPath;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

/**
 * Example bean to show method parameter mapping with and without annotations
 */
public class ParameterAnnotationsBean {
    
    private final SourceTransformer transformer = new SourceTransformer();
    
    /**
     * The handle method shows the types that will be automatically recognized
     * without having to annotate the parameters 
     * 
     * @throws TransformerException 
     */
    public void handle(MessageExchange exchange,
                       NormalizedMessage message, 
                       Source source) throws TransformerException {
        System.out.println(String.format("Received exchange %s with content %s", exchange.toString(), transformer.toString(source)));
    }
    
    /**
     * The annotations methods shows some of the options for annotating parameters
     * 
     * @param value uses the {@link Property} annotation to extract a message property
     * @param type use the {@link XPath} annotation to extract data from the message content using XPath
     */
    public void annotations(@Property(name = "key") Object value,
                            @XPath(prefix = "my", uri = "urn:test", xpath = "/my:message/@type") String type) {
        System.out.println(String.format("Type %s has key value %s", type, value));
    }
}
