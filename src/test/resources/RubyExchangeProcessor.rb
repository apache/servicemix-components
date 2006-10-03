require 'java'

include_class 'org.apache.servicemix.script.ScriptExchangeProcessor'
include_class 'javax.jbi.messaging.MessageExchange'
include_class 'javax.jbi.messaging.NormalizedMessage'
include_class 'org.apache.servicemix.jbi.jaxp.StringSource'
include_class 'org.apache.servicemix.jbi.jaxp.SourceTransformer'

class RubyExchangeProcessor < org.apache.servicemix.script.ScriptExchangeProcessor

 def start()
   print "Starting"
 end
 
 def process(javax.jbi.messaging.MessageExchange me)
   print "Processing "+me
 end

 def stop()
   print "Stopping"
 end

end