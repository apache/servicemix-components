package org.apache.servicemix.http.endpoints;

public interface Person {

	String getGivenName(); 
	
	void setGivenName(String givenName);
	
	String getSurName();
	
	void setSurName(String surName);
	
	int getAge();
	
	void setAge(int age);
	
	String toString();
}
