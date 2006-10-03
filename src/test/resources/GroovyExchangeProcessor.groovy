import org.apache.servicemix.script.ScriptExchangeProcessor;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

class GroovyExchangeProcessor extends ScriptExchangeProcessor {

	def void start() {
	    println "Starting";
	}

    def void process(javax.jbi.messaging.MessageExchange exchange) {
    	def inputMessage = new SourceTransformer().toString(exchange.getInMessage().getContent());
    	println "Hello, I got an input message "+inputMessage;
    	NormalizedMessage out = exchange.createMessage();
        out.setContent(new StringSource("<world>hello</world>"));
        exchange.setMessage(out, "out");
        send(exchange);    	
    }
    
    def void stop() {
    	println "Stopping";
    }
}