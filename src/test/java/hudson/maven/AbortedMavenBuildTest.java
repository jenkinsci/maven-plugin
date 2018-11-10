package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AbortedMavenBuildTest {
    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    @Bug(8054)
    @Test
    public void testBuildWrapperSeesAbortedStatus() throws Exception {
        ToolInstallations.configureDefaultMaven();
        MavenModuleSet project = j.createProject(MavenModuleSet.class, "p");
        JenkinsRule.TestBuildWrapper wrapper = new JenkinsRule.TestBuildWrapper();
        project.getBuildWrappersList().add(wrapper);
        project.getReporters().add(new AbortingReporter());
        project.setGoals("clean");
        project.setScm(new FolderResourceSCM("src/test/projects/maven-empty-mod"));
        MavenModuleSetBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.ABORTED, build.getResult());
        assertEquals(Result.ABORTED, wrapper.buildResultInTearDown);
    }

    private static class AbortingReporter extends MavenReporter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            throw new InterruptedException();
        }
    }
}
