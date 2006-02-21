/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.wsn.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.servicemix.wsn.AbstractCreatePullPoint;
import org.apache.servicemix.wsn.AbstractPullPoint;

public class JmsCreatePullPoint extends AbstractCreatePullPoint {

    private ConnectionFactory connectionFactory;
    private Connection connection;
    
    public JmsCreatePullPoint(String name) {
        super(name);
    }

    public void init() throws Exception {
        if (connection == null) {
            connection = connectionFactory.createConnection();
            connection.start();
        }
        super.init();
    }
    
    @Override
    protected AbstractPullPoint createPullPoint(String name) {
        JmsPullPoint pullPoint = new JmsPullPoint(name);
        pullPoint.setManager(getManager());
        pullPoint.setConnection(connection);
        return pullPoint;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

}
