package hudson.maven;

import hudson.EnvVars;
import hudson.maven.MavenModuleSet.DescriptorImpl;
import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Andrew Bayer
 */
@WithJenkins
class MavenOptsTest {

    private DescriptorImpl d;

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;

        d = j.jenkins.getDescriptorByType(DescriptorImpl.class);
        // Only in these tests we reset global MAVEN_OPTS
        d.setGlobalMavenOpts(null);
    }

    @Test
    void testEnvMavenOptsNoneInProject() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setAssignedLabel(j.createSlave(new EnvVars("MAVEN_OPTS", "-Dhudson.mavenOpt.test=foo")).getSelfLabel());
        
        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Test
    void testEnvMavenOptsOverriddenByProject() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setMavenOpts("-Dhudson.mavenOpt.test=bar");
        m.setAssignedLabel(j.createSlave(new EnvVars("MAVEN_OPTS", "-Dhudson.mavenOpt.test=foo")).getSelfLabel());
        
        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=bar]", m.getLastBuild());
    }

    @Test
    void testEnvAndGlobalMavenOptsOverriddenByProject() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        d.setGlobalMavenOpts("-Dhudson.mavenOpt.test=bar");
        m.setAssignedLabel(j.createSlave(new EnvVars("MAVEN_OPTS", "-Dhudson.mavenOpt.test=foo")).getSelfLabel());
        m.setMavenOpts("-Dhudson.mavenOpt.test=baz");

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=baz]", m.getLastBuild());
    }


    @Test
    void testGlobalMavenOpts() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        d.setGlobalMavenOpts("-Dhudson.mavenOpt.test=bar");
        
        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=bar]", m.getLastBuild());
    }

    @Test
    void testGlobalMavenOptsOverridenByProject() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        d.setGlobalMavenOpts("-Dhudson.mavenOpt.test=bar");
        m.setMavenOpts("-Dhudson.mavenOpt.test=foo");
       
        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Issue("JENKINS-5651")
    @Test
    void testNewlinesInOptsRemoved() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
	    m.setScm(new ExtractResourceSCM(getClass().getResource("maven-surefire-unstable.zip")));
        m.setMavenOpts("-Xms128m\r\n-Xmx512m");
        m.setGoals("install -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        
	    j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
	    MavenModuleSetBuild pBuild = m.getLastBuild();

	    assertEquals(Result.UNSTABLE, pBuild.getResult(), "Parent build should have Result.UNSTABLE");
	
    }

    /**
     * Makes sure that environment variables in MAVEN_OPTS are properly expanded.
     */
    @Test
    void testEnvironmentVariableExpansion() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMavenOpts("$FOO");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setAssignedLabel(j.createSlave(new EnvVars("FOO", "-Dhudson.mavenOpt.test=foo -Dhudson.mavenOpt.test2=bar")).getSelfLabel());

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Issue("JENKINS-13926")
    @Test
    void testMust_not_store_global_maven_opts_in_job_maven_opts() throws Exception {
        final String firstGlobalMavenOpts = "first maven opts";
        final String secondGlobalMavenOpts = "second maven opts";
        final String jobMavenOpts = "job maven opts";

        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        
        assertNull(m.getMavenOpts());
        
        d.setGlobalMavenOpts(firstGlobalMavenOpts);
        assertEquals(firstGlobalMavenOpts, m.getMavenOpts());
        
        m.setMavenOpts(firstGlobalMavenOpts);
        assertEquals(firstGlobalMavenOpts, m.getMavenOpts());
        
        d.setGlobalMavenOpts(secondGlobalMavenOpts);
        assertEquals(secondGlobalMavenOpts, m.getMavenOpts());
        
        m.setMavenOpts(jobMavenOpts);
        assertEquals(jobMavenOpts, m.getMavenOpts());
        
        d.setGlobalMavenOpts(firstGlobalMavenOpts);
        m.setMavenOpts(firstGlobalMavenOpts);
        assertEquals(firstGlobalMavenOpts, m.getMavenOpts());
        
        d.setGlobalMavenOpts(secondGlobalMavenOpts);
        assertEquals(secondGlobalMavenOpts, m.getMavenOpts());
    }

}

