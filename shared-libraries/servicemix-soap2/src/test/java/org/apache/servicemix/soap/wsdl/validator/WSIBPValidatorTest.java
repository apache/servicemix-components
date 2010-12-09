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
package org.apache.servicemix.soap.wsdl.validator;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WSIBPValidatorTest extends TestCase {
    private static transient Log log = LogFactory.getLog(WSIBPValidatorTest.class);

    public void testR2303() throws Exception {
        Definition def = WSDLFactory.newInstance().newDefinition();
        def.setTargetNamespace("urn:test");
        PortType pt = def.createPortType();
        pt.setQName(new QName("urn:test", "porttype"));
        Operation op = def.createOperation();
        op.setName("operation");
        def.addPortType(pt);
        pt.addOperation(op);
        
        WSIBPValidator validator = new WSIBPValidator(def);
        assertFalse(validator.isValid());
        
        for (String err : validator.getErrors()) {
            log.info(err);
        }
    }
    
}
