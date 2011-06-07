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
package org.apache.servicemix.http;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * By default, the Maven build will find free port numbers and assign them to system properties for our tests to use.
 *
 * However, when running the unit tests in an IDE, this mechanism is not always available so this class provides a
 * helper method to graciously fall back to a random port number for running the tests.
 */
public class PortFinder {

    private static final int DEFAULT_PORT = 10101;

    private PortFinder() {
        // hiding the constructor, only static helper methods in this class
    }

    /**
     * Find the port number that's defined in the system properties.  If there's no valid port number there,
     * this method will try to return a random, free port number.
     *
     * @param name the system property
     * @return the port number defined or a random, free port number
     */
    public static int find(String name) {
        String port = System.getProperty(name);

        if (port == null) {
            return findFreePort();
        }

        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return findFreePort();
        }
    }

    private static int findFreePort() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException e) {
            return DEFAULT_PORT;
        }
    }
}
