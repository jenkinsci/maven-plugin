package hudson.maven;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;


import hudson.Launcher;
import hudson.model.BuildListener;

import java.io.IOException;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * @author Andrew Bayer
 */
public class MavenEmptyModuleTest extends AbstractMavenTestCase {
    /**
     * Verify that a build will work with a module <module></module> and a module <module> </module>
     */
    @Bug(4442)
    public void testEmptyModuleParsesAndBuilds() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-empty-mod.zip")));
        buildAndAssertSuccess(m);
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