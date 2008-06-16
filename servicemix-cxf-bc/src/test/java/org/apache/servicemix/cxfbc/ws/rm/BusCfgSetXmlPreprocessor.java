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
package org.apache.servicemix.cxfbc.ws.rm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.xbean.spring.context.SpringApplicationContext;
import org.apache.xbean.spring.context.SpringXmlPreprocessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;


public class BusCfgSetXmlPreprocessor implements SpringXmlPreprocessor {
    
    private String busCfg;
    
    public BusCfgSetXmlPreprocessor(String busCfg) {
        this.busCfg = busCfg;
    }

    public void preprocess(SpringApplicationContext applicationContext,
            XmlBeanDefinitionReader reader, Document document) {
        NodeList consumerList = document.getDocumentElement().getElementsByTagNameNS(
                "http://servicemix.apache.org/cxfbc/1.0", "consumer");
        for (int i = 0; i < consumerList.getLength(); i++) {
            String busAttr = ((Element)consumerList.item(i)).getAttribute("busCfg");
            if (busAttr != null && busAttr.length() > 0) {
                ((Element)consumerList.item(i)).setAttribute("busCfg", busCfg);
            }
        }

    }

}
