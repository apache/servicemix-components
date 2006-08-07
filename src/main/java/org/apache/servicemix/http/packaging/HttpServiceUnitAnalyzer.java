package org.apache.servicemix.http.packaging;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.xbean.AbstractXBeanServiceUnitAnalzyer;
import org.apache.servicemix.http.HttpEndpoint;

public class HttpServiceUnitAnalyzer extends AbstractXBeanServiceUnitAnalzyer {

	protected List getConsumes(Endpoint endpoint) {
		return new ArrayList();
	}

	protected String getXBeanFile() {
		return "xbean.xml";
	}

	protected boolean isValidEndpoint(Object bean) {
		if (bean instanceof HttpEndpoint)
			return true;
		else
			return false;
	}

}
