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
package org.apache.servicemix.cxfse;


import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService(serviceName = "Greeter2", endpointInterface = "org.apache.servicemix.cxfse.Greeter2")
public class Greeter2Impl implements Greeter2 {

    public void sayHi(Holder<String> msg) {
    	msg.value = "Hi";
	}

	public void greetMe(Holder<String> name, Holder<String> msg) {
		msg.value = "Hello " + name.value;
	}
	
	public int add(AddRequest parameters) {
        System.out.println(parameters);
        try {
            int _return = parameters.nb1 + parameters.nb2;
            return _return;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
