package hudson.maven;

import hudson.EnvVars;
import hudson.maven.MavenModuleSet.DescriptorImpl;
import hudson.model.Result;
import hudson.tasks.Maven.MavenInstallation;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * @author Andrew Bayer
 */
public class MavenOptsTest extends AbstractMavenTestCase {
    DescriptorImpl d;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        d = jenkins.getDescriptorByType(DescriptorImpl.class);
        // Only in these tests we reset global MAVEN_OPTS
        d.setGlobalMavenOpts(null);
    }

    public void testEnvMavenOptsNoneInProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setAssignedLabel(createSlave(new EnvVars("MAVEN_OPTS", JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo")).getSelfLabel());
        
        buildAndAssertSuccess(m);

        assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }

    
    public void testEnvMavenOptsOverriddenByProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        m.setAssignedLabel(createSlave(new EnvVars("MAVEN_OPTS", JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo")).getSelfLabel());
        
        buildAndAssertSuccess(m);

        assertLogContains("[hudson.mavenOpt.test=bar]", m.getLastBuild());
    }

    public void testEnvAndGlobalMavenOptsOverriddenByProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        d.setGlobalMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        m.setAssignedLabel(createSlave(new EnvVars("MAVEN_OPTS", JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo")).getSelfLabel());
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=baz");

        buildAndAssertSuccess(m);

        assertLogContains("[hudson.mavenOpt.test=baz]", m.getLastBuild());
    }


    public void testGlobalMavenOpts() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        d.setGlobalMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        
        buildAndAssertSuccess(m);

        assertLogContains("[hudson.mavenOpt.test=bar]", m.getLastBuild());
    }

    public void testGlobalMavenOptsOverridenByProject() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        d.setGlobalMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=bar");
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -Dhudson.mavenOpt.test=foo");
       
        buildAndAssertSuccess(m);

        assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Bug(5651)
    public void testNewlinesInOptsRemoved() throws Exception {
        ToolInstallations.configureDefaultMaven("apache-maven-2.2.1", MavenInstallation.MAVEN_21);
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
	m.setScm(new ExtractResourceSCM(getClass().getResource("maven-surefire-unstable.zip")));
        m.setMavenOpts(JAVA_HEADLESS_OPT + " -XX:MaxPermSize=512m\r\n-Xms128m\r\n-Xmx512m");
        m.setGoals("install");
        
	assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
	MavenModuleSetBuild pBuild = m.getLastBuild();

	assertEquals("Parent build should have Result.UNSTABLE", Result.UNSTABLE, pBuild.getResult());
	
    }

    /**
     * Makes sure that environment variables in MAVEN_OPTS are properly expanded.
     */
    public void testEnvironmentVariableExpansion() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setMavenOpts(JAVA_HEADLESS_OPT + " $FOO");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        m.setGoals("validate");
        m.setAssignedLabel(createSlave(new EnvVars("FOO", "-Dhudson.mavenOpt.test=foo -Dhudson.mavenOpt.test2=bar")).getSelfLabel());

        buildAndAssertSuccess(m);

        assertLogContains("[hudson.mavenOpt.test=foo]", m.getLastBuild());
    }
    
    @Bug(13926)
    public void testMustnt_store_global_maven_opts_in_job_maven_opts() throws Exception {
        final String firstGlobalMavenOpts = "first maven opts";
        final String secondGlobalMavenOpts = "second maven opts";
        final String jobMavenOpts = "job maven opts";
        
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        
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

