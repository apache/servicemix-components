package org.servicemix.wsn.component;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import org.servicemix.common.Endpoint;
import org.servicemix.common.ExchangeProcessor;
import org.servicemix.jbi.jaxp.StringSource;

public class WSNEndpoint extends Endpoint implements ExchangeProcessor {

    protected ServiceEndpoint activated;
    protected String address;
    protected Object pojo;
    protected DeliveryChannel channel;
    protected JAXBContext jaxbContext;
    protected Class endpointInterface;
    
	public WSNEndpoint(String address, Object pojo) {
		this.address = address;
		this.pojo = pojo;
		String[] parts = split(address);
		service = new QName(parts[0], parts[1]);
		endpoint = parts[2];
	}

	@Override
	public Role getRole() {
		return Role.PROVIDER;
	}

	@Override
	public void activate() throws Exception {
        logger = this.serviceUnit.getComponent().getLogger();
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        activated = ctx.activateEndpoint(service, endpoint);
        channel = ctx.getDeliveryChannel();
        jaxbContext = createJAXBContext();
	}
	
	protected JAXBContext createJAXBContext() throws Exception {
		WebService ws = getWebServiceAnnotation();
		if (ws == null) {
			throw new IllegalStateException("Unable to find WebService annotation");
		}
		endpointInterface = Class.forName(ws.endpointInterface());
		List<Class> classes = new ArrayList<Class>();
		for (Method mth : endpointInterface.getMethods()) {
			WebMethod wm = (WebMethod) mth.getAnnotation(WebMethod.class);
			if (wm != null) {
				classes.add(mth.getReturnType());
				classes.addAll(Arrays.asList(mth.getParameterTypes()));
			}
		}
		return JAXBContext.newInstance(classes.toArray(new Class[0]));
	}

	@Override
	public void deactivate() throws Exception {
        ServiceEndpoint ep = activated;
        activated = null;
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        ctx.deactivateEndpoint(ep);
	}

	@Override
	public ExchangeProcessor getProcessor() {
		return this;
	}

	public void process(MessageExchange exchange) throws Exception {
		if (exchange.getStatus() == ExchangeStatus.DONE) {
			return;
		} else if (exchange.getStatus() == ExchangeStatus.ERROR) {
			exchange.setStatus(ExchangeStatus.DONE);
			channel.send(exchange);
			return;
		}
		Object input = jaxbContext.createUnmarshaller().unmarshal(exchange.getMessage("in").getContent());
		Method webMethod = null;
		for (Method mth : endpointInterface.getMethods()) {
			Class[] params = mth.getParameterTypes();
			if (params.length == 1 && params[0].isAssignableFrom(input.getClass())) {
				webMethod = mth;
				break;
			}
		}
		if (webMethod ==  null) {
			throw new IllegalStateException("Could not determine invoked web method");
		}
		Object output = webMethod.invoke(pojo, new Object[] { input });
		if (webMethod.getAnnotation(Oneway.class) != null) {
			exchange.setStatus(ExchangeStatus.DONE);
			channel.send(exchange);
		} else {
			NormalizedMessage msg = exchange.createMessage();
			exchange.setMessage(msg, "out");
			StringWriter writer = new StringWriter();
			jaxbContext.createMarshaller().marshal(output, writer);
			msg.setContent(new StringSource(writer.toString()));
			channel.send(exchange);
		}
	}
	
	protected Method getWebServiceMethod(QName interfaceName, QName operation) throws Exception {
		WebService ws = getWebServiceAnnotation();
		if (ws == null) {
			throw new IllegalStateException("Unable to find WebService annotation");
		}
		Class itf = Class.forName(ws.endpointInterface());
		for (Method mth : itf.getMethods()) {
			WebMethod wm = (WebMethod) mth.getAnnotation(WebMethod.class);
			if (wm != null) {
				
			}
		}
		return null;
	}

	protected WebService getWebServiceAnnotation() {
		for (Class cl = pojo.getClass(); cl != null; cl = cl.getSuperclass()) {
			WebService ws = (WebService) cl.getAnnotation(WebService.class);
			if (ws != null) {
				return ws;
			}
		}
		return null;
	}

	public void start() throws Exception {
		// Nothing to do
	}

	public void stop() throws Exception {
		// Nothing to do
	}

    protected String[] split(String uri) {
		char sep;
		if (uri.indexOf('/') > 0) {
			sep = '/';
		} else {
			sep = ':';
		}
		int idx1 = uri.lastIndexOf(sep);
		int idx2 = uri.lastIndexOf(sep, idx1 - 1);
		String epName = uri.substring(idx1 + 1);
		String svcName = uri.substring(idx2 + 1, idx1);
		String nsUri   = uri.substring(0, idx2);
    	return new String[] { nsUri, svcName, epName };
    }
}
