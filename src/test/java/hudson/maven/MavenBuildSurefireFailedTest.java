package hudson.maven;

import hudson.model.Result;
import hudson.tasks.Shell;
import java.util.concurrent.Callable;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.RunLoadCounter;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * @author Olivier Lamy
 */
public class MavenBuildSurefireFailedTest extends AbstractMavenTestCase {

    @Bug(8415)
    public void testMaven2Unstable() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setGoals( "test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8" );
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    @Bug(8415)
    public void testMaven3Failed() throws Exception {
        Maven36xBuildTest.configureMaven36();
        final MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setGoals( "test -Dmaven.test.failure.ignore=false -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8" );
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
        // JENKINS-18895:
        MavenModule failing = m.getModule("com.mycompany.app:my-app");
        assertEquals(Result.FAILURE, failing.getLastBuild().getResult());
        RunLoadCounter.prepare(failing);
        assertEquals(Result.FAILURE, RunLoadCounter.assertMaxLoads(failing, 0, new Callable<Result>() {
            @Override public Result call() throws Exception {
                return m.getLastBuild().getResult();
            }
        }));
    }   
    
    @Bug(8415)
    public void testMaven3Unstable() throws Exception {
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( Maven36xBuildTest.configureMaven36().getName() );
        m.setGoals( "test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8" );
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    @Bug(8415)
    public void testMaven3Failure() throws Exception {
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( Maven36xBuildTest.configureMaven36().getName() );
        m.setGoals( "test -Dmaven.test.failure.ignore=false -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8" );
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
    }    
    
    @Bug(14102)
    public void testMaven3SkipPostBuilder() throws Exception {
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setMaven( Maven36xBuildTest.configureMaven36().getName() );
        m.setGoals( "test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8" );
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        // run dummy command only if build state is SUCCESS
        m.setRunPostStepsIfResult(Result.SUCCESS);
        m.addPostBuilder(new Shell("no-valid-command"));
        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }        
    
    @Bug(14102)
    public void testMaven2SkipPostBuilder() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setGoals( "test -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8" );
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimodule-unit-failure.zip")));
        // run dummy command only if build state is SUCCESS
        m.setRunPostStepsIfResult(Result.SUCCESS);
        m.addPostBuilder(new Shell("no-valid-command"));        
        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }    
}
