package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.Serial;

import static hudson.Functions.isWindows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@WithJenkins
class AbortedMavenBuildTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Issue("JENKINS-8054")
    @Test
    void testBuildWrapperSeesAbortedStatus() throws Exception {
        assumeTrue(isWindows(), "Build fails on Windows due to open file handles.");
        ToolInstallations.configureMaven35();
        MavenModuleSet project = j.createProject(MavenModuleSet.class, "p");
        JenkinsRule.TestBuildWrapper wrapper = new JenkinsRule.TestBuildWrapper();
        project.getBuildWrappersList().add(wrapper);
        project.getReporters().add(new AbortingReporter());
        project.setGoals("clean verify -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        project.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimod.zip")));
        MavenModuleSetBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.ABORTED, build.getResult());
        assertEquals(Result.ABORTED, wrapper.buildResultInTearDown);
    }

    private static class AbortingReporter extends MavenReporter {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException {
            throw new InterruptedException();
        }
    }
}
