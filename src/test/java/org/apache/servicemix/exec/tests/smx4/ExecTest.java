/**
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
package org.apache.servicemix.exec.tests.smx4;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.profile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.localRepository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * Test the Exec component deployment into the NMR.
 * </p>
 * 
 * @author jbonofre
 */
@RunWith(JUnit4TestRunner.class)
public class ExecTest {
    
    @Inject
    protected BundleContext bundleContext;

    @Test
    public void test() throws Exception {
        MavenArtifactProvisionOption execUrl = CoreOptions.mavenBundle().groupId("org.apache.servicemix").artifactId("servicemix-exec").versionAsInProject();
        Bundle execBundle = bundleContext.installBundle(execUrl.getURL());
        System.out.println(execBundle);
    }

    @Configuration
    public static Option[] configuration() {
        Option[] options = options(
                // this is how you set the default log level when using pax logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
                systemProperty("karaf.name").value("root"),
                systemProperty("karaf.home").value("target/karaf.home"),
                systemProperty("karaf.base").value("target/karaf.home"),
                systemProperty("karaf.startLocalConsole").value("false"),
                systemProperty("karaf.startRemoteShell").value("false"),
                
                // define maven repository
                repositories(
                        "http://repo1.maven.org/maven2", 
                        "http://repository.apache.org/content/groups/snapshots-group",
                        "http://repository.ops4j.org/maven2",
                        "http://svn.apache.org/repos/asf/servicemix/m2-repo",
                        "http://repository.springsource.com/maven/bundles/release",
                        "http://repository.springsource.com/maven/bundles/external"
                        ),
                
                // hack system packages
                systemPackages("org.apache.felix.karaf.jaas.boot;version=1.99"),
                bootClasspathLibrary(mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.boot")).afterFramework(),
                bootClasspathLibrary(mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.main")).afterFramework(),
                
                // log
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
                // felix config admin
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                // felix preference service
                mavenBundle("org.apache.felix", "org.apache.felix.prefs"),
                // blueprint
                mavenBundle("org.apache.geronimo.blueprint", "geronimo-blueprint"),
                
                // bundles
                mavenBundle("org.apache.mina", "mina-core"),
                mavenBundle("org.apache.sshd", "sshd-core"),
                mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.config"),
                mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.console"),
                mavenBundle("org.apache.felix.gogo", "org.apache.felix.gogo.runtime"),
                mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.osgi"),
                mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.log").noStart(),
                
                // load karaf feature (obr, wrapper)
                scanFeatures("mvn:org.apache.felix.karaf/apache-felix-karaf/1.2.0/xml/features", "obr", "wrapper"),
                
                // load smx nmr feature (jbi)
                scanFeatures("mvn:org.apache.servicemix.nmr/apache-servicemix-nmr/1.0.0/xml/features", "jbi"),
                
                // start felix framework
                felix());
        return options;
    }

}
