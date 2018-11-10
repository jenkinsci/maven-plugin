package hudson.maven;

import hudson.EnvVars;
import hudson.maven.MavenModuleSet.DescriptorImpl;
import hudson.model.Result;
import hudson.tasks.Maven.MavenInstallation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import static hudson.maven.MavenJenkinsRule.JAVA_HEADLESS_OPT;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * @author Andrew Bayer
 */
public class MavenOptsTest {

    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    @Test
    public void testEnvMavenOptsNoneInProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setAssignedLabel(j.createSlave(new EnvVars("MAVEN_OPTS", JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo")).getSelfLabel());
        
        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }

    @Test
    public void testEnvMavenOptsOverriddenByProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        m.setAssignedLabel(j.createSlave(new EnvVars("MAVEN_OPTS", JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo")).getSelfLabel());

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=bar]", m.getLastBuild());
    }

    @Test
    public void testEnvAndGlobalMavenOptsOverriddenByProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        //m.setGlobalMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        m.setAssignedLabel(j.createSlave(new EnvVars("MAVEN_OPTS", JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo")).getSelfLabel());
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=baz");

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=baz]", m.getLastBuild());
    }

    @Test
    public void testGlobalMavenOpts() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        //d.setGlobalMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=bar]", m.getLastBuild());
    }

    @Test
    public void testGlobalMavenOptsOverridenByProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        //d.setGlobalMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo");

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Bug(5651)
    @Test
    public void testNewlinesInOptsRemoved() throws Exception {
        ToolInstallations.configureDefaultMaven("apache-maven-2.2.1", MavenInstallation.MAVEN_21);
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
	    m.setScm(new ExtractResourceSCM(getClass().getResource("maven-surefire-unstable.zip")));
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -XX:MaxPermSize=512m\r\n-Xms128m\r\n-Xmx512m");
        m.setGoals("install");

        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
	    MavenModuleSetBuild pBuild = m.getLastBuild();

	    assertEquals("Parent build should have Result.UNSTABLE", Result.UNSTABLE, pBuild.getResult());
	
    }

    /**
     * Makes sure that environment variables in MAVEN_OPTS are properly expanded.
     */
    @Test
    public void testEnvironmentVariableExpansion() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMavenOpts(JAVA_HEADLESS_OPT + " $FOO");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setAssignedLabel(j.createSlave(new EnvVars("FOO", "-Dhudson.mavenOpt.test=foo -Dhudson.mavenOpt.test2=bar")).getSelfLabel());

        j.buildAndAssertSuccess(m);

        j.assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Bug(13926)
    @Test
    public void testMustnt_store_global_maven_opts_in_job_maven_opts() throws Exception {
        final String firstGlobalMavenOpts = "first maven opts";
        final String secondGlobalMavenOpts = "second maven opts";
        final String jobMavenOpts = "job maven opts";
        
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        
        assertNull(m.getMavenOpts());
        
        //d.setGlobalMavenOpts(firstGlobalMavenOpts);
        assertEquals(firstGlobalMavenOpts, m.getMavenOpts());
        
        m.setMavenOpts(firstGlobalMavenOpts);
        assertEquals(firstGlobalMavenOpts, m.getMavenOpts());
        
        //d.setGlobalMavenOpts(secondGlobalMavenOpts);
        assertEquals(secondGlobalMavenOpts, m.getMavenOpts());
        
        m.setMavenOpts(jobMavenOpts);
        assertEquals(jobMavenOpts, m.getMavenOpts());
        
        //d.setGlobalMavenOpts(firstGlobalMavenOpts);
        m.setMavenOpts(firstGlobalMavenOpts);
        assertEquals(firstGlobalMavenOpts, m.getMavenOpts());
        
        //d.setGlobalMavenOpts(secondGlobalMavenOpts);
        assertEquals(secondGlobalMavenOpts, m.getMavenOpts());
    }

}

