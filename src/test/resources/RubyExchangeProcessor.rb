require 'java'

include_class 'javax.jbi.messaging.MessageExchange'
include_class 'org.apache.servicemix.jbi.jaxp.StringSource'
include_class 'org.apache.servicemix.common.ExchangeProcessor'
include_class 'org.apache.servicemix.script.ScriptExchangeHelper'

class RubyExchangeProcessor < ExchangeProcessor

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