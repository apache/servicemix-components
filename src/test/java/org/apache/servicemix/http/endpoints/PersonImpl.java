package org.apache.servicemix.http.endpoints;

import java.io.StringWriter;
import java.io.Writer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PersonImpl implements Person {
	
	protected String givenName; 
	protected String surName;
	protected int age;
	
	public PersonImpl(String givenName, String surName, int age) {
		this.givenName = givenName;
		this.surName = surName;
		this.age = age;
	}

	public String getGivenName() {
		return givenName;
	}

	public String getSurName() {
		return surName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName; 		
	}

	public void setSurName(String surName) {
		this.surName = surName;
	}
	
	public int getAge() {
		return age;
	}
	
	public void setAge(int age) {
		this.age = age;
	}

	public String toString() {
		Writer w = new StringWriter();
		XStream xstream = new XStream(new DomDriver());
		xstream.alias("person", PersonImpl.class);
		xstream.aliasField("given-name", PersonImpl.class, "givenName");
		xstream.aliasField("sur-name", PersonImpl.class, "surName");
		xstream.toXML(this, w);
		return w.toString();
	}
}
