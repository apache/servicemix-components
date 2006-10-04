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
   print "Processing exchange "
   print exchange
   print " using "
   print @exchangeHelper
   @exchangeHelper.sendExchange(exchange)
 end

 def stop()
   print "Stopping"
 end

end