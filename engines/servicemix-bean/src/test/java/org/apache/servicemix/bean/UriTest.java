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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @version $Revision: $
 */
public class UriTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(UriTest.class);
    
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
        LOG.info("URI: " + uri);
        LOG.info("getAuthority(): " + uri.getAuthority());
        LOG.info("getFragment(): " + uri.getFragment());
        LOG.info("getHost(): " + uri.getHost());
        LOG.info("getPath(): " + uri.getPath());
        LOG.info("getPort(): " + uri.getPort());
        LOG.info("getQuery(): " + uri.getQuery());
        LOG.info("getRawAuthority(): " + uri.getRawAuthority());
        LOG.info("getRawFragment(): " + uri.getRawFragment());
        LOG.info("getRawPath(): " + uri.getRawPath());
        LOG.info("getRawQuery(): " + uri.getRawQuery());
        LOG.info("getRawSchemeSpecificPart(): " + uri.getRawSchemeSpecificPart());
        LOG.info("getRawUserInfo(): " + uri.getRawUserInfo());
        LOG.info("getScheme(): " + uri.getScheme());
        LOG.info("getSchemeSpecificPart(): " + uri.getSchemeSpecificPart());
        LOG.info("getUserInfo(): " + uri.getUserInfo());
    }
}
