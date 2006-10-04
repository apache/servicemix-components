require 'java'

include_class 'org.apache.servicemix.common.ExchangeProcessor'

class RubyExchangeProcessor < ExchangeProcessor

 def setExchangeHelper(exchangeHelper)
  @@exchangeHelper = exchangeHelper
 end

 def start()
   print "Starting"
 end
 
 def process(messageExchange)
   print "Processing exchange in JRuby"
   @@exchangeHelper.send messageExchange
 end

 def stop()
   print "Stopping"
 end

end