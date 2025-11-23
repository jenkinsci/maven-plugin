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

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Olivier Lamy
 */
class MavenUtilTest {

    private MavenProject project;

    @BeforeEach
    void beforeEach() {
        project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId("testmodule");
        project.setVersion("2.0-SNAPSHOT");
        project.setPackaging("jar");
    }

    @Test
    void testMaven2(){
        assertEquals(MavenUtil.MavenVersion.MAVEN_2, MavenUtil.getMavenVersion("2.2.1"));
    }

    @Test
    void testMaven301(){
        assertEquals(MavenUtil.MavenVersion.MAVEN_3_0_X, MavenUtil.getMavenVersion("3.0.1"));
    }

    @Test
    void testMaven31(){
        assertEquals(MavenUtil.MavenVersion.MAVEN_3_1, MavenUtil.getMavenVersion("3.1.0"));
    }

    @Test
    void eventSpy2x(){
        assertFalse(MavenUtil.supportEventSpy("2.2.1"));
    }

    @Test
    void eventSpy301(){
        assertFalse(MavenUtil.supportEventSpy("3.0.1"));
    }

    @Test
    void eventSpy302(){
        assertFalse(MavenUtil.supportEventSpy("3.0.2"));
    }

    @Test
    void eventSpy31x(){
        assertTrue(MavenUtil.supportEventSpy("3.1.0"));
    }

    @Test
    void resolveVersionPlain() {
        assertEquals("1.2.3", MavenUtil.resolveVersion("1.2.3", project));
    }

    @Test
    void resolveVersionComplex() {
        assertNull(MavenUtil.resolveVersion("1.${abc}.3", project));
        assertNull(MavenUtil.resolveVersion("1.${abc}", project));
        assertNull(MavenUtil.resolveVersion("${abc}.3", project));
    }

    @Test
    void resolveVersionMissingProperty() {
        assertNull(MavenUtil.resolveVersion("${abc}", project));
    }

    @Test
    void resolveVersionProperty() {
        project.getProperties().setProperty("abc", "1.2.3");
        assertEquals("1.2.3", MavenUtil.resolveVersion("${abc}", project));
    }
}
