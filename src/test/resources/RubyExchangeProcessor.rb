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
require 'java'

include_class 'javax.jbi.messaging.MessageExchange'
include_class 'org.apache.servicemix.jbi.jaxp.StringSource'
include_class 'org.apache.servicemix.common.ExchangeProcessor'
include_class 'org.apache.servicemix.script.ScriptExchangeHelper'

class RubyExchangeProcessor
 include ExchangeProcessor

 def setExchangeHelper(exchangeHelper)
  @exchangeHelper = exchangeHelper
 end

 def start()
   print "Starting\n"
 end
 
 def process(exchange)
   print "Processing exchange "
   print exchange
   print " using "
   print @exchangeHelper
   print "\n"
   out = exchange.createMessage()
   out.setContent(StringSource.new("<world>hello</world>"))
   exchange.setMessage(out, "out")
   @exchangeHelper.sendExchange(exchange)
 end

 def stop()
   print "Stopping\n"
 end
 
 def toString()
 	return "You do need to implement a toString?"
 end

end
