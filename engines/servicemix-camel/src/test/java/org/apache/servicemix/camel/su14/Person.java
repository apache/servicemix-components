package org.apache.servicemix.camel.su14;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Just a simple person object to test the JAXB fallback converter mechanism
 */
@XmlRootElement
public class Person {

    private String name;
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
