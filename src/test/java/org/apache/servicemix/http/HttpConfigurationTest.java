package org.apache.servicemix.http;

import java.io.File;

import org.apache.servicemix.util.FileUtil;

import junit.framework.TestCase;

public class HttpConfigurationTest extends TestCase {

	private HttpConfiguration httpConfig;
	
	protected void setUp() throws Exception {
		super.setUp();
		httpConfig = new HttpConfiguration();
	}
	
	protected void tearDown() throws Exception {
		httpConfig = null;
	}
	
	// Test load() when rootDir is not set.
	public void testLoadNoRootDir() throws Exception {
		boolean isLoaded = httpConfig.load();
		assertTrue("HTTP Config should NOT be loaded with no root dir", !isLoaded);
	}
	
	// Test load() when config file does not exist.
	public void testLoadNoConfigFile() throws Exception {
		httpConfig.setRootDir("src/test/resources");
		assertTrue("HTTP Config should NOT be loaded with no config file", !httpConfig.load());
	}
	
	// Test save() (to config file) and load() (from config file).
	public void testSaveAndLoad() throws Exception {
		File rootDir = new File("target/httpConfig-test");
		if (!rootDir.exists())
			rootDir.mkdirs();
		
		httpConfig.setRootDir(rootDir.getAbsolutePath());
		
		// Save the HTTP Config (mostly default values)
		httpConfig.save();
		
		File configFile = new File(rootDir, HttpConfiguration.CONFIG_FILE);
		assertTrue("HTTP Config file should exist", configFile.exists());
		
		boolean isLoaded = httpConfig.load();
		
		assertTrue("HTTP Config should be loaded", isLoaded);		
		
		// clean up
		FileUtil.deleteFile(new File(httpConfig.getRootDir()));
	}
	
	// Test setMapping when input string does not begin with "/"
	public void testSetMappingBeginsNoSlash() throws Exception {
		String strMap = "pathHasNoSlash";
		httpConfig.setMapping(strMap);
		
		String actMap = httpConfig.getMapping();
		
		assertTrue("HTTP Config Mapping should begin with /", actMap.equals("/" + strMap));
	}
	
	// Test setMapping when input string ends with "/"
	public void testSetMappingEndsWithSlash() throws Exception {
		String strMap1 = "/pathEndsWithSlash";
		String strMap2 = "/";
		httpConfig.setMapping(strMap1 + strMap2);
		
		String actMap = httpConfig.getMapping();
		
		assertTrue("HTTP Config Mapping should not end with /", actMap.equals(strMap1));
	}
	
}
