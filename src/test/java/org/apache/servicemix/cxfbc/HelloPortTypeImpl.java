package org.apache.servicemix.cxfbc;

import uri.helloworld.HelloFault_Exception;
import uri.helloworld.HelloHeader;
import uri.helloworld.HelloPortType;
import uri.helloworld.HelloRequest;
import uri.helloworld.HelloResponse;

public class HelloPortTypeImpl implements HelloPortType {

	public HelloResponse hello(HelloRequest body, HelloHeader header1) throws HelloFault_Exception {
		HelloResponse rep = new HelloResponse();
		rep.setText(body.getText() + header1.getId());
		return rep;
	}

}
