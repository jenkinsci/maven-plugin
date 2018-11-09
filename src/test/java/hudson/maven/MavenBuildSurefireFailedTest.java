package hudson.maven;

import hudson.model.Result;
import hudson.tasks.Shell;
import org.junit.Rule;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RunLoadCounter;
import org.jvnet.hudson.test.ToolInstallations;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;

/**
 * @author Olivier Lamy
 */
public class MavenBuildSurefireFailedTest {

    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    @Bug(8415)
    public void testMaven2Unstable() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setGoals( "test" );
        m.setScm(new FolderResourceSCM("src/test/projects/maven-multimodule-unit-failure"));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    @Bug(8415)
    public void testMaven2Failed() throws Exception {
        ToolInstallations.configureDefaultMaven();
        final MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setGoals( "test -Dmaven.test.failure.ignore=false" );
        m.setScm(new FolderResourceSCM("src/test/projects/maven-multimodule-unit-failure"));
        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
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
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMaven( ToolInstallations.configureMaven3().getName() );
        m.setGoals( "test" );
        m.setScm(new FolderResourceSCM("src/test/projects/maven-multimodule-unit-failure"));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }
    
    @Bug(8415)
    public void testMaven3Failed() throws Exception {
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMaven( ToolInstallations.configureMaven3().getName() );
        m.setGoals( "test -Dmaven.test.failure.ignore=false" );
        m.setScm(new FolderResourceSCM("src/test/projects/maven-multimodule-unit-failure"));
        j.assertBuildStatus(Result.FAILURE, m.scheduleBuild2(0).get());
    }    
    
    @Bug(14102)
    public void testMaven3SkipPostBuilder() throws Exception {
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setMaven( ToolInstallations.configureMaven3().getName() );
        m.setGoals( "test" );
        m.setScm(new FolderResourceSCM("src/test/projects/maven-multimodule-unit-failure"));
        // run dummy command only if build state is SUCCESS
        m.setRunPostStepsIfResult(Result.SUCCESS);
        m.addPostBuilder(new Shell("no-valid-command"));
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }        
    
    @Bug(14102)
    public void testMaven2SkipPostBuilder() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setGoals( "test" );
        m.setScm(new FolderResourceSCM("src/test/projects/maven-multimodule-unit-failure"));
        // run dummy command only if build state is SUCCESS
        m.setRunPostStepsIfResult(Result.SUCCESS);
        m.addPostBuilder(new Shell("no-valid-command"));        
        j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
    }    
}
