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
package org.apache.servicemix.mail.utils;

import java.beans.PropertyEditorSupport;
import java.util.Collection;

/**
 * @author lhein
 */
public class IgnoreListEditor extends PropertyEditorSupport
{
    @Override
    public void setValue(Object o) {
        IgnoreList list = new IgnoreList();

        if (o != null && o instanceof Collection) {
            Collection col = (Collection)o;
            for (Object obj : col) {
                list.add(obj.toString());
             }
        }

        super.setValue(list);
    }
}
