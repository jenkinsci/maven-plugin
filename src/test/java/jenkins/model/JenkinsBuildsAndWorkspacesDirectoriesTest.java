package jenkins.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LogRecorder;
import org.jvnet.hudson.test.junit.jupiter.JenkinsSessionExtension;

import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Since JENKINS-50164, Jenkins#workspacesDir and Jenkins#buildsDir had their associated UI deleted.
 * So instead of configuring through the UI, we now have to use sysprops for this.
 *
 * <p>So this test class uses a {@link JenkinsSessionExtension} to check the behaviour of this
 * sysprop being present or not between two restarts.
 */
class JenkinsBuildsAndWorkspacesDirectoriesTest {

    @RegisterExtension
    private final JenkinsSessionExtension story = new JenkinsSessionExtension();

    private final LogRecorder logRecorder = new LogRecorder();

    @BeforeEach
    void beforeEach() {
        clearSystemProperties();
    }

    @AfterEach
    void afterEach() {
        clearSystemProperties();
    }

    private void clearSystemProperties() {
        Stream.of(Jenkins.BUILDS_DIR_PROP, Jenkins.WORKSPACES_DIR_PROP)
                .forEach(System::clearProperty);
    }

    private void setWorkspacesDirProperty(String workspacesDir) {
        System.setProperty(Jenkins.WORKSPACES_DIR_PROP, workspacesDir);
    }

    private void setBuildsDirProperty(String buildsDir) {
        System.setProperty(Jenkins.BUILDS_DIR_PROP, buildsDir);
    }

    private boolean logWasFound(String searched) {
        return logRecorder.getRecords().stream()
                .anyMatch(record -> record.getMessage().contains(searched));
    }

    @Test
    @Issue("JENKINS-12251")
    void testItemFullNameExpansion() throws Throwable {
        logRecorder.record(Jenkins.class, Level.WARNING)
                .record(Jenkins.class, Level.INFO)
                .capture(1000);

        story.then(j -> {
            assertTrue(j.getInstance().isDefaultBuildDir());
            assertTrue(j.getInstance().isDefaultWorkspaceDir());
            setBuildsDirProperty("${JENKINS_HOME}/test12251_builds/${ITEM_FULL_NAME}");
            setWorkspacesDirProperty("${JENKINS_HOME}/test12251_ws/${ITEM_FULL_NAME}");
        });

        story.then(j -> {
            assertTrue(JenkinsBuildsAndWorkspacesDirectoriesTest.this.logWasFound("Changing builds directories from "));
            assertFalse(j.getInstance().isDefaultBuildDir());
            assertFalse(j.getInstance().isDefaultWorkspaceDir());

            // build a dummy project
            MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "p");
            m.setScm(new ExtractResourceSCM(getClass().getResource("/simple-projects.zip")));
            MavenModuleSetBuild b = m.scheduleBuild2(0).get();

            // make sure these changes are effective
            assertTrue(b.getWorkspace().getRemote().contains("test12251_ws"));
            assertTrue(b.getRootDir().toString().contains("test12251_builds"));
        });
    }
}
