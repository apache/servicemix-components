import org.apache.servicemix.common.ExchangeProcessor;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.script.ScriptExchangeHelper;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

class GroovyExchangeProcessor implements ExchangeProcessor {

    @Property ScriptExchangeHelper exchangeHelper;

	def void start() {
	    println "Starting";
	}

    def void process(MessageExchange exchange) {
    	def inputMessage = new SourceTransformer().toString(exchange.getInMessage().getContent());
    	println "Hello, I got an input message "+inputMessage;
    	NormalizedMessage out = exchange.createMessage();
        out.setContent(new StringSource("<world>hello</world>"));
        exchange.setMessage(out, "out");
        exchangeHelper.sendExchange(exchange);    	
    }
    
    def void stop() {
    	println "Stopping";
    }
}