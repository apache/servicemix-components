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

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;

/**
 * @version $Revision: $
 */
public class UriTest extends TestCase {

    private static final Log log = LogFactory.getLog(UriTest.class);
    
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

    protected void dump(URI uri) {
        log.info("URI: " + uri);
        log.info("getAuthority(): " + uri.getAuthority());
        log.info("getFragment(): " + uri.getFragment());
        log.info("getHost(): " + uri.getHost());
        log.info("getPath(): " + uri.getPath());
        log.info("getPort(): " + uri.getPort());
        log.info("getQuery(): " + uri.getQuery());
        log.info("getRawAuthority(): " + uri.getRawAuthority());
        log.info("getRawFragment(): " + uri.getRawFragment());
        log.info("getRawPath(): " + uri.getRawPath());
        log.info("getRawQuery(): " + uri.getRawQuery());
        log.info("getRawSchemeSpecificPart(): " + uri.getRawSchemeSpecificPart());
        log.info("getRawUserInfo(): " + uri.getRawUserInfo());
        log.info("getScheme(): " + uri.getScheme());
        log.info("getSchemeSpecificPart(): " + uri.getSchemeSpecificPart());
        log.info("getUserInfo(): " + uri.getUserInfo());
    }
}
