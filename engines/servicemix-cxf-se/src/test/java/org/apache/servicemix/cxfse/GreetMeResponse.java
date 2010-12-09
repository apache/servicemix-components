package org.apache.servicemix.cxfse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "GreetMeResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
	"msg"
})
public class GreetMeResponse {

	@XmlElement(name = "msg", required = true)
	private String msg;

	/**
	 * Gets the value of the msg property.
	 * @return
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * Sets the value of the msg property.
	 * @param msg
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}
}