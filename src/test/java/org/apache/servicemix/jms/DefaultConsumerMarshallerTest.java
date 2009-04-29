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
package org.apache.servicemix.jms;

import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;

import com.mockrunner.mock.jms.MockTextMessage;
import org.apache.servicemix.jms.endpoints.DefaultConsumerMarshaler;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.mock.MockNormalizedMessage;
import junit.framework.TestCase;
import org.custommonkey.xmlunit.XMLAssert;

public class DefaultConsumerMarshallerTest extends TestCase {

   public void testWhiteSpace() throws Exception {
      String inputText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <Test>" +
                         "<element> </element>" +
                         "</Test>";

      MockTextMessage mockTextMessage = new MockTextMessage(inputText);
      MockNormalizedMessage mockNormalizedMessage = new MockNormalizedMessage();
      MockDefaultConsumerMarshaler defaultConsumerMarshaler = new MockDefaultConsumerMarshaler();
      defaultConsumerMarshaler.populateMessage(mockTextMessage, mockNormalizedMessage);
      String result = new SourceTransformer().contentToString(mockNormalizedMessage);
      XMLAssert.assertXMLEqual(inputText, result);
   }

   class MockDefaultConsumerMarshaler extends DefaultConsumerMarshaler {
      @Override
      public void populateMessage(Message message, NormalizedMessage normalizedMessage) throws Exception {
         super.populateMessage(message,
            normalizedMessage);
      }
   }
}
