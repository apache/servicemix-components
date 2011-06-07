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
package org.apache.servicemix.http.jetty;

import junit.framework.TestCase;
import org.mortbay.util.ajax.Continuation;

import static org.apache.servicemix.http.jetty.ContinuationHelper.isNewContinuation;

/**
 * Test case for {@link ContinuationHelper}
 */
public class ContinuationHelperTest extends TestCase {


    public void testIsNewContinuation() {
        assertTrue(isNewContinuation(new MockContinuation(true, null, null)));
        assertTrue(isNewContinuation(new MockContinuation(false, false, false)));

        assertFalse(isNewContinuation(new MockContinuation(false, true, false)));
        assertFalse(isNewContinuation(new MockContinuation(false, false, true)));
    }

    protected static final class MockContinuation implements Continuation {

        private boolean isNew;
        private Boolean pending;
        private Boolean resumed;

        protected MockContinuation(boolean isNew, Boolean pending, Boolean resumed) {
            super();
            this.isNew = isNew;
            this.pending = pending;
            this.resumed = resumed;
        }

        public boolean suspend(long l) {
            throw new UnsupportedOperationException("Not yet supported");
        }

        public void resume() {
            throw new UnsupportedOperationException("Not yet supported");
        }

        public void reset() {
            throw new UnsupportedOperationException("Not yet supported");
        }

        public boolean isNew() {
            return isNew;
        }

        public boolean isPending() {
            return pending;
        }

        public boolean isResumed() {
            return resumed;
        }

        public Object getObject() {
            throw new UnsupportedOperationException("Not yet supported");
        }

        public void setObject(Object o) {
            throw new UnsupportedOperationException("Not yet supported");
        }
    }
}
