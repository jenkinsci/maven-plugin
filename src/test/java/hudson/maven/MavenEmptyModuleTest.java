package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
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
     * Verify that a build will work with a module <module></module> and a module <module> </module>
     */
    @Bug(4442)
    @Test
    public void testEmptyModuleParsesAndBuilds() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.getReporters().add(new TestReporter());
        m.setScm(new FolderResourceSCM("src/test/projects/maven-empty-mod"));
        jenkins.buildAndAssertSuccess(m);
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