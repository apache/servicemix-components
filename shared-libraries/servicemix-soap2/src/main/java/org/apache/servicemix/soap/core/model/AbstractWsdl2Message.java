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
package org.apache.servicemix.soap.core.model;

import org.apache.servicemix.soap.api.model.wsdl2.Wsdl2Message;
import org.apache.ws.commons.schema.XmlSchemaElement;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class AbstractWsdl2Message extends AbstractMessage implements Wsdl2Message {

    private ContentModel contentModel;
    private XmlSchemaElement elementDeclaration;
    
    /**
     * @return the contentModel
     */
    public ContentModel getContentModel() {
        return contentModel;
    }
    /**
     * @param contentModel the contentModel to set
     */
    public void setContentModel(ContentModel contentModel) {
        this.contentModel = contentModel;
    }
    /**
     * @return the elementDeclaration
     */
    public XmlSchemaElement getElementDeclaration() {
        return elementDeclaration;
    }
    /**
     * @param elementDeclaration the elementDeclaration to set
     */
    public void setElementDeclaration(XmlSchemaElement elementDeclaration) {
        this.elementDeclaration = elementDeclaration;
    }
    
}
