package org.apache.servicemix.cxfse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "GreetMeRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
    "name"
})
public class GreetMeRequest {

	@XmlElement(name = "name", required = true)
	private String name;

	/**
	 * Gets the value of the name property.
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the value of the name property.
	 * @param kbId
	 */
	public void setName(String name) {
		this.name = name;
	}
}
