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
importPackage(java.lang); 
importPackage(javax.jbi.messaging);
importPackage(org.apache.servicemix.jbi.jaxp);

System.out.println("Starting JSR-223 JavaScript processor");

System.out.println(exchange);       
inputMessage = new SourceTransformer().toString(inMessage.getContent());
System.out.println("Hello, I got an input message " + inputMessage);
out = exchange.createMessage();
out.setContent(new StringSource("<response>" + bindings.get("answerJS") + "</response>"));
exchange.setMessage(out, "out");
System.out.println(exchange);

System.out.println("Stopping JSR-223 JavaScript processor");
