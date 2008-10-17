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
package org.apache.servicemix.snmp.util;

import java.beans.PropertyEditorSupport;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.snmp4j.smi.OID;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * @author lhein
 */
public class OIDListEditor extends PropertyEditorSupport {
    
    /* (non-Javadoc)
     * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        OIDList list = new OIDList();
        if (text.indexOf(",") != -1) {
            // seems to be a comma separated oid list
            StringTokenizer strTok = new StringTokenizer(text, ",");
            while (strTok.hasMoreTokens()) {
                String tok = strTok.nextToken();
                if (tok != null && tok.trim().length()>0) {
                    list.add(new OID(tok.trim()));                    
                } else {
                    // empty token - skip
                }
            }
        } else {
            // seems to be a file resource
            try {
                DefaultResourceLoader loader = new DefaultResourceLoader();
                Resource file = loader.getResource(text);

                if (file.exists()) {
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new FileReader(file.getFile()));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            list.add(new OID(line.trim()));
                        }
                    } catch (IOException ex) {
                        throw new IllegalArgumentException(text + " is not a valid argument.", ex);
                    } finally {
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException ex) {
                                // ignore
                            }
                        }
                    }
                } else {
                    // the specified resource file is not existing
                    throw new IllegalArgumentException(text + " is not a valid argument.");
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException(text + " is not a valid argument.", ex);
            }
        }
        setValue(list);
    }
}
