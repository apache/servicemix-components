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
import org.apache.servicemix.jbi.jaxp.StringSource
import org.apache.servicemix.common.ExchangeProcessor
import org.apache.servicemix.script.ScriptExchangeHelper

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
   out.content = StringSource.new("<world>hello</world>")
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
