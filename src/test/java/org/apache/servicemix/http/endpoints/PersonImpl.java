package org.apache.servicemix.http.endpoints;

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
		return "Person: " + 
			"surName [" + surName + "], givenName [" + givenName + "], age [" + age + "]";
	}
}
