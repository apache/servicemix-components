package org.apache.servicemix.eip.support;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.InitializingBean;

/**
 * An ExchangeTarget may be used to specify the target of an exchange,
 * while retaining all the JBI features (interface based routing, service
 * name based routing or endpoint routing).
 *   
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="exchange-target"
 */
public class ExchangeTarget implements InitializingBean {

    private QName _interface;

    private QName operation;

    private QName service;

    private String endpoint;

    /**
     * @return Returns the endpointName.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpointName
     *            The endpointName to set.
     */
    public void setEndpoint(String endpointName) {
        this.endpoint = endpointName;
    }

    /**
     * @return Returns the interface name.
     */
    public QName getInterface() {
        return _interface;
    }

    /**
     * @param interface name
     *            The interface name to set.
     */
    public void setInterface(QName _interface) {
        this._interface = _interface;
    }

    /**
     * @return Returns the operation name.
     */
    public QName getOperation() {
        return operation;
    }

    /**
     * @param operation
     *            The operation to set.
     */
    public void setOperation(QName operation) {
        this.operation = operation;
    }

    /**
     * @return Returns the serviceName.
     */
    public QName getService() {
        return service;
    }

    /**
     * @param serviceName
     *            The serviceName to set.
     */
    public void setService(QName serviceName) {
        this.service = serviceName;
    }

    /**
     * Configures the target on the newly created exchange 
     * @param exchange the exchange to configure
     * @throws MessagingException if the target could not be configured
     */
    public void configureTarget(MessageExchange exchange, ComponentContext context) throws MessagingException {
        if (_interface == null && service == null) {
            throw new MessagingException("interfaceName or serviceName should be specified");
        }
        if (_interface != null) {
            exchange.setInterfaceName(_interface);
        }
        if (operation != null) {
            exchange.setOperation(operation);
        }
        if (service != null) {
            exchange.setService(service);
            if (endpoint != null) {
                ServiceEndpoint se = context.getEndpoint(service, endpoint);
                exchange.setEndpoint(se);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (_interface == null && service == null) {
            throw new MessagingException("interfaceName or serviceName should be specified");
        }
    }

}
