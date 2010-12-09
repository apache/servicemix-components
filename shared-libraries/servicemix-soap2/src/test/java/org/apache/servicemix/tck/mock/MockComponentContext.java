package org.apache.servicemix.tck.mock;

import java.util.logging.Logger;
import java.util.MissingResourceException;

import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.JBIException;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;
import javax.management.MBeanServer;
import javax.naming.InitialContext;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Document;

public class MockComponentContext implements ComponentContext {

    public ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getComponentName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DeliveryChannel getDeliveryChannel() throws MessagingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint getEndpoint(QName service, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getEndpoints(QName interfaceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getInstallRoot() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MBeanNames getMBeanNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MBeanServer getMBeanServer() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InitialContext getNamingContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getTransactionManager() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getWorkspaceRoot() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
