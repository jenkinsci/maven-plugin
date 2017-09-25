package hudson.maven;

/*
 * Copyright 2013 Olivier Lamy
 *
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


import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Olivier Lamy
 */
public class MavenUtilTest
{
    @Test
    public void testMaven2(){
        Assert.assertEquals( MavenUtil.MavenVersion.MAVEN_2, MavenUtil.getMavenVersion( "2.2.1" ) );
    }

    @Test
    public void testMaven301(){
        Assert.assertEquals( MavenUtil.MavenVersion.MAVEN_3_0_X, MavenUtil.getMavenVersion( "3.0.1" ) );
    }

    @Test
    public void testMaven31(){
        Assert.assertEquals( MavenUtil.MavenVersion.MAVEN_3_1, MavenUtil.getMavenVersion( "3.1.0" ) );
    }

    @Test
    public void eventSpy2x(){
        Assert.assertFalse( MavenUtil.supportEventSpy( "2.2.1" ) );
    }

    @Test
    public void eventSpy301(){
        Assert.assertFalse( MavenUtil.supportEventSpy( "3.0.1" ) );
    }

    @Test
    public void eventSpy302(){
        Assert.assertFalse( MavenUtil.supportEventSpy( "3.0.2" ) );
    }

    @Test
    public void eventSpy31x(){
        Assert.assertTrue( MavenUtil.supportEventSpy( "3.1.0" ) );
    }

    private MavenProject project;

    @Before
    public void setUp() {
        project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId("testmodule");
        project.setVersion("2.0-SNAPSHOT");
        project.setPackaging("jar");
    }

    @Test
    public void resolveVersionPlain() {
        Assert.assertTrue("1.2.3".equals(MavenUtil.resolveVersion("1.2.3", project)));
    }

    @Test
    public void resolveVersionComplex() {
        Assert.assertNull(MavenUtil.resolveVersion("1.${abc}.3", project));
        Assert.assertNull(MavenUtil.resolveVersion("1.${abc}", project));
        Assert.assertNull(MavenUtil.resolveVersion("${abc}.3", project));
    }

    @Test
    public void resolveVersionMissingProperty() {
        Assert.assertNull(MavenUtil.resolveVersion("${abc}", project));
    }

    @Test
    public void resolveVersionProperty() {
        project.getProperties().setProperty("abc", "1.2.3");
        Assert.assertTrue("1.2.3".equals(MavenUtil.resolveVersion("${abc}", project)));
    }
}
