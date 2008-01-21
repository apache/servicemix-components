/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.saxon;

import java.util.Date;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class SaxonComponentTest extends SpringTestSupport {
    private static transient Log log = LogFactory.getLog(SaxonComponentTest.class);

    public void testXslt() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xslt"));
        me.getInMessage().setContent(new StreamSource(getClass().getResourceAsStream("/books.xml")));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals("2005", textValueOfXPath(el, "/transformed/bookstore/book[1]/year"));
        client.done(me);
    }

    public void testXsltWithElement() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xslt"));
        Element e = transformer.toDOMElement(new StreamSource(getClass().getResourceAsStream("/books.xml")));
        e = DOMUtil.getFirstChildElement(e);
        me.getInMessage().setContent(new DOMSource(e));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals("2005", textValueOfXPath(el, "/transformed/book/year"));
        client.done(me);
    }

    public void testXsltDynamic() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xslt-dynamic"));
        Element e = transformer.toDOMElement(new StreamSource(getClass().getResourceAsStream("/books.xml")));
        e = DOMUtil.getFirstChildElement(e);
        me.getInMessage().setContent(new DOMSource(e));
        me.getInMessage().setProperty("xslt.source", "classpath:transform.xsl");
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals("2005", textValueOfXPath(el, "/transformed/book/year"));
        client.done(me);
    }

    public void testXsltWithParam() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xslt-params"));
        me.getInMessage().setContent(new StringSource("<sample id='777888' sent='"
                + new Date() + "'>hello world!</sample>"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals("cheeseyCheese", textValueOfXPath(el, "//param"));
        assertEquals("4002", textValueOfXPath(el, "//integer"));
    }

    public void testXsltWithDocCall() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xslt-doccall"));
        me.getInMessage().setContent(new StreamSource(getClass().getClassLoader().getResourceAsStream("request.xml")));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
    }

    public void testXQuery() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xquery"));
        me.getInMessage().setContent(new StreamSource(getClass().getResourceAsStream("/books.xml")));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals("XQuery Kick Start", textValueOfXPath(el, "/titles/title[1]"));
        client.done(me);
    }

    public void testXQueryInline() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xquery-inline"));
        me.getInMessage().setContent(new StreamSource(getClass().getResourceAsStream("/books.xml")));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals(new QName("http://saxon.sf.net/xquery-results", "sequence"), DOMUtil.getQName(el));
        el = DOMUtil.getFirstChildElement(el);
        assertEquals(new QName("http://saxon.sf.net/xquery-results", "element"), DOMUtil.getQName(el));
        el = DOMUtil.getFirstChildElement(el);
        assertEquals(new QName("title"), DOMUtil.getQName(el));
        assertEquals("XQuery Kick Start", DOMUtil.getElementText(el));
        client.done(me);
    }

    public void testXQueryDynamic() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("urn:test", "xquery-dynamic"));
        me.getInMessage().setContent(new StreamSource(getClass().getResourceAsStream("/books.xml")));
        me.getInMessage().setProperty("xquery.source", getClass().getResource("/query.xq"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
        log.info(transformer.toString(me.getOutMessage().getContent()));
        Element el = transformer.toDOMElement(me.getOutMessage());
        assertEquals("XQuery Kick Start", textValueOfXPath(el, "/titles/title[1]"));
        client.done(me);
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring.xml");
    }

}
