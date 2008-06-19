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
package org.apache.servicemix.soap.util;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * Utilities for converting QNames into different representations
 * 
 * @author Alex Boisvert
 * @version $Revision: 1.5 $
 * @since 3.0
 */
public class QNameUtil {

    /**
     * Convert QName to the Clark notation, e.g., {namespace}localName
     */
    public static String toString(QName qname) {
        if (qname.getNamespaceURI() == null) {
            return "{}" + qname.getLocalPart();
        } else {
            return "{" + qname.getNamespaceURI() + "}" + qname.getLocalPart();
        }
    }

    /**
     * Convert QName to the Clark notation, e.g., {namespace}localName
     */
    public static String toString(Element element) {
        if (element.getNamespaceURI() == null) {
            return "{}" + element.getLocalName();
        } else {
            return "{" + element.getNamespaceURI() + "}" + element.getLocalName();
        }
    }

    /**
     * Convert QName to the Clark notation, e.g., {namespace}localName
     */
    public static String toString(Attr attr) {
        if (attr.getNamespaceURI() == null) {
            return "{}" + attr.getLocalName();
        } else {
            return "{" + attr.getNamespaceURI() + "}" + attr.getLocalName();
        }
    }

    public static String toString(Collection collection) {
        StringBuffer buf = new StringBuffer();
        Iterator iter = collection.iterator();
        while (iter.hasNext()) {
            QName qname = (QName) iter.next();
            buf.append(toString(qname));
            if (iter.hasNext()) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }
    
    /**
     * Convert a String back into a QName following the Clark notation
     */
    public static QName parse(String name) {
        int pos = name.indexOf('}');
        if (name.startsWith("{") && pos > 0) {
            String ns = name.substring(1, pos);
            String lname = name.substring(pos + 1, name.length());
            return new QName(ns, lname);
        }
        return null;
    }

}
