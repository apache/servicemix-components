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
package org.apache.servicemix.exec.utils;

import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This utility class parses the content of a message coming from the NMR
 * and constructs the execution command.
 * 
 * @author jbonofre
 */
public class ExecMarshaler {

    private static final String TAG_COMMAND = "command";
    private static final String TAG_ARGUMENT = "argument";
    
    /**
     * <p>
     * Parses the in normalized message, extracts command and arguments from the content
     * and constructs the execution command.
     * </p>
     * 
     * @param message the in <code>NormalizedMessage</code>.
     * @return the execution command.
     */
    public static final String constructExecCommand(NormalizedMessage message) throws TransformerException {
        String execString = null;
        // create a source transformer
        SourceTransformer transformer = new SourceTransformer();
        try {
            Document document = transformer.toDOMDocument(message);
            document.getDocumentElement().normalize();
            
            NodeList commandNode = document.getElementsByTagName(TAG_COMMAND);
            if (commandNode != null && commandNode.getLength() > 1) {
                throw new TransformerException("Invalid message content. Only one command tag is supported.");
            }
            if (commandNode != null && commandNode.item(0) != null) {
                execString = commandNode.item(0).getChildNodes().item(0).getNodeValue();
            }
            
            NodeList argumentNodes = document.getElementsByTagName(TAG_ARGUMENT);
            for (int i = 0; i < argumentNodes.getLength(); i++) {
                execString = execString + " " + argumentNodes.item(i).getChildNodes().item(0).getNodeValue();
            }
            
        } catch (Exception e) {
            throw new TransformerException(e);
        }
        return execString;
    }
    
    
}
