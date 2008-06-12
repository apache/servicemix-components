# 
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed  under the  License is distributed on an "AS IS" BASIS,
# WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
# implied.
#  
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
include Java

import javax.jbi.messaging.MessageExchange
import javax.jbi.messaging.NormalizedMessage
import org.apache.servicemix.jbi.jaxp.StringSource
import org.apache.servicemix.jbi.jaxp.SourceTransformer

print "Starting JSR-223 jruby processor\n"

print $exchange
print "\n"

inputMessage = SourceTransformer.new().toString($inMessage.getContent());
print "Hello, I got an input message "
print inputMessage
print "\n"

outMsg = $exchange.createMessage()
outMsg.setContent(StringSource.new("<response>" + $bindings.get("answerJRuby") + "</response>"))
$exchange.setMessage(outMsg, "out")

print $exchange
print "\n"

print "Stopping JSR-223 jruby processor\n"

