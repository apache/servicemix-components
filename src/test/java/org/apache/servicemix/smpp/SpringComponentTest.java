package org.apache.servicemix.smpp;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * @author jbonofre
 */
public class SpringComponentTest extends SpringTestSupport {

    private final static String MESSAGE = "<message>" + "<source>0123456789</source>"
                                          + "<destination>9876543210</destination>"
                                          + "<text>SMPP Component Test</text>" + "<ton>NATIONAL</ton>"
                                          + "<npi>NATIONAL</npi>" + "</message>";

    public void testSending() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("http://test", "service"));
        me.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            fail("Received ERROR status: " + me.getError());
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring.xml");
    }

}
