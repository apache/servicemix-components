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
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

println "Starting JSR-223 groovy processor";

println exchange;       
def inputMessage = new SourceTransformer().toString(inMessage.getContent());
println "Hello, I got an input message " + inputMessage;
NormalizedMessage out = exchange.createMessage();
out.setContent(new StringSource("<response>" + bindings.get("answerGroovy") + "</response>"));
exchange.setMessage(out, "out");
println exchange;

println "Stopping JSR-223 groovy processor";
