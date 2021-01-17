package jenkins.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Since JENKINS-50164, Jenkins#workspacesDir and Jenkins#buildsDir had their associated UI deleted.
 * So instead of configuring through the UI, we now have to use sysprops for this.
 *
 * <p>So this test class uses a {@link RestartableJenkinsRule} to check the behaviour of this
 * sysprop being present or not between two restarts.
 */
public class JenkinsBuildsAndWorkspacesDirectoriesTest {

    @Rule public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Rule public LoggerRule loggerRule = new LoggerRule();

    @Before
    public void before() {
        clearSystemProperties();
    }

    @After
    public void after() {
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
        return loggerRule.getRecords().stream()
                .anyMatch(record -> record.getMessage().contains(searched));
    }

    @Test
    @Issue("JENKINS-12251")
    public void testItemFullNameExpansion() {
        loggerRule.record(Jenkins.class, Level.WARNING)
                .record(Jenkins.class, Level.INFO)
                .capture(1000);

        story.then(steps -> {
            assertTrue(story.j.getInstance().isDefaultBuildDir());
            assertTrue(story.j.getInstance().isDefaultWorkspaceDir());
            setBuildsDirProperty("${JENKINS_HOME}/test12251_builds/${ITEM_FULL_NAME}");
            setWorkspacesDirProperty("${JENKINS_HOME}/test12251_ws/${ITEM_FULL_NAME}");
        });

        story.then(steps -> {
            assertTrue(JenkinsBuildsAndWorkspacesDirectoriesTest.this.logWasFound("Changing builds directories from "));
            assertFalse(story.j.getInstance().isDefaultBuildDir());
            assertFalse(story.j.getInstance().isDefaultWorkspaceDir());

            // build a dummy project
            MavenModuleSet m = story.j.jenkins.createProject(MavenModuleSet.class, "p");
            m.setScm(new ExtractResourceSCM(getClass().getResource("/simple-projects.zip")));
            MavenModuleSetBuild b = m.scheduleBuild2(0).get();

            // make sure these changes are effective
            assertTrue(b.getWorkspace().getRemote().contains("test12251_ws"));
            assertTrue(b.getRootDir().toString().contains("test12251_builds"));
        });
    }
}
