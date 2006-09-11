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
package org.apache.servicemix.eip;

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.apache.activemq.broker.BrokerService;
import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.transaction.context.GeronimoTransactionManager;
import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.nmr.flow.Flow;
import org.apache.servicemix.jbi.nmr.flow.jca.JCAFlow;
import org.apache.servicemix.jbi.nmr.flow.seda.SedaFlow;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.jdbc.JdbcStoreFactory;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.tranql.connector.AllExceptionsAreFatalSorter;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;

public abstract class AbstractEIPTransactionalTest extends AbstractEIPTest {

    protected BrokerService broker;
    protected TransactionManager tm;
    protected DataSource dataSource;
    protected Store store;
    
    protected void setUp() throws Exception {
        // Create an AMQ broker
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:61616");
        broker.start();
        
        TransactionManagerImpl exTransactionManager = new TransactionManagerImpl(600, new XidFactoryImpl(), null, null);
        TransactionContextManager transactionContextManager = new TransactionContextManager(exTransactionManager, exTransactionManager);
        tm = (TransactionManager) new GeronimoTransactionManager(transactionContextManager);
        
        // Create an embedded database for testing tx results when commit / rollback
        ConnectionManager cm = new GenericConnectionManager(
                        new XATransactions(true, true),
                        new NoPool(),
                        false,
                        null,
                        transactionContextManager,
                        "connectionManager",
                        GenericConnectionManager.class.getClassLoader());
        ManagedConnectionFactory mcf = new DerbyDataSourceMCF("target/testdb");
        dataSource = (DataSource) mcf.createConnectionFactory(cm);

        JdbcStoreFactory storeFactory = new JdbcStoreFactory();
        storeFactory.setDataSource(dataSource);
        storeFactory.setTransactional(true);
        store = storeFactory.open("store");
        
        JCAFlow jcaFlow = new JCAFlow();
        jcaFlow.setTransactionContextManager(transactionContextManager);
        
        jbi = new JBIContainer();
        jbi.setFlows(new Flow[] { new SedaFlow(), jcaFlow });
        jbi.setEmbedded(true);
        jbi.setUseMBeanServer(false);
        jbi.setCreateMBeanServer(false);
        jbi.setTransactionManager(tm);
        jbi.setAutoEnlistInTransaction(true);
        listener = new ExchangeCompletedListener();
        jbi.addListener(listener);
        jbi.init();
        jbi.start();

        client = new DefaultServiceMixClient(jbi);
    }
    
    protected void tearDown() throws Exception {
        listener.assertExchangeCompleted();
        jbi.shutDown();
        broker.stop();
    }

    protected void configurePattern(EIPEndpoint endpoint) {
        endpoint.setStore(store);
    }
    
    public static class DerbyDataSourceMCF extends AbstractXADataSourceMCF {
        private static final long serialVersionUID = 7971682207810098396L;
        protected DerbyDataSourceMCF(String dbName) {
            super(createXADS(dbName), new AllExceptionsAreFatalSorter());
        }
        public String getPassword() {
            return null;
        }
        public String getUserName() {
            return null;
        }
        protected static XADataSource createXADS(String dbName) {
            EmbeddedXADataSource xads = new EmbeddedXADataSource();
            xads.setDatabaseName(dbName);
            xads.setCreateDatabase("create");
            return xads;
        }
    }
    
}
