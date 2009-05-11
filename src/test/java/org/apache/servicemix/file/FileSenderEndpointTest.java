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
package org.apache.servicemix.file;

import java.io.File;
import java.io.IOException;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.framework.ComponentNameSpace;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.servicedesc.InternalEndpoint;
import org.apache.servicemix.util.FileUtil;
import org.apache.servicemix.tck.mock.MockExchangeFactory;

public class FileSenderEndpointTest extends TestCase {

	private static final File OUT_DIR = new File("target/file-test");
	private static final File OUT_FILE = new File(OUT_DIR, "file-exists.tmp");
    public static final String FILE_NAME_PROPERTY = "org.apache.servicemix.file.name";
    public static final String FILE_PATH_PROPERTY = "org.apache.servicemix.file.path";
    private FileSenderEndpoint endpoint;
    private FileComponent component;
    private ComponentNameSpace cns;
    private InternalEndpoint ie;

    public FileSenderEndpointTest(final String name) {
        super(name);
    }

    protected final void setUp() throws Exception {
        super.setUp();
        component = new FileComponent();
        cns = new ComponentNameSpace("myContainer", "myName");
        ie = new InternalEndpoint(cns, "myEndpoint", new QName("myService"));
        endpoint = new FileSenderEndpoint(component, ie);
    }

    protected final void tearDown() throws Exception {
        super.tearDown();

        component = null;
        cns = null;
        ie = null;
        endpoint = null;
    }

    // Test when no directory has been set on the endpoint.
    public final void testValidateNoDirectory() throws Exception {
        try {
            endpoint.validate();
            fail("validate() should fail when no directory is specified.");
        } catch (DeploymentException de) {
            // test succeeds
        }
    }

    // Test when directory specified is not a directory.
    public final void testValidateIsNotDirectory() throws Exception {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("servicemix", "test");
            endpoint.setDirectory(tempFile);
            endpoint.validate();
            fail("validate() should fail when setDirectory is set to a file.");
        } catch (DeploymentException de) {
            // test succeeds
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }
    
    public final void testValidateOverwriteAndAppend() throws Exception {
    	endpoint.setDirectory(OUT_DIR);
    	endpoint.setAppend(true);
    	endpoint.setOverwrite(true);
    	try {
    		endpoint.validate();
    		fail("validate() should fail when isAppend and isOverwrite are both true.");
    	} catch (DeploymentException de) {
    		// test succeeds
    	}
    }

    // Test when the output file does not exist.
    public final void testProcessInOnlyNewFile()
        throws Exception {
        MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setOperation(new QName("uri", "op"));
        me.setProperty("myProp", "myValue");
        NormalizedMessage msg = me.createMessage();
        msg.setProperty("myMsgProp", "myMsgValue");
        msg.setContent(new StringSource("<input>input message</input>"));
        endpoint.setDirectory(OUT_DIR);
        endpoint.setAutoCreateDirectory(true);
        endpoint.validate();

        endpoint.processInOnly(me, msg);

        // Check to see if a file was written to the directory.
        File[] fileList = endpoint.getDirectory().listFiles();
        assertTrue("Directory should not be empty", fileList != null);

        // cleanup
        FileUtil.deleteFile(endpoint.getDirectory());
    }
    
    // Test when output file exists but neither append nor overwrite are set.
    public final void testProcessInOnlyFileExistsNoAppendNoOverwrite()
        throws Exception {
        MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setOperation(new QName("uri", "op"));
        me.setProperty("myProp", "myValue");
        NormalizedMessage msg = me.createMessage();
        msg.setProperty(FILE_PATH_PROPERTY, OUT_FILE.getAbsolutePath());
        msg.setProperty(FILE_NAME_PROPERTY, OUT_FILE.getName());
        msg.setContent(new StringSource("<input>input message</input>"));
        endpoint.setDirectory(OUT_DIR);
        endpoint.setAutoCreateDirectory(true);
        endpoint.validate();
        
        // Create the initial file for later use.
        endpoint.processInOnly(me, msg);
        
        endpoint.setAppend(false);
        endpoint.setOverwrite(false);
        
        try {
        	endpoint.processInOnly(me, msg);
        	fail("processInOnly() should fail when file exists but append is false and " +
        			"overwrite is false");
        } catch (IOException ioe) {
        	// test succeeds - file exists but cannot be appended to or overwritten
        } finally {
        	FileUtil.deleteFile(OUT_FILE);
        }
    }
    
    // Test when output file exists and overwrite is true.
    public final void testProcessInOnlyFileExistsOverwrite() throws Exception {
        MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setOperation(new QName("uri", "op"));
        me.setProperty("myProp", "myValue");
        NormalizedMessage msg = me.createMessage();
        msg.setProperty(FILE_PATH_PROPERTY, OUT_FILE.getAbsolutePath());
        msg.setProperty(FILE_NAME_PROPERTY, OUT_FILE.getName());
        msg.setContent(new StringSource("<input>input message</input>"));
        endpoint.setDirectory(OUT_DIR);
        endpoint.setAutoCreateDirectory(true);
        endpoint.validate();
        
        // Create the initial file for later use.
        endpoint.processInOnly(me, msg);
        
        long fileLength = OUT_FILE.length();
        
        endpoint.setOverwrite(true);
        endpoint.setAppend(false);
        
        endpoint.processInOnly(me, msg);
        
        assertTrue("File was not overwritten: " + OUT_FILE.getAbsolutePath(), OUT_FILE.length() == fileLength);
        
        // clean up
        FileUtil.deleteFile(OUT_FILE);
    }

    // Test when output file exists and append is true.
    public final void testProcessInOnlyFileExistsAppend() throws Exception {
        MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOnlyExchange();
        me.setOperation(new QName("uri", "op"));
        me.setProperty("myProp", "myValue");
        NormalizedMessage msg = me.createMessage();
        msg.setProperty(FILE_PATH_PROPERTY, OUT_FILE.getAbsolutePath());
        msg.setProperty(FILE_NAME_PROPERTY, OUT_FILE.getName());
        msg.setContent(new StringSource("<input>input message</input>"));
        endpoint.setDirectory(OUT_DIR);
        endpoint.setAutoCreateDirectory(true);
        endpoint.validate();
        
        // Create the initial file for later use.
        endpoint.processInOnly(me, msg);
        
        long fileLength = OUT_FILE.length();
        
        endpoint.setOverwrite(false);
        endpoint.setAppend(true);
        
        endpoint.processInOnly(me, msg);
        
        assertTrue("File was not overwritten: " + OUT_FILE.getAbsolutePath(), OUT_FILE.length() > fileLength);
        
        // clean up
        FileUtil.deleteFile(OUT_FILE);
    }
    
    // Test when calling processInOut - not supported.
    public final void testProcessInOutNotSupported() throws Exception {
        MockExchangeFactory mef = new MockExchangeFactory();
        MessageExchange me = mef.createInOutExchange();
        me.setOperation(new QName("uri", "op"));
        me.setProperty("myProp", "myValue");
        NormalizedMessage inMsg = me.createMessage();
        inMsg.setProperty("myMsgProp", "myMsgValue");
        inMsg.setContent(new StringSource("<input>input message</input>"));
        NormalizedMessage outMsg = me.createMessage();
        outMsg.setContent(new StringSource("<output>output message</output>"));
        endpoint.setDirectory(new File("target/test"));

        try {
            endpoint.processInOut(me, inMsg, outMsg);
            fail("processInOut is an unsupported operation");
        } catch (UnsupportedOperationException uoe) {
            // test succeeds
        }
    }

}
