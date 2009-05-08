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
package org.apache.servicemix.exec.marshaler;

import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.exec.utils.ExecutionData;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Default exec marshaler.
 * 
 * @author jbonofre
 */
public class DefaultExecMarshaler implements ExecMarshalerSupport {
    
    public static final String TAG_COMMAND = "command";
    public static final String TAG_ARGUMENT = "argument";
    public static final String TAG_RESULT = "result";
    public static final String TAG_EXITCODE = "exitcode";
    public static final String TAG_OUTPUT = "output";
    public static final String TAG_ERROR = "error";
    public static final String TAG_START_TIME = "started";
    public static final String TAG_END_TIME = "finished";
    public static final String TAG_DURATION = "duration";
    
    public static final String RESULT_FORMAT = "<%s><%s>%d</%s><%s>%d</%s><%s>%d</%s><%s>%d</%s><%s><![CDATA[%s]]></%s><%s><![CDATA[%s]]></%s></%s>";
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.exec.marshaler.ExecMarshalerSupport#constructExecCommand(javax.jbi.messaging.NormalizedMessage)
     */
    public String constructExecCommand(NormalizedMessage message) throws TransformerException {
        String execString = null;
        // create a source transformer
        SourceTransformer transformer = new SourceTransformer();
        try {
            // transform the message content into a DOM document
            Document document = transformer.toDOMDocument(message);
            document.getDocumentElement().normalize();
            
            // get the command node
            NodeList commandNode = document.getElementsByTagName(TAG_COMMAND);
            if (commandNode != null && commandNode.getLength() > 1) {
                throw new TransformerException("Invalid message content. Only one command tag is supported.");
            }
            if (commandNode != null && commandNode.item(0) != null) {
                execString = commandNode.item(0).getChildNodes().item(0).getNodeValue();
            }
            
            // get the argument nodes
            NodeList argumentNodes = document.getElementsByTagName(TAG_ARGUMENT);
            for (int i = 0; i < argumentNodes.getLength(); i++) {
                execString = execString + " " + argumentNodes.item(i).getChildNodes().item(0).getNodeValue();
            }
            
        } catch (Exception e) {
            throw new TransformerException(e);
        }
        return execString;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.exec.marshaler.ExecMarshalerSupport#formatExecutionResult(org.apache.servicemix.exec.utils.ExecutionData)
     */
    public String formatExecutionResult(ExecutionData executionData) {
    	String result = String.format(RESULT_FORMAT, 
				TAG_RESULT,
				TAG_START_TIME,
				executionData.getStartTime(),
				TAG_START_TIME,
				TAG_END_TIME,
				executionData.getEndTime(),
				TAG_END_TIME,
				TAG_DURATION,
				executionData.getExecutionDuration(),
				TAG_DURATION,
				TAG_EXITCODE,
				executionData.getExitCode(),
				TAG_EXITCODE,
				TAG_OUTPUT,
				executionData.getOutputData() != null ? executionData.getOutputData().toString() : "",
				TAG_OUTPUT,
				TAG_ERROR,
				executionData.getErrorData() != null ? executionData.getErrorData().toString() : "",
				TAG_ERROR,
				TAG_RESULT
				);

    	return result;
    }
}
