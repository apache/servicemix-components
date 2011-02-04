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
package org.apache.servicemix.bean;

import java.net.URI;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Revision: $
 */
public class UriTest extends TestCase {

    private final Logger logger = LoggerFactory.getLogger(UriTest.class);
    
    public void testSimpleURI() throws Exception {
        URI uri = new URI("bean:cheese");
        dump(uri);
    }

    public void testSimpleURIWithQuery() throws Exception {
        URI uri = new URI("bean:cheese?foo=bar");
        dump(uri);
    }

    public void testHierarchial() throws Exception {
        URI uri = new URI("bean://cheese?foo=123");
        dump(uri);
    }

    public void testHierarchialWithPath() throws Exception {
        URI uri = new URI("bean://cheese/path?foo=123");
        dump(uri);
    }


    public void testHierarchialWithAbsolutePath() throws Exception {
        URI uri = new URI("file:///cheese/path?foo=123");
        dump(uri);
    }

    public void testUserNameAndPassword() throws Exception {
        URI uri = new URI("jabber://me@host/them@somewhere.else.com?foo=bar");
        dump(uri);
    }

    protected void dump(URI uri) {
        logger.info("URI: " + uri);
        logger.info("getAuthority(): " + uri.getAuthority());
        logger.info("getFragment(): " + uri.getFragment());
        logger.info("getHost(): " + uri.getHost());
        logger.info("getPath(): " + uri.getPath());
        logger.info("getPort(): " + uri.getPort());
        logger.info("getQuery(): " + uri.getQuery());
        logger.info("getRawAuthority(): " + uri.getRawAuthority());
        logger.info("getRawFragment(): " + uri.getRawFragment());
        logger.info("getRawPath(): " + uri.getRawPath());
        logger.info("getRawQuery(): " + uri.getRawQuery());
        logger.info("getRawSchemeSpecificPart(): " + uri.getRawSchemeSpecificPart());
        logger.info("getRawUserInfo(): " + uri.getRawUserInfo());
        logger.info("getScheme(): " + uri.getScheme());
        logger.info("getSchemeSpecificPart(): " + uri.getSchemeSpecificPart());
        logger.info("getUserInfo(): " + uri.getUserInfo());
    }
}
