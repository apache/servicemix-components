package org.apache.servicemix.jms;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.naming.Context;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.apache.xbean.spring.jndi.SpringInitialContextFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class JmsSpringTest extends SpringTestSupport {

    private static Log logger =  LogFactory.getLog(JmsSpringTest.class);

    public void test() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getFault() != null) {
                fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
            } else if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else {
            logger.info(new SourceTransformer().toString(me.getOutMessage().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, SpringInitialContextFactory.class.getName());
        System.setProperty(Context.PROVIDER_URL, "org/apache/servicemix/jms/jndi.xml");
        return new ClassPathXmlApplicationContext("org/apache/servicemix/jms/spring.xml");
    }

}
