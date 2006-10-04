require 'java'

include_class 'org.apache.servicemix.common.ExchangeProcessor'

class RubyExchangeProcessor < ExchangeProcessor

 def setExchangeHelper(exchangeHelper)
  @exchangeHelper = exchangeHelper
 end

 def start()
   print "Starting"
 end
 
 def process(exchange)
   print "Processing exchange in JRuby"
   @exchangeHelper.sendExchange exchange
 end

 def stop()
   print "Stopping"
 end
 
 def toString()
   return "RubyExchangeProcessor"
 end

end