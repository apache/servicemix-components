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
package org.apache.servicemix.jsr181.xfire;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jsr181.Jsr181Component;
import org.apache.servicemix.jsr181.Jsr181Endpoint;
import org.codehaus.xfire.XFire;

public class JbiProxyTest extends TestCase {

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.setFlowName("st");
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testProxy() throws Exception {
        container.start();

        Jsr181Component component1 = new Jsr181Component();
        Jsr181Endpoint endpoint1 = new Jsr181Endpoint();
        endpoint1.setPojo(new EchoService());
        component1.setEndpoints(new Jsr181Endpoint[] { endpoint1 });
        container.activateComponent(component1, "JSR181Component-1");
        
        Jsr181Component component2 = new Jsr181Component();
        Jsr181Endpoint endpoint2 = new Jsr181Endpoint();
        endpoint2.setPojo(new ProxyPojoService());
        endpoint2.setServiceInterface(ProxyPojo.class.getName());
        component2.setEndpoints(new Jsr181Endpoint[] { endpoint2 });
        container.activateComponent(component2, "JSR181Component-2");
        
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://xfire.jsr181.servicemix.apache.org", "ProxyPojoPortType"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://jsr181.servicemix.apache.org'><echoin0>world</echoin0></echo>"));
        client.sendSync(me);
        if (me.getError() != null) {
            throw me.getError();
        }
        assertTrue(me.getStatus() == ExchangeStatus.ACTIVE);
        client.done(me);
    }
    
    public static interface Echo {
        String echo(String msg);
    }
    
    public static class EchoService implements Echo {
        public String echo(String msg) {
            return msg;
        }
    }
    
    public static interface ProxyPojo {
        String echo(String s);
    }
    
    public static class ProxyPojoService implements ProxyPojo {
        private ComponentContext context;
        private Echo proxy;

        public ComponentContext getContext() {
            return context;
        }

        public void setContext(ComponentContext context) throws Exception {
            this.context = context;
            if (context != null) {
                try {
                    XFire xfire = Jsr181Component.createXFire(context);
                    QName service = new QName("http://xfire.jsr181.servicemix.apache.org", "EchoService");
                    proxy = (Echo) JbiProxy.create(xfire, context, null, service, null, Echo.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                } catch (Error e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                proxy = null;
            }
        }
        
        public String echo(String s) {
            return proxy.echo(s);
        }
    }
    
}
