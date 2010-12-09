package org.apache.servicemix.cxfse;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService
public interface Greeter2 {

	@RequestWrapper(className = "org.apache.servicemix.cxfse.SayHiRequest")
    @ResponseWrapper(className = "org.apache.servicemix.cxfse.SayHiResponse")
    @WebMethod(operationName = "SayHi")
	public void sayHi(
			@WebParam(mode = WebParam.Mode.OUT, name = "msg")
			Holder<String> msg);

	@RequestWrapper(className = "org.apache.servicemix.cxfse.GreetMeRequest")
    @ResponseWrapper(className = "org.apache.servicemix.cxfse.GreetMeResponse")
    @WebMethod(operationName = "GreetMe")
	public void greetMe(
			@WebParam(mode = WebParam.Mode.IN, name = "name")
			Holder<String> name,
			@WebParam(mode = WebParam.Mode.OUT, name = "msg")
			Holder<String> msg);
	
	public int add(AddRequest parameters);
}
