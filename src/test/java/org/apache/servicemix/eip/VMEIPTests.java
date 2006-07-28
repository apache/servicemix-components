package org.apache.servicemix.eip;

import junit.framework.Test;
import junit.framework.TestSuite;

public class VMEIPTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.apache.servicemix.eip");
        //$JUnit-BEGIN$
        suite.addTestSuite(XPathSplitterTest.class);
        suite.addTestSuite(PipelineTest.class);
        suite.addTestSuite(StaticRoutingSlipTest.class);
        suite.addTestSuite(ContentBasedRouterTest.class);
        suite.addTestSuite(WireTapTest.class);
        suite.addTestSuite(StaticRecipientListTest.class);
        suite.addTestSuite(MessageFilterTest.class);
        suite.addTestSuite(SplitAggregatorTest.class);
        //$JUnit-END$
        return suite;
    }

}
