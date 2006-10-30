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
package org.apache.servicemix.ftp;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.net.SocketClient;

import javax.jbi.JBIException;
import java.net.InetAddress;
import java.io.IOException;

/**
 * A pool of {@link org.apache.commons.net.SocketClient} instances from the
 * <a href="http://jakarta.apache.org/commons/net.html">Jakarta Commons Net</a> library to handle
 * internet networking connections.
 *
 * @version $Revision: 426415 $
 */
public abstract class SocketClientPoolSupport implements InitializingBean, DisposableBean, PoolableObjectFactory {
    private ObjectPool pool;
    private InetAddress address;
    private String host;
    private int port = -1;
    private InetAddress localAddress;
    private int localPort;

    public void afterPropertiesSet() throws Exception {
        if (pool == null) {
            GenericObjectPool goPool = new GenericObjectPool();
            goPool.setTestOnBorrow(true);
            pool = goPool;
        }
        pool.setFactory(this);
    }

    public void destroy() throws Exception {
        if (pool != null) {
            pool.close();
        }
    }

    public SocketClient borrowClient() throws Exception {
        return (SocketClient) getPool().borrowObject();
    }

    public void returnClient(SocketClient client) throws Exception {
        getPool().returnObject(client);
    }

    // PoolableObjectFactory interface
    //-------------------------------------------------------------------------
    public Object makeObject() throws Exception {
        SocketClient client = createSocketClient();
        connect(client);
        return client;
    }

    public void destroyObject(Object object) throws Exception {
        SocketClient client = (SocketClient) object;
        disconnect(client);
    }

    public boolean validateObject(Object object) {
        return true;
    }

    public void activateObject(Object object) throws Exception {
    }

    public void passivateObject(Object object) throws Exception {
    }


    // Properties
    //-------------------------------------------------------------------------
    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public InetAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetAddress localAddress) {
        this.localAddress = localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ObjectPool getPool() {
        return pool;
    }

    public void setPool(ObjectPool pool) {
        if (pool != null) {
            pool.setFactory(this);
        }
        this.pool = pool;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected void connect(SocketClient client) throws IOException, JBIException, Exception {
        if (host != null) {
            if (localAddress != null) {
                client.connect(host, port, localAddress, localPort);
            }
            else if (port >= 0) {
                client.connect(host, port);
            }
            else {
                client.connect(host);
            }
        }
        else if (address != null) {
            if (localAddress != null) {
                client.connect(address, port, localAddress, localPort);
            }
            else if (port >= 0) {
                client.connect(address, port);
            }
            else {
                client.connect(address);
            }
        }
        else {
            throw new JBIException("You must configure the address or host property");
        }
    }


    protected void disconnect(SocketClient client) throws Exception {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    protected abstract SocketClient createSocketClient();

}
