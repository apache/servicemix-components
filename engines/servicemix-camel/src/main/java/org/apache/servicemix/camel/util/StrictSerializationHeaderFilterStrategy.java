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
package org.apache.servicemix.camel.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.camel.Exchange;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.camel.spi.HeaderFilterStrategy} that filters out non-serializable values.
 *
 * It will try to write the object to a stream to make sure that an object that implements the
 * {@link Serializable} interface can actually be serialized
 */
public class StrictSerializationHeaderFilterStrategy implements HeaderFilterStrategy {

    private final Logger logger = LoggerFactory.getLogger(StrictSerializationHeaderFilterStrategy.class);

    public boolean applyFilterToCamelHeaders(String s, Object o, Exchange exchange) {
        return doApplyFilter(s, o);
    }

    public boolean applyFilterToExternalHeaders(String s, Object o, Exchange exchange) {
        return doApplyFilter(s, o);
    }

    private boolean doApplyFilter(String s, Object o) {
        if (o instanceof Serializable) {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(new ByteArrayOutputStream());
                oos.writeObject(o);
            } catch (IOException e) {
                logger.debug(String.format("%s implements Serializable, but serialization throws IOException: filtering key %s", o, s));
                return true;
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                        // ignoring exception on stream close
                    }
                }
            }
            return false;
        }
        return true;
    }
}
