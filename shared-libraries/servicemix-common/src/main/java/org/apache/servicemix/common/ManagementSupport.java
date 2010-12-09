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
package org.apache.servicemix.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jbi.management.DeploymentException;

/**
 * ManagementMessageHelper is a class that ease the building of management messages. 
 */
public class ManagementSupport {
    
    private static final Log logger = LogFactory.getLog(ManagementSupport.class);

    public static DeploymentException failure(String task, String component, String info, Throwable e) {
        Message msg = new Message();
        msg.setComponent(component);
        msg.setTask(task);
        msg.setResult("FAILED");
        msg.setType("ERROR");
        if (info != null) {
            msg.setMessage(info);
        } else if (e != null) {
            msg.setMessage(e.toString());
        }
        msg.setException(e);
        return new DeploymentException(createComponentMessage(msg));
    }

     public static String createComponentMessage(Message msg) {
        try {
            StringBuffer sw = new StringBuffer();
            // component-task-result
            sw.append("<component-task-result ");
            sw.append("xmlns=\"http://java.sun.com/xml/ns/jbi/management-message\">");
            sw.append("\n\t");
            // component-name
            sw.append("<component-name>");
            sw.append(msg.getComponent());
            sw.append("</component-name>");
            // component-task-result-details
            sw.append("\n\t");
            sw.append("<component-task-result-details>");
            // task-result-details
            sw.append("\n\t\t");
            sw.append("<task-result-details>");
            // task-id
            sw.append("\n\t\t\t");
            sw.append("<task-id>");
            sw.append(msg.getTask());
            sw.append("</task-id>");
            // task-result
            sw.append("\n\t\t\t");
            sw.append("<task-result>");
            sw.append(msg.getResult());
            sw.append("</task-result>");
            // message-type
            if (msg.getType() != null) {
                sw.append("\n\t\t\t");
                sw.append("<message-type>");
                sw.append(msg.getType());
                sw.append("</message-type>");
            }
            // task-status-message
            if (msg.getMessage() != null) {
                sw.append("\n\t\t\t");
                sw.append("<task-status-msg>");
                sw.append("<msg-loc-info>");
                sw.append("<loc-token/>");
                sw.append("<loc-message>");
                sw.append(msg.getMessage());
                sw.append("</loc-message>");
                sw.append("</msg-loc-info>");
                sw.append("</task-status-msg>");
            }
            // exception-info
            if (msg.getException() != null) {
                sw.append("\n\t\t\t");
                sw.append("<exception-info>");
                sw.append("\n\t\t\t\t");
                sw.append("<nesting-level>1</nesting-level>");
                sw.append("\n\t\t\t\t");
                sw.append("<msg-loc-info>");
                sw.append("\n\t\t\t\t\t");
                sw.append("<loc-token />");
                sw.append("\n\t\t\t\t\t");
                sw.append("<loc-message>");
                sw.append(msg.getException().getMessage());
                sw.append("</loc-message>");
                sw.append("\n\t\t\t\t\t");
                sw.append("<stack-trace>");
                StringWriter sw2 = new StringWriter();
                PrintWriter pw = new PrintWriter(sw2);
                msg.getException().printStackTrace(pw);
                pw.close();
                sw.append("<![CDATA[");
                sw.append(sw2.toString());
                sw.append("]]>");
                sw.append("</stack-trace>");
                sw.append("\n\t\t\t\t");
                sw.append("</msg-loc-info>");
                sw.append("\n\t\t\t");
                sw.append("</exception-info>");
            }
            // end: task-result-details
            sw.append("\n\t\t");
            sw.append("</task-result-details>");
            // end: component-task-result-details
            sw.append("\n\t");
            sw.append("</component-task-result-details>");
            // end: component-task-result
            sw.append("\n");
            sw.append("</component-task-result>");
            // return result
            return sw.toString();
        } catch (Exception e) {
            logger.warn("Error generating component management message", e);
            return null;
        }
    }
    
     public static class Message {
         private String task;
         private String component;
         private String result;
         private Throwable exception;
         private String type;
         private String message;
         
         public String getComponent() {
             return component;
         }
         public void setComponent(String component) {
             this.component = component;
         }
         public Throwable getException() {
             return exception;
         }
         public void setException(Throwable exception) {
             this.exception = exception;
         }
         public String getResult() {
             return result;
         }
         public void setResult(String result) {
             this.result = result;
         }
         public String getTask() {
             return task;
         }
         public void setTask(String task) {
             this.task = task;
         }
         public String getType() {
             return type;
         }
         public void setType(String type) {
             this.type = type;
         }
         public String getMessage() {
             return message;
         }
         public void setMessage(String message) {
             this.message = message;
         }
     }
     
}
