package hudson.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import hudson.Launcher;
import hudson.maven.MavenBuildProxy.BuildCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BallColor;
import hudson.model.BuildListener;
import hudson.model.InvisibleAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.BuildWrapper.Environment;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;


import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenBuildTest {

    @Rule public JenkinsRule j = new MavenJenkinsRule();
    
    /**
     * NPE in {@code build.getProject().getWorkspace()} for {@link MavenBuild}.
     */
    @Bug(4192)
    @Test
    public void testMavenWorkspaceExists() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("HUDSON-4192.zip")));
        j.buildAndAssertSuccess(m);
    }
    
    /**
     * {@link Result} getting set to SUCCESS even if there's a test failure, when the test failure
     * does not happen in the final task segment.
     */
    @Bug(4177)
    @Test
    public void testTestFailureInEarlyTaskSegment() throws Exception {
        ToolInstallations.configureMaven3();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setGoals("clean install findbugs:findbugs");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-test-failure-findbugs.zip")));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    /**
     * Verify that a compilation error properly shows up as a failure.
     */
    @Test
    public void testCompilationFailure() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setGoals("clean install");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-compilation-failure.zip")));
        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
    }
    
    /**
     * Workspace determination problem on non-aggregator style build.
     */
    @Bug(4226)
    @Test
    public void testParallelModuleBuild() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("multimodule-maven.zip")));
        
        j.buildAndAssertSuccess(m);

        m.setAggregatorStyleBuild(false);

        // run module builds
        j.buildAndAssertSuccess(m.getModule("test$module1"));
        j.buildAndAssertSuccess(m.getModule("test$module1"));
    }
    
    @Bug(value=8395)
    @Test
    public void testMaven2BuildWrongScope() throws Exception {
        
        File pom = new File(this.getClass().getResource("test-pom-8395.xml").toURI());
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setRootPOM(pom.getAbsolutePath());
        m.setGoals( "clean validate" );
        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertFalse( mmsb.getProject().getModules().isEmpty());
    }    
    
    @Bug(value=8390)
    @Test
    public void testMaven2BuildWrongInheritence() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("incorrect-inheritence-testcase.zip")));
        m.setGoals( "clean validate" );
        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertFalse( mmsb.getProject().getModules().isEmpty());
    }   

    @Bug(value=8445)
    @Test
    public void testMaven2SeveralModulesInDirectory() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("several-modules-in-directory.zip")));
        m.setGoals( "clean validate" );
        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertFalse( mmsb.getProject().getModules().isEmpty());
    }    

    @Email("https://groups.google.com/d/msg/hudson-users/Xhw00UopVN0/FA9YqDAIsSYJ")
    @Test
    public void testMavenWithDependencyVersionInEnvVar() throws Exception {
        
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
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
    
    @Bug(8573)
    @Test
    public void testBuildTimeStampProperty() throws Exception {
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( mavenInstallation.getName() );
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-8573.zip")));
        m.setGoals( "process-resources" );
        j.buildAndAssertSuccess(m);
        String content = m.getLastBuild().getWorkspace().child( "target/classes/test.txt" ).readToString();
        assertFalse( content.contains( "${maven.build.timestamp}") );
        assertFalse( content.contains( "${maven.build.timestamp}") );

        System.out.println( "content " + content );
    }
    
    @Bug(value=15865)
    @Test
    public void testMavenFailsafePluginTestResultsAreRecorded() throws Exception {
        
        // GIVEN: a Maven project with maven-failsafe-plugin and Maven 2.2.1
        MavenModuleSet mavenProject = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        mavenProject.setMaven(mavenInstallation.getName());
        mavenProject.getReporters().add(new TestReporter());
        mavenProject.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-15865.zip")));
        mavenProject.setGoals( "clean install" );
        
        // WHEN project is build
        MavenModuleSetBuild mmsb = j.buildAndAssertSuccess(mavenProject);
        
        // THEN we have a testresult recorded
        AggregatedTestResultAction aggregatedTestResultAction = mmsb.getAction(AggregatedTestResultAction.class);
        assertNotNull(aggregatedTestResultAction);
        assertEquals(1, aggregatedTestResultAction.getTotalCount());
        
        Map<MavenModule, MavenBuild> moduleBuilds = mmsb.getModuleLastBuilds();
        assertEquals(1, moduleBuilds.size());
        MavenBuild moduleBuild = moduleBuilds.values().iterator().next();
         AbstractTestResultAction<?> testResultAction = moduleBuild.getAction(AbstractTestResultAction.class);
        assertNotNull(testResultAction);
        assertEquals(1, testResultAction.getTotalCount());
    }

    @Bug(18178)
    @Test
    public void testExtensionsConflictingWithCore() throws Exception {
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven(ToolInstallations.configureDefaultMaven().getName());
        m.setScm(new SingleFileSCM("pom.xml",
                "<project><modelVersion>4.0.0</modelVersion>" +
                "<groupId>g</groupId><artifactId>a</artifactId><version>0</version>" +
                "<build><extensions>" +
                "<extension><groupId>org.springframework.build.aws</groupId><artifactId>org.springframework.build.aws.maven</artifactId><version>3.0.0.RELEASE</version></extension>" +
                "</extensions></build></project>"));
        j.buildAndAssertSuccess(m);
    }

    @Bug(19801)
    @Test
    public void stopBuildAndAllSubmoduleBuilds() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet project = j.jenkins.createProject(MavenModuleSet.class, "p");
        project.setGoals("clean package");
        project.setScm(new ExtractResourceSCM(
                getClass().getResource("/hudson/maven/maven-multimod.zip")
        ));

        project.getReporters().add(new AbortInTheMiddleOfModuleB());

        MavenModuleSetBuild build = project.scheduleBuild2(0).get();

        j.assertBuildStatus(Result.ABORTED, build);
        assertFalse(build.isBuilding());

        j.assertBuildStatus(Result.SUCCESS, getModuleBuild(build, "moduleA"));
        j.assertBuildStatus(Result.ABORTED, getModuleBuild(build, "moduleB"));
        j.assertBuildStatus(Result.NOT_BUILT, getModuleBuild(build, "moduleC"));

        for (MavenBuild mb: build.getModuleLastBuilds().values()) {
            final String moduleName = mb.getParent().getDisplayName();
            assertFalse("Module " + moduleName + " is still building", mb.isBuilding());
        }
    }

    private MavenBuild getModuleBuild(MavenModuleSetBuild build, String name) {
        MavenModule module = build.getProject().getModule(
                "org.jvnet.hudson.main.test.multimod:" + name
        );
        return build.getModuleLastBuilds().get(module);
    }

    /** Abort build after ModuleB compile phase */
    private static class AbortInTheMiddleOfModuleB extends MavenReporter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {

            if ("moduleB".equals(pom.getArtifactId()) && "compile".equals(mojo.getGoal())) {
                stop(build);
            }

            return true;
        }

        private void stop(MavenBuildProxy build) throws IOException, InterruptedException {
            build.execute(new BuildCallable<Void, IOException>() {
                private static final long serialVersionUID = 1L;

                public Void call(MavenBuild build) throws InterruptedException, IOException {
                    try {

                        build.getParentBuild().doStop();
                        System.out.println("Stopped");
                    } catch (ServletException ex) {
                        throw new IOException(ex);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * This tests build a project with two modules. One that have tests failures
     * and the other don't compile due to a missing dependency.
     * 
     * @throws Exception
     */
    @Bug(16522)
    @Test
    public void testCorrectModuleBuildStatus() throws Exception {
        MavenModuleSet mavenProject = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        mavenProject.setMaven(mavenInstallation.getName());
        mavenProject.setScm(new ExtractResourceSCM(getClass().getResource("JENKINS-16522.zip")));
        mavenProject.setGoals( "clean install" );
        MavenModuleSetBuild build = mavenProject.scheduleBuild2(0).get();
        
        j.assertBuildStatus(Result.FAILURE, build);
        
        MavenModule moduleA = mavenProject.getModule("org.marcelo$moduleA");
        assertNotNull(moduleA);
        assertEquals(BallColor.YELLOW, moduleA.getIconColor());
        
        MavenModule moduleB = mavenProject.getModule("org.marcelo$moduleB");
        assertNotNull(moduleB);
        assertEquals(BallColor.RED, moduleB.getIconColor());
        
    }
    
    private static class TestReporter extends MavenReporter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            assertNotNull(build.getWorkspace());
            return true;
        }
    }    

    /**
     * This test makes sure the project actions of the prebuilders and postbuilders get requested (and therefore exposed).
     * 
     * @throws Exception
     */
    @Bug(20506)
    @Test
    public void testActionsOfPreAndPostBuildersMustBeExposed() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("multimodule-maven.zip")));
        m.setGoals("initialize");

        TestBuilder pre = new TestBuilder();
        TestBuilder post = new TestBuilder();
        assertFalse(pre.projectActionsGotRequested());
        assertFalse(post.projectActionsGotRequested());

        m.getPrebuilders().add(pre);
        m.getPostbuilders().add(post);
        
        j.buildAndAssertSuccess(m);
        
        assertTrue("actions of prebuilders have not been requested during build",pre.projectActionsGotRequested());
        assertTrue("actions of postbuilders have not been requested during build",post.projectActionsGotRequested());
        
        final TestAction action = m.getAction(TestAction.class);
        assertNotNull(action);
        final List<TestAction> actions = m.getActions(TestAction.class);
        assertEquals(2, actions.size());
    }     

    @Test
    public void testBuildWrappersTeardown() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("multimodule-maven.zip")));
        m.setGoals("initialize");

        TearingDownBuildWrapper testBuild1Wrapper = new TearingDownBuildWrapper();
        TearingDownBuildWrapper testBuild2Wrapper = new TearingDownBuildWrapper();
        m.getBuildWrappersList().addAll(Arrays.asList(testBuild1Wrapper, testBuild2Wrapper, new FailingBuildWrapper()));

        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());

        assertTrue(testBuild1Wrapper.tearDown);
        assertTrue(testBuild2Wrapper.tearDown);
    } 
    
    private static class TestBuilder extends Builder {

        private boolean projectActionsGotRequested = false;

        public Action getProjectAction(AbstractProject<?, ?> project) {
            return null;
        }

        public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
            this.projectActionsGotRequested = true;
            return Collections.singletonList(new TestAction());
        }

        public boolean projectActionsGotRequested() {
            return projectActionsGotRequested;
        }

    }
    
    private static class TestAction extends InvisibleAction{
    }

    private static class FailingBuildWrapper extends BuildWrapper {
        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return null;
        }
    }

    private static class TearingDownBuildWrapper extends BuildWrapper {
        public boolean tearDown;

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return new Environment() {
                public boolean tearDown(AbstractBuild build, BuildListener listener) {
                    tearDown = true;

                    return true;
                }
            };
        }
    }
}
