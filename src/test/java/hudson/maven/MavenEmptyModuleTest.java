package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * @author Andrew Bayer
 */
public class MavenEmptyModuleTest {

    @Rule
    public JenkinsRule jenkins = new MavenJenkinsRule();

    /**
     * <b>behaviour has changed when upgrading to maven 3.5.4</b>
     * Verify that a build will work with a module <module></module> and a module <module> </module>
     */
    @Issue("JENKINS-4442")
    @Test
    public void testEmptyModuleParsesAndBuilds() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.getReporters().add(new TestReporter());
        m.setScm(new FolderResourceSCM("src/test/projects/maven-empty-mod"));
        jenkins.assertBuildStatus( Result.ABORTED, m.scheduleBuild2( 0 ).get());
    }
    
    private static class TestReporter extends MavenReporter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            assertNotNull(build.getWorkspace());
            return true;
        }
    }
}