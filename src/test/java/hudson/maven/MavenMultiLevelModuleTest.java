package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.Maven.MavenInstallation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractChangeLogSet;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * Test incremental build for a project with modules and submodules.
 *
 * @author Jean Blanchard
 */
public class MavenMultiLevelModuleTest {

    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    /**
     * Given a multi-module project:
     * <pre>
     *     parent
     *       - A
     *         - AA
     *         - AB
     *       - B
     * </pre>
     * If a changeset impacts modules AA and B, A should not be built.
     * @throws Exception
     */
    @For(MavenModuleSetBuild.class)
    @Test
    public void testIncrementalBuildWithMultiModuleChangeSet() throws Exception {
        ToolInstallations.configureDefaultMaven("apache-maven-2.2.1", MavenInstallation.MAVEN_21);
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceWithChangesSCM2(getClass().getResource("maven-multilevelmod.zip"),
                getClass().getResource("maven-multilevelmod-changes.zip")));

        j.buildAndAssertSuccess(m);

        // Now run a second build with the changes.
        m.setIncrementalBuild(true);
        j.buildAndAssertSuccess(m);

        MavenModuleSetBuild pBuild = m.getLastBuild();
        ExtractChangeLogSet changeSet = (ExtractChangeLogSet) pBuild.getChangeSet();

        assertFalse("ExtractChangeLogSet should not be empty.", changeSet.isEmptySet());

        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            String moduleName = modBuild.getProject().getName().toString();
            if (moduleName.equals("org.jvnet.hudson.main.test.multilevelmod:moduleA")) {
                assertEquals("moduleA should have Result.NOT_BUILT", Result.NOT_BUILT, modBuild.getResult());
            } else if (moduleName.equals("org.jvnet.hudson.main.test.multilevelmod:moduleAA")) {
                assertEquals("moduleAA should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            } else if (moduleName.equals("org.jvnet.hudson.main.test.multilevelmod:moduleAB")) {
                assertEquals("moduleAB should have Result.NOT_BUILT", Result.NOT_BUILT, modBuild.getResult());
            } else if (moduleName.equals("org.jvnet.hudson.main.test.multilevelmod:moduleB")) {
                assertEquals("moduleB should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            }
        }

        long summedModuleDuration = 0;
        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            summedModuleDuration += modBuild.getDuration();
        }
        assertTrue("duration of moduleset build should be greater-equal than sum of the module builds",
                pBuild.getDuration() >= summedModuleDuration);
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
