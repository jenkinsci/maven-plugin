package hudson.maven;

import hudson.model.Result;
import hudson.tasks.Shell;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RunLoadCounter;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Olivier Lamy
 */
@WithJenkins
class MavenBuildSurefireFailedTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Issue("JENKINS-8415")
    @Test
    void testMaven2Unstable() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setGoals("test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    @Issue("JENKINS-8415")
    @Test
    void testMaven3Failed() throws Exception {
        Maven36xBuildTest.configureMaven36();
        final MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setGoals("test -Dmaven.test.failure.ignore=false -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
        // JENKINS-18895:
        MavenModule failing = m.getModule("com.mycompany.app:my-app");
        assertEquals(Result.FAILURE, failing.getLastBuild().getResult());
        RunLoadCounter.prepare(failing);
        assertEquals(Result.FAILURE, RunLoadCounter.assertMaxLoads(failing, 0, (Callable<Result>) () -> m.getLastBuild().getResult()));
    }   
    
    @Issue("JENKINS-8415")
    @Test
    void testMaven3Unstable() throws Exception {
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMaven(Maven36xBuildTest.configureMaven36().getName());
        m.setGoals("test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    @Issue("JENKINS-8415")
    @Test
    void testMaven3Failure() throws Exception {
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMaven(Maven36xBuildTest.configureMaven36().getName());
        m.setGoals("test -Dmaven.test.failure.ignore=false -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
    }    
    
    @Issue("JENKINS-14102")
    @Test
    void testMaven3SkipPostBuilder() throws Exception {
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMaven(Maven36xBuildTest.configureMaven36().getName());
        m.setGoals("test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        // run dummy command only if build state is SUCCESS
        m.setRunPostStepsIfResult(Result.SUCCESS);
        m.addPostBuilder(new Shell("no-valid-command"));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }        
    
    @Issue("JENKINS-14102")
    @Test
    void testMaven2SkipPostBuilder() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setGoals("test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        // run dummy command only if build state is SUCCESS
        m.setRunPostStepsIfResult(Result.SUCCESS);
        m.addPostBuilder(new Shell("no-valid-command"));        
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }    
}
