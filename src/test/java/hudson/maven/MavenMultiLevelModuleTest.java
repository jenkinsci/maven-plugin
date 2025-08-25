package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.ExtractChangeLogSet;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.Serial;

import static org.junit.jupiter.api.Assertions.*;

import org.jvnet.hudson.test.ExtractResourceWithChangesSCM;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Test incremental build for a project with modules and submodules.
 *
 * @author Jean Blanchard
 */
@WithJenkins
class MavenMultiLevelModuleTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

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
    void testIncrementalBuildWithMultiModuleChangeSet() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceWithChangesSCM(getClass().getResource("maven-multilevelmod.zip"),
                getClass().getResource("maven-multilevelmod-changes.zip")));

        j.buildAndAssertSuccess(m);

        // Now run a second build with the changes.
        m.setIncrementalBuild(true);
        j.buildAndAssertSuccess(m);

        MavenModuleSetBuild pBuild = m.getLastBuild();
        ExtractChangeLogSet changeSet = (ExtractChangeLogSet) pBuild.getChangeSet();

        assertFalse(changeSet.isEmptySet(), "ExtractChangeLogSet should not be empty.");

        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            String moduleName = modBuild.getProject().getName();
            switch (moduleName) {
                case "org.jvnet.hudson.main.test.multilevelmod:moduleA" ->
                        assertEquals(Result.NOT_BUILT, modBuild.getResult(), "moduleA should have Result.NOT_BUILT");
                case "org.jvnet.hudson.main.test.multilevelmod:moduleAA" ->
                        assertEquals(Result.SUCCESS, modBuild.getResult(), "moduleAA should have Result.SUCCESS");
                case "org.jvnet.hudson.main.test.multilevelmod:moduleAB" ->
                        assertEquals(Result.NOT_BUILT, modBuild.getResult(), "moduleAB should have Result.NOT_BUILT");
                case "org.jvnet.hudson.main.test.multilevelmod:moduleB" ->
                        assertEquals(Result.SUCCESS, modBuild.getResult(), "moduleB should have Result.SUCCESS");
            }
        }

        long summedModuleDuration = 0;
        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            summedModuleDuration += modBuild.getDuration();
        }
        assertTrue(pBuild.getDuration() >= summedModuleDuration,
                "duration of moduleset build should be greater-equal than sum of the module builds");
    }

    private static class TestReporter extends MavenReporter {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) {
            assertNotNull(build.getWorkspace());
            return true;
        }
    }
}
