package org.apache.servicemix.bean;

import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.servicemix.tck.ReceiverComponent;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class SenderBeanTest extends SpringTestSupport {

    public void testSendingToDynamicEndpointForExchangeProcessorBeanWithFooOperation() throws Exception {

        ReceiverComponent receiver = (ReceiverComponent) getBean("receiver");
        receiver.getMessageList().assertMessagesReceived(1);

        ((ExchangeCompletedListener) getBean("listener")).assertExchangeCompleted();
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("sender-bean.xml");
    }

}
