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

import hudson.Launcher;
import hudson.maven.reporters.SurefireReport;
import hudson.model.BuildListener;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.ExtractResourceSCM;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 * @author Olivier Lamy
 */
public abstract class AbstractMaven3xBuildTest {

    @Rule
    public MavenJenkinsRule j = new MavenJenkinsRule();

    public abstract MavenInstallation configureMaven3x() throws Exception;

    @Test
    public void testSimpleMaven3Build() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven3-project.zip")));
        m.setGoals( "clean install" );
        MavenModuleSetBuild b = j.buildAndAssertSuccess(m);
        assertTrue( MavenUtil.maven3orLater( b.getMavenVersionUsed() ) );
    }

    @Test
    public void testSimpleMaven3BuildRedeployPublisher() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        m.setMaven( mavenInstallation.getName() );
        File repo = j.createTmpDir();
        FileUtils.cleanDirectory( repo );
        m.getReporters().add(new TestReporter());
        m.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),true, false));
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven3-project.zip")));
        m.setGoals( "clean install" );
        MavenModuleSetBuild b = j.buildAndAssertSuccess(m);
        assertTrue( MavenUtil.maven3orLater( b.getMavenVersionUsed() ) );
        File artifactDir = new File(repo,"com/mycompany/app/my-app/1.7-SNAPSHOT/");
        String[] files = artifactDir.list( new FilenameFilter()
        {
            
            public boolean accept( File dir, String name )
            {
                System.out.println("file name : " +name );
                return name.endsWith( ".jar" );
            }
        });
        assertTrue("SNAPSHOT exist",!files[0].contains( "SNAPSHOT" ));
        assertTrue("file not ended with -1.jar", files[0].endsWith( "-1.jar" ));
    }

    @Ignore("TODO long failing in 3.0.x with NPE in DefaultDependencyGraphBuilder.canFindCoreClass after failing to resolve an artifact from the test project; started failing in 3.1.x with ClassNotFoundException: org.apache.maven.doxia.siterenderer.DocumentContent; and 3.3.9 and 3.5.x are ignored overall, so not even tested there")
    @Test
    public void testSiteBuildWithForkedMojo() throws Exception {
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        m.setMaven( mavenInstallation.getName() );        
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven3-project.zip")));
        m.setGoals("-e clean site");
        MavenModuleSetBuild b = j.buildAndAssertSuccess(m);
        assertTrue( MavenUtil.maven3orLater( b.getMavenVersionUsed() ) );
    }

    @Test
    @Bug(value=8395)
    public void testMaven3BuildWrongScope() throws Exception {
        
        File pom = new File(this.getClass().getResource("test-pom-8395.xml").toURI());
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setRootPOM(pom.getAbsolutePath());
        m.setGoals( "clean validate" );
        MavenModuleSetBuild mmsb =  m.scheduleBuild2( 0 ).get();
        j.assertBuildStatus( Result.FAILURE, mmsb );
        System.out.println("mmsb.getProject().getModules " + mmsb.getProject().getModules() );
        assertTrue( mmsb.getProject().getModules().isEmpty());
    }

    @Test
    @Bug(value=8390)
    public void testMaven3BuildWrongInheritence() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("incorrect-inheritence-testcase.zip")));
        m.setGoals( "clean validate" );
        MavenModuleSetBuild mmsb =  m.scheduleBuild2( 0 ).get();
        j.assertBuildStatus( Result.FAILURE, mmsb );
        System.out.println("mmsb.getProject().getModules " + mmsb.getProject().getModules() );
        assertTrue( mmsb.getProject().getModules().isEmpty());
    }

    @Test
    @Bug(value=8445)
    public void testMavenSeveralModulesInDirectory() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("several-modules-in-directory.zip")));
        m.setGoals( "clean validate" );
        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertFalse( mmsb.getProject().getModules().isEmpty());
    }

    @Test
    @Email("https://groups.google.com/d/msg/hudson-users/Xhw00UopVN0/FA9YqDAIsSYJ")
    public void testMavenWithDependencyVersionInEnvVar() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = configureMaven3x();
        ParametersDefinitionProperty parametersDefinitionProperty = 
            new ParametersDefinitionProperty(new StringParameterDefinition( "JUNITVERSION", "3.8.2" ));
        
        m.addProperty( parametersDefinitionProperty );
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("envars-maven-project.zip")));
        m.setGoals( "clean test-compile" );
        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertFalse( mmsb.getProject().getModules().isEmpty());
    }

    @Test
    @Bug(8484)
    public void testMultiModMavenNonRecursive() throws Exception {
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimod.zip")));
        m.setGoals( "-N validate" );
        assertTrue("MavenModuleSet.isNonRecursive() should be true", m.isNonRecursive());
        j.buildAndAssertSuccess(m);
        assertEquals("not only one module", 1, m.getModules().size());
    }

    @Test
    @Bug(8573)
    public void testBuildTimeStampProperty() throws Exception {
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-8573.zip")));
        m.setGoals( "process-resources" );
        j.buildAndAssertSuccess(m);
        String content = m.getLastBuild().getWorkspace().child( "target/classes/test.txt" ).readToString();
        assertFalse( content.contains( "${maven.build.timestamp}") );
        assertFalse( content.contains( "${maven.build.timestamp}") );
    }

    @Test
    @Bug(1557)
    public void testDuplicateTestResults() throws Exception {
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-1557.zip")));
        m.setGoals("verify");
        j.buildAndAssertSuccess(m);

        int totalCount = m.getModules().iterator().next()
                .getAction(TestResultProjectAction.class).getLastTestResultAction().getTotalCount();
        assertEquals(4, totalCount);
    }

    @Test
    @Issue("JENKINS-9326")
    // Because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=340852 this test is failing
    // if you maven installation (MAVEN_HOME) is using a symbolic link
    public void testTychoTestResults() throws Exception {
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setRootPOM( "org.foobar.build/pom.xml" );
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-9326.zip"),"foobar"));
        m.setGoals("verify");
        j.buildAndAssertSuccess(m);

        System.out.println("modules size " + m.getModules());


        MavenModule testModule = null;
        for (MavenModule mavenModule : m.getModules()) {
            System.out.println("module " + mavenModule.getName() + "/" + mavenModule.getDisplayName());
            if ("org.foobar:org.foobar.test".equals( mavenModule.getName() )) testModule = mavenModule;
        }

        AbstractTestResultAction trpa = testModule.getLastBuild().getAction(AbstractTestResultAction.class);

        int totalCount = trpa.getTotalCount();
        assertEquals(2, totalCount);
    }

    @Test
    @Bug(9326)
    public void testTychoEclipseTestResults() throws Exception {
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setRootPOM( "org.foobar.build/pom.xml" );
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("foobar_eclipse_with_fix.zip"),"foobar_eclipse"));
        m.setGoals("verify");
        j.buildAndAssertSuccess(m);

        System.out.println("modules size " + m.getModules().size());

        MavenModule testModule = null;
        for (MavenModule mavenModule : m.getModules()) {
            System.out.println("module '" + mavenModule.getName() + "/" + mavenModule.getDisplayName() + "'");
            if ("org.foobar:org.foobar.test".equals( mavenModule.getName() )) testModule = mavenModule;
        }

        AbstractTestResultAction trpa = testModule.getLastBuild().getAction(AbstractTestResultAction.class);
        int totalCount = trpa.getTotalCount();
        assertEquals(getTychoEclipseTestResultsCount(), totalCount);
    }

    int getTychoEclipseTestResultsCount() {
        return 1;
    }

    @Test
    @Bug(11964)
    public void testSingleModuleBuild() throws Exception {
        // Given a multi-module build.
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimod.zip")));
        m.setGoals("verify");
        j.buildAndAssertSuccess(m);

        // When building a single module within the given build.
        MavenModule singleModule = m.getModule("org.jvnet.hudson.main.test.multimod$moduleA");
        MavenBuild isolated = j.buildAndAssertSuccess(singleModule);

        // Then only the single module should have been built.
        Collection<MavenModule> expectedNonBuiltModules = new ArrayList<MavenModule>(m.getModules());
        expectedNonBuiltModules.remove(singleModule);

        assertEquals(2, isolated.number);
        for(MavenModule module : expectedNonBuiltModules) {
            assertEquals(1,module.getLastBuild().getNumber());
        }
        
        // AbstractBuild#getRootBuild() never be null.
        assertNotNull(isolated.getRootBuild());
        // as there's no parent build, rootBuild is itself.
        assertEquals(isolated, isolated.getRootBuild());
    }

    @Test
    @Bug(12109)
    public void testMultiModuleInvalidRecursivePom() throws Exception {
        MavenInstallation mavenInstallation = configureMaven3x();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "JENKINS-12109");
        m.setMaven(mavenInstallation.getName());
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-12109.zip")));
        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
        j.jenkins.reload();
        assertNotNull(j.jenkins.getItemByFullName("JENKINS-12109"));
    }

    private static class TestReporter extends MavenReporter {
        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            assertNotNull(build.getProject().getWorkspace());
            assertNotNull(build.getWorkspace());
            return true;
        }
    }
    
}
