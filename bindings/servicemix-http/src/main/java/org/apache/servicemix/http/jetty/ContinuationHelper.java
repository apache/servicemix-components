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

import org.mortbay.util.ajax.Continuation;

/**
 * A few helper methods for working with Jetty continuations
 */
public class ContinuationHelper {

    private ContinuationHelper() {
        // static helper methods only - no need for a public constructor
    }

    /**
     * Is this a new continuation object?
     *
     * @param continuation
     * @return <code>true</code> for a new continuation object
     */
    public static boolean isNewContinuation(Continuation continuation) {
        return continuation.isNew() ||
               (!continuation.isPending() && !continuation.isResumed());
    }
}
