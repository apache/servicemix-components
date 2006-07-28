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
package org.apache.servicemix.wsn.spring;

import javax.xml.parsers.DocumentBuilderFactory;

import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.springframework.beans.factory.FactoryBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="create-pull-point"
 */
public class CreatePullPointFactoryBean implements FactoryBean {

    private String address;
    
    /**
     * @return Returns the address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address The address to set.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    public Object getObject() throws Exception {
        CreatePullPoint createPullPoint = new CreatePullPoint();
        if (address != null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();
            Element el = doc.createElementNS("http://servicemix.apache.org/wsn2005/1.0", "address");
            Text txt = doc.createTextNode(address);
            el.appendChild(txt);
            doc.appendChild(el);
            createPullPoint.getAny().add(el);
        }
        return createPullPoint;
    }

    public Class getObjectType() {
        return CreatePullPoint.class;
    }

    public boolean isSingleton() {
        return false;
    }

}
