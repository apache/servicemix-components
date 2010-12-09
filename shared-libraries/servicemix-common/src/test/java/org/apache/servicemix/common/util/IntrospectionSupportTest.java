/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.common.util;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;

import java.util.Map;
import junit.framework.TestCase;

public class IntrospectionSupportTest extends TestCase {
	
    public void testProperties() throws Exception {    	   	
    	
    	// Tests setProperties
    	try {
    	    IntrospectionSupport.setProperties(null, null);
    	    fail();
    	} catch (IllegalArgumentException ex) {
    		// "target was null."
    	}
    	
    	try {
    	    IntrospectionSupport.setProperties(new Object(), null);
    	    fail();
    	} catch (IllegalArgumentException ex) {
    		// "props was null."
    	}
    	        
        IntrospectionSupport.setProperties(new Person(), getProps());                
        
        // Tests extractProperties
        try {
    	    IntrospectionSupport.extractProperties(null, "test-");
    	    fail();
    	} catch (IllegalArgumentException ex) {
    		// "props was null."
    	}
        
        Map map = IntrospectionSupport.extractProperties(getProps(), "test-");
        assertTrue(map.containsKey("Name"));
        assertEquals("Joe", map.get("Name").toString());
        
    	// Tests setProperties
        try {
    	    IntrospectionSupport.setProperties(null, null, null);
    	    fail();
    	} catch (IllegalArgumentException ex) {
    		// "target was null."
    	}
    	
    	try {
    	    IntrospectionSupport.setProperties(new Object(), null, null);
    	    fail();
    	} catch (IllegalArgumentException ex) {
    		// "props was null."
    	}
        
        boolean setProp = IntrospectionSupport.setProperties(new Person(), getProps(), "test-");
        assertTrue(setProp);
        
        String name = IntrospectionSupport.toString(new Person("bloggs"));
        assertTrue(name.contains("bloggs"));
        String buf = IntrospectionSupport.toString(new Person("bloggs"), new Person().getClass());
        assertTrue(name.contains("bloggs"));
        System.out.println("end");
    }
    
    public Map getProps() throws Exception {    	    	
    	URI uri = new URI("http://www.apache.org/");
    	Map<String, Object> props =  new HashMap<String,Object>();
    	props.put("test-Name", "Joe");
    	props.put("test-location", "USA");
    	props.put("test-job", "IT");
    	props.put("test-Number", "12");
    	return props;
    }
    
    
    public class Address {
    	String city;
    	String country;
    	
    	public Address(String city, String country) {
    		this.city = city;
    		this.country = country;
    	}
    	
    	public void setCity(String city) {
    		this.city = city;
    	}
    	
    	public String getCity() {
    		return this.city;
    	}
    	
    	public void setCountry(String country) {
    		this.country = country;   	
    	}
    	
    	public String getCountry() {
    		return this.country;
    	}    
    	
    	public String toString() {
    		return this.city + this.country;
    	}
    }
    
    public class Person {
    	
    	String name;
    	URI location;
    	String job;    	
    	long number;
    	
    	public Person() {
    	}
    	
    	public Person(String name) {
    		this.name =  name;
    	}
    	
    	public Person(String name, String job) {
    		this.name =  name;
    		this.job =  job;
    	}
    	
    	public void setName(String name) {
    		this.name =  name;    		    		
    	}
    	
    	public String getName() {
    		return this.name;
    	}
    	
    	public void setLocation(URI location) {
    		this.location = location;
    	}
    	
    	public URI getLocation() {
    		return this.location;
    	}    	
    	
    	public String getJob() {
    		return this.job;
    	}
    	
    	public void setNumber(long number) {    		
    		this.number = number; 
    	}
    	
    	public long getNumber() {
    		return this.number;
    	}
    
    }
    

}
