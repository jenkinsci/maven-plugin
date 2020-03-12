package hudson.maven;

/*
 * Olivier Lamy
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * @author Olivier Lamy
 */
public class Maven36xBuildTest
    extends AbstractMaven3xBuildTest {

    @Override
    public MavenInstallation configureMaven3x()
        throws Exception
    {
        return configureMaven36();
    }

    public static Maven.MavenInstallation configureMaven36() throws Exception {
        Maven.MavenInstallation mvn = ToolInstallations.configureDefaultMaven("apache-maven-3.6.3", MavenInstallation.MAVEN_30);

        Maven.MavenInstallation m3 = new Maven.MavenInstallation( "apache-maven-3.6.3", mvn.getHome(), JenkinsRule.NO_PROPERTIES);
        Jenkins.getInstance().getDescriptorByType( Maven.DescriptorImpl.class).setInstallations( m3);
        return m3;
    }

    int getTychoEclipseTestResultsCount() {
        return 2;
    }
}
