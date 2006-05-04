package org.apache.servicemix.eip.support;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.resolver.URIResolver;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.DocumentFragment;

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
    
    private String uri;

    /**
     * @return Returns the endpoint.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint
     *            The endpoint to set.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
     * @return Returns the service.
     */
    public QName getService() {
        return service;
    }

    /**
     * @param service
     *            The service to set.
     */
    public void setService(QName service) {
        this.service = service;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Configures the target on the newly created exchange 
     * @param exchange the exchange to configure
     * @throws MessagingException if the target could not be configured
     */
    public void configureTarget(MessageExchange exchange, ComponentContext context) throws MessagingException {
        if (_interface == null && service == null && uri == null) {
            throw new MessagingException("interface, service or uri should be specified");
        }
        if (uri != null) {
            if (uri.startsWith("interface:")) {
                String uri = this.uri.substring(10);
                String[] parts = URIResolver.split2(uri);
                exchange.setInterfaceName(new QName(parts[0], parts[1]));
            } else if (uri.startsWith("operation:")) {
                String uri = this.uri.substring(10);
                String[] parts = URIResolver.split3(uri);
                exchange.setInterfaceName(new QName(parts[0], parts[1]));
                exchange.setOperation(new QName(parts[0], parts[2]));
            } else if (uri.startsWith("service:")) {
                String uri = this.uri.substring(8);
                String[] parts = URIResolver.split2(uri);
                exchange.setService(new QName(parts[0], parts[1]));
            } else if (uri.startsWith("endpoint:")) {
                String uri = this.uri.substring(9);
                String[] parts = URIResolver.split3(uri);
                ServiceEndpoint se = context.getEndpoint(new QName(parts[0], parts[1]), parts[2]);
                exchange.setEndpoint(se);
            } else {
                DocumentFragment epr = URIResolver.createWSAEPR(uri);
                ServiceEndpoint se = context.resolveEndpointReference(epr);
                exchange.setEndpoint(se);
            }
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
        if (_interface == null && service == null && uri == null) {
            throw new MessagingException("interface, service or uri should be specified");
        }
    }

}
