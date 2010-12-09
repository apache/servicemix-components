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
package org.apache.servicemix.http.endpoints;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PersonImpl implements Person, Serializable {

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
