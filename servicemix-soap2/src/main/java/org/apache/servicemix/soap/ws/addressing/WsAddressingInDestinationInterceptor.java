package org.apache.servicemix.soap.ws.addressing;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.messaging.MessageExchange;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.interceptors.jbi.JbiInInterceptor;

public class WsAddressingInDestinationInterceptor extends AbstractWsAddressingInterceptor {

    public WsAddressingInDestinationInterceptor(WsAddressingPolicy policy, boolean server) {
        super(policy, server);
        addAfter(JbiInInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        ServiceEndpoint se = message.get(ServiceEndpoint.class);
        MessageExchange me = message.getContent(MessageExchange.class);
        if (se != null && me != null) {
            me.setEndpoint(se);
        }
    }

    public Collection<URI> getRoles() {
        return Collections.emptyList();
    }

    public Collection<QName> getUnderstoodHeaders() {
        return Collections.emptyList();
    }
}
