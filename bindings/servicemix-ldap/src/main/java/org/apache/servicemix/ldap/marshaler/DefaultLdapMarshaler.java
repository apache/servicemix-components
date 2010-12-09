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
package org.apache.servicemix.ldap.marshaler;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * This is the default LDAP marshaler 
 * 
 * @author jbonofre
 */
public class DefaultLdapMarshaler implements LdapMarshalerSupport {

	// logging facility
	private final static transient Log LOG = LogFactory.getLog(DefaultLdapMarshaler.class);
	
	// message content format
	private final static String TAG_ENTRIES = "entries";
	private final static String TAG_ENTRY = "entry";
	private final static String TAG_ENTRY_DN = "dn";
	private final static String TAG_ENTRY_ATTRIBUTES = "attributes";
	private final static String TAG_ENTRY_ATTRIBUTE = "attribute";
	private final static String TAG_ENTRY_ATTRIBUTE_NAME = "name";
	private final static String TAG_ENTRY_ATTRIBUTE_VALUES = "values";
	private final static String TAG_ENTRY_ATTRIBUTE_VALUE = "value";
	
	private final static String TAG_ENTRIES_OPEN = "<" + TAG_ENTRIES + ">";
	private final static String TAG_ENTRIES_CLOSE = "</" + TAG_ENTRIES + ">";
	private final static String TAG_ENTRY_OPEN = "<" + TAG_ENTRY + ">";
	private final static String TAG_ENTRY_CLOSE = "</" + TAG_ENTRY + ">";
	private final static String TAG_ENTRY_DN_OPEN = "<" + TAG_ENTRY_DN + ">";
	private final static String TAG_ENTRY_DN_CLOSE = "</" + TAG_ENTRY_DN + ">";
	private final static String TAG_ENTRY_ATTRIBUTES_OPEN = "<" + TAG_ENTRY_ATTRIBUTES + ">";
	private final static String TAG_ENTRY_ATTRIBUTES_CLOSE = "</" + TAG_ENTRY_ATTRIBUTES + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_OPEN = "<" + TAG_ENTRY_ATTRIBUTE + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_CLOSE = "</" + TAG_ENTRY_ATTRIBUTE + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_NAME_OPEN = "<" + TAG_ENTRY_ATTRIBUTE_NAME + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_NAME_CLOSE = "</" + TAG_ENTRY_ATTRIBUTE_NAME + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_VALUES_OPEN = "<" + TAG_ENTRY_ATTRIBUTE_VALUES + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_VALUES_CLOSE = "</" + TAG_ENTRY_ATTRIBUTE_VALUES + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_VALUE_OPEN = "<" + TAG_ENTRY_ATTRIBUTE_VALUE + ">";
	private final static String TAG_ENTRY_ATTRIBUTE_VALUE_CLOSE = "</" + TAG_ENTRY_ATTRIBUTE_VALUE + ">";
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.servicemix.ldap.marshaler.LdapMarshalerSupport#readNamingEnumeration(javax.jbi.messaging.NormalizedMessage, javax.naming.NamingEnumeration)  
	 */
	public void marshal(NormalizedMessage message, NamingEnumeration namingEnumeration) throws MessagingException {
		if (message == null) {
		    throw new MessagingException("The NormalizedMessage is null.");
		}
		if (namingEnumeration == null) {
		    throw new MessagingException("The NamingEnumeration is null.");
		}
		
		// TODO use JAXB to marshal
		
		StringBuffer data = new StringBuffer();
		
		data.append(TAG_ENTRIES_OPEN);
		
		try {
		    LOG.debug("Iterate in the LDAP search naming enumeration");
		    while(namingEnumeration.hasMore()) {
		        SearchResult result = (SearchResult)namingEnumeration.next();
		        LOG.debug("Get the result with DN " + result.getName());
		        data.append(TAG_ENTRY_OPEN);
		        data.append(TAG_ENTRY_DN_OPEN);
		        data.append(result.getName());
		        data.append(TAG_ENTRY_DN_CLOSE);
		        LOG.debug("Get the attributes for DN " + result.getName());
		        data.append(TAG_ENTRY_ATTRIBUTES_OPEN);
		        NamingEnumeration attributes = result.getAttributes().getAll();
		        while(attributes.hasMore()) {
		            Attribute attribute = (Attribute)attributes.next();
		            LOG.debug("Get the attribute " + attribute.getID());
		            data.append(TAG_ENTRY_ATTRIBUTE_OPEN);
		            data.append(TAG_ENTRY_ATTRIBUTE_NAME_OPEN);
		            data.append(attribute.getID());
		            data.append(TAG_ENTRY_ATTRIBUTE_NAME_CLOSE);
		            LOG.debug("Get the attribute " + attribute.getID() + " values");
		            data.append(TAG_ENTRY_ATTRIBUTE_VALUES_OPEN);
		            NamingEnumeration attributeValues = attribute.getAll();
		            while(attributeValues.hasMore()) {
		                String value = (String)attributeValues.next();
		                LOG.debug("Get the value " + value + " for the attribute " + attribute.getID());
		                data.append(TAG_ENTRY_ATTRIBUTE_VALUE_OPEN);
		                data.append(value);
		                data.append(TAG_ENTRY_ATTRIBUTE_VALUE_CLOSE);
		            }
		            data.append(TAG_ENTRY_ATTRIBUTE_VALUES_CLOSE);
		            data.append(TAG_ENTRY_ATTRIBUTE_CLOSE);
		        }
		        data.append(TAG_ENTRY_ATTRIBUTES_CLOSE);
		        data.append(TAG_ENTRY_CLOSE);
		    }
		} catch (NamingException namingException) {
		    LOG.error("Error while reading the LDAP result", namingException);
		    throw new MessagingException("Error while reading the LDAP result", namingException);
		}
		
		data.append(TAG_ENTRIES_CLOSE);
		
		message.setContent(new StringSource(data.toString()));
	}
	
}