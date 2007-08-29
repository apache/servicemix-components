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
package org.apache.servicemix.cxfbc.ws.addressing;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.support.ServiceDelegateAccessor;
import org.apache.cxf.ws.addressing.AddressingBuilder;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.servicemix.cxfbc.interceptors.JbiConstants;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;

public class CxfBcAddressingTest extends SpringTestSupport implements VerificationCache {
    
    static final String INBOUND_KEY = "inbound";
    static final String OUTBOUND_KEY = "outbound";
    static final String ADDRESS = "http://localhost:9008/SoapContext/SoapPort";
    static final QName CUSTOMER_NAME =
        new QName("http://example.org/customer", "CustomerKey", "customer");
    static final String CUSTOMER_KEY = "Key#123456789";
    private static final Logger LOG = Logger
            .getLogger(CxfBcAddressingTest.class.getName());

    private static Bus staticBus;

    
    private static MAPVerifier mapVerifier;
    private static HeaderVerifier headerVerifier;

    private static final QName SERVICE_NAME = 
        new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");
    
    private static final java.net.URL WSDL_LOC;
    private static final String CONFIG;
    
    
    private static Map<Object, Map<String, String>> messageIDs =
        new HashMap<Object, Map<String, String>>();
    protected Greeter greeter;
    private String verified;
    
    

    
    static {
        CONFIG = "org/apache/servicemix/cxfbc/ws/addressing/addressing" 
            + (("HP-UX".equals(System.getProperty("os.name"))
                || "Windows XP".equals(System.getProperty("os.name"))) ? "-hpux" : "")
            + ".xml";
        
        java.net.URL tmp = null;
        try {
            tmp = CxfBcAddressingTest.class.getClassLoader().getResource(
                "org/apache/servicemix/cxfbc/ws/addressing/hello_world.wsdl"
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        WSDL_LOC = tmp;
    }
    
    public void setUp() throws Exception {
        //super.setUp();
        if (context != null) {
            context.refresh();
        }
        transformer = new SourceTransformer();
        context = createBeanFactory();
                        
        jbi = (SpringJBIContainer) context.getBean("jbi");
        assertNotNull("JBI Container not found in spring!", jbi);
        LOG.info("setUp is invoked");
        if (staticBus == null) {
            SpringBusFactory bf = new SpringBusFactory();
            staticBus = bf.createBus(getConfigFileName());
        }
        BusFactory.setDefaultBus(staticBus);       
        messageIDs.clear();
        mapVerifier = new MAPVerifier();
        headerVerifier = new HeaderVerifier();
        Interceptor[] interceptors = {mapVerifier, headerVerifier };
        addInterceptors(staticBus.getInInterceptors(), interceptors);
        addInterceptors(staticBus.getOutInterceptors(), interceptors);
        addInterceptors(staticBus.getOutFaultInterceptors(), interceptors);
        addInterceptors(staticBus.getInFaultInterceptors(), interceptors);
        
        EndpointReferenceType target = 
            EndpointReferenceUtils.getEndpointReference(ADDRESS);
        ReferenceParametersType params = 
            ContextUtils.WSA_OBJECT_FACTORY.createReferenceParametersType();
        JAXBElement<String> param =
             new JAXBElement<String>(CUSTOMER_NAME, String.class, CUSTOMER_KEY);
        params.getAny().add(param);
        target.setReferenceParameters(params);
        
        ServiceImpl serviceImpl = 
            ServiceDelegateAccessor.get(new SOAPService(WSDL_LOC, SERVICE_NAME));
        greeter = serviceImpl.getPort(target, Greeter.class);

        mapVerifier.verificationCache = this;
        headerVerifier.verificationCache = this;
    }
    
    public void tearDown() throws Exception {
        Interceptor[] interceptors = {mapVerifier, headerVerifier };
        removeInterceptors(staticBus.getInInterceptors(), interceptors);
        removeInterceptors(staticBus.getOutInterceptors(), interceptors);
        removeInterceptors(staticBus.getOutFaultInterceptors(), interceptors);
        removeInterceptors(staticBus.getInFaultInterceptors(), interceptors);
        mapVerifier = null;
        headerVerifier = null;
        verified = null;
        messageIDs.clear();
        if (context != null) {
            context.destroy();
            context = null;
        }
        if (jbi != null) {
            jbi.shutDown();
            jbi.destroy();
            jbi = null;
        }
        BusFactory.setDefaultBus(null);
        super.tearDown();
    }
    
    
    public void testImplicitMAPs() throws Exception {
        try {
            String greeting = greeter.greetMe("implicit1");
            assertEquals("unexpected response received from service", 
                         "Hello implicit1",
                         greeting);
            checkVerification();
            greeting = greeter.greetMe("implicit2");
            assertEquals("unexpected response received from service", 
                         "Hello implicit2",
                         greeting);
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    public void testExplicitMAPsAddressTo() throws Exception {
        AddressingBuilder builder = AddressingBuilder.getAddressingBuilder();
        AddressingProperties maps = builder.newAddressingProperties();
        AttributedURIType to = new AttributedURIType();
        String delimiter = "/";
        String ns = "http://apache.org/hello_world_soap_http";
        String serviceName = "SOAPServiceWSSecurity";
        String endpointName = "TimestampSignEncrypt";
        
        to.setValue(ns + delimiter + serviceName + delimiter + endpointName + delimiter + JbiConstants.JBI_SUFFIX);
        EndpointReferenceType toRef = new EndpointReferenceType();
        toRef.setAddress(to);
        maps.setTo(toRef);
        //associate MAPs with request context
        Map<String, Object> requestContext =
            ((BindingProvider)greeter).getRequestContext();
        requestContext.put(CLIENT_ADDRESSING_PROPERTIES, maps);
        String greeting = greeter.greetMe("explicitly set to invoke antoher endpoint in another service");
        //dispatch operation to to greetMeSomeTime method
        assertEquals(greeting, 
                "Another Greeter Hello explicitly set to invoke antoher endpoint in another service");
        //disassociate MAPs from request context
        requestContext.remove(CLIENT_ADDRESSING_PROPERTIES);
    }
    
    public void testExplicitMAPs() throws Exception {
        try {
            AddressingBuilder builder = AddressingBuilder.getAddressingBuilder();
            AddressingProperties maps = builder.newAddressingProperties();
            AttributedURIType action = new AttributedURIType();
            String delimiter = "/";
            String ns = "http://apache.org/hello_world_soap_http";
            String interfaceName = "Greeter";
            String operationName = "greetMeSometime";
            action.setValue(ns + delimiter + interfaceName + delimiter + operationName + delimiter + JbiConstants.JBI_SUFFIX);
            maps.setAction(action);
            //associate MAPs with request context
            Map<String, Object> requestContext =
                ((BindingProvider)greeter).getRequestContext();
            requestContext.put(CLIENT_ADDRESSING_PROPERTIES, maps);
            String greeting = greeter.greetMe("explicitly set ot invoke greetMeSomeTime");
            //dispatch operation to to greetMeSomeTime method
            assertEquals(greeting, "greetMeSomeTime:How are you explicitly set ot invoke greetMeSomeTime");
            checkVerification();
            operationName = "sayHi";
            action.setValue(ns + delimiter + interfaceName + delimiter + operationName 
                    + delimiter + JbiConstants.JBI_SUFFIX);
            greeting = greeter.greetMe("explicitly set to invoke sayHi");
            assertEquals(greeting, "sayHi:Bonjour");
            checkVerification();
            //disassociate MAPs from request context
            requestContext.remove(CLIENT_ADDRESSING_PROPERTIES);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    public void testOneway() throws Exception {
        try {
            greeter.greetMeOneWay("implicit_oneway1");
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    public void testApplicationFault() throws Exception {
        try {
            greeter.testDocLitFault("BadRecordLitFault");
            fail("expected fault from service");
        } catch (BadRecordLitFault brlf) {
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        String greeting = greeter.greetMe("intra-fault");
        assertEquals("unexpected response received from service", 
                     "Hello intra-fault",
                     greeting);
        try {
            greeter.testDocLitFault("NoSuchCodeLitFault");
            fail("expected NoSuchCodeLitFault");
        } catch (NoSuchCodeLitFault nsclf) {
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    

    public void testVersioning() throws Exception {
        try {
            // expect two MAPs instances versioned with 200408, i.e. for both 
            // the partial and full responses
            mapVerifier.expectedExposedAs.add(VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
            mapVerifier.expectedExposedAs.add(VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
            String greeting = greeter.greetMe("versioning1");
            assertEquals("unexpected response received from service", 
                         "Hello versioning1",
                         greeting);
            checkVerification();
            greeting = greeter.greetMe("versioning2");
            assertEquals("unexpected response received from service", 
                         "Hello versioning2",
                         greeting);
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }


    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/ws/addressing/xbean.xml");
    }

    protected static String verifyMAPs(AddressingProperties maps,
            Object checkPoint) {
        if (maps == null) {
            return "expected MAPs";
        }
        String id = maps.getMessageID().getValue();
        if (id == null) {
            return "expected MessageID MAP";
        }
        if (!id.startsWith("urn:uuid")) {
            return "bad URN format in MessageID MAP: " + id;
        }
        // ensure MessageID is unique for this check point
        Map<String, String> checkPointMessageIDs = messageIDs.get(checkPoint);
        if (checkPointMessageIDs != null) {
            if (checkPointMessageIDs.containsKey(id)) {
                // return "MessageID MAP duplicate: " + id;
                return null;
            }
        } else {
            checkPointMessageIDs = new HashMap<String, String>();
            messageIDs.put(checkPoint, checkPointMessageIDs);
        }
        checkPointMessageIDs.put(id, id);
        // To
        if (maps.getTo() == null) {
            return "expected To MAP";
        }
        return null;
    }
    
    public String getConfigFileName() {
        return CONFIG;
    }
    
    public static void shutdownBus() throws Exception {
        staticBus.shutdown(true);
    }
    
    private void addInterceptors(List<Interceptor> chain,
                                     Interceptor[] interceptors) {
        for (int i = 0; i < interceptors.length; i++) {
            chain.add(interceptors[i]);
        }
    }
    private void removeInterceptors(List<Interceptor> chain,
                                 Interceptor[] interceptors) {
        for (int i = 0; i < interceptors.length; i++) {
            chain.add(interceptors[i]);
        }
    }

    public void put(String verification) {
        if (verification != null) {
            verified = verified == null
                       ? verification
                : verified + "; " + verification;
        }
    }
    
    /**
     * Verify presence of expected MAP headers.
     *
     * @param wsaHeaders a list of the wsa:* headers present in the SOAP
     * message
     * @param parial true if partial response
     * @return null if all expected headers present, otherwise an error string.
     */
    protected static String verifyHeaders(List<String> wsaHeaders,
                                          boolean partial,
                                          boolean requestLeg) {
        
        String ret = null;
        if (!wsaHeaders.contains(Names.WSA_MESSAGEID_NAME)) {
            ret = "expected MessageID header"; 
        }
        if (!wsaHeaders.contains(Names.WSA_TO_NAME)) {
            ret = "expected To header";
        }
       
        if (!(wsaHeaders.contains(Names.WSA_REPLYTO_NAME)
              || wsaHeaders.contains(Names.WSA_RELATESTO_NAME))) {
            ret = "expected ReplyTo or RelatesTo header";
        }
        if (partial) { 
            if (!wsaHeaders.contains(Names.WSA_FROM_NAME)) {
                //ret = "expected From header";
            }
        } else {
            // REVISIT Action missing from full response
            //if (!wsaHeaders.contains(Names.WSA_ACTION_NAME)) {
            //    ret = "expected Action header";
            //}            
        }
        if (requestLeg && !(wsaHeaders.contains(CUSTOMER_NAME.getLocalPart()))) {
            ret = "expected CustomerKey header";
        }
        return ret;
    }

    private void checkVerification() {
        assertNull("MAP/Header verification failed: " + verified, verified);
    }

    
}
