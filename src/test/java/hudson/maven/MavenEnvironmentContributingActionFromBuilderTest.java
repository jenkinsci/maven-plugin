package hudson.maven;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * This test case verifies that a maven build also takes EnvironmentContributingAction into account to resolve variables on the command line
 * 
 * @see hudson.model.EnvironmentContributingAction
 * @author Marcin Cylke (mcl)
 */
@WithJenkins
class MavenEnvironmentContributingActionFromBuilderTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Issue("JENKINS-20844")
    void builderInjectedVariableFromEnvironmentContributingActionMustBeAvailableInMavenModuleSetBuild() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.jenkins.createProject(MavenModuleSet.class, "mvn");

        p.setMaven(Maven36xBuildTest.configureMaven36().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("maven3-project.zip")));
        p.setGoals("initialize -Dval=${KEY} -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");

        String keyValue = "MY_VALUE";

        p.getPrebuilders().add(new TestMvnBuilder(keyValue));
        p.getBuildWrappersList().add(new AssertingBuildWrapper("-Dval=" + keyValue));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new Cause.UserIdCause()).get());
    }

    /**
     * This action contributes env variables
     */
    private static final class TestAction extends InvisibleAction implements EnvironmentContributingAction {
        private final String key, value;

        public TestAction(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void buildEnvVars(AbstractBuild<?, ?> arg0, EnvVars vars) {
            vars.put(key, value);
        }

    }

    /**
     * This action verifies that the variable in the maven arguments got replaced
     */
    private static class MvnCmdLineVerifier extends InvisibleAction implements MavenArgumentInterceptorAction {
        private String containsString;

        public MvnCmdLineVerifier(String containsString) {
            this.containsString = containsString;
        }

        @Override
        public ArgumentListBuilder intercept(ArgumentListBuilder cli, MavenModuleSetBuild arg1) {
            String all = cli.toString();
            assertTrue(all.contains(containsString),
                containsString + " was not found in the goals arguments(" + all + ")");
            return cli;
        }

        @Override
        public String getGoalsAndOptions(MavenModuleSetBuild arg0) {
            return null;
        }
    }

    /**
     * This wrapper adds MvnCmdLineVerifier to the build to test whether the variable really got replaced
     */
    public static class AssertingBuildWrapper extends BuildWrapper {
        private String containsString;

        public AssertingBuildWrapper(String expectedString) {
            this.containsString = expectedString;
        }

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) {

            build.addAction(new MvnCmdLineVerifier(containsString));

            return new Environment() { };
        }
    }

    /**
     * Simulates envinject plugin that does stuff and add an action to the build
     */
    public static class TestMvnBuilder extends Builder {

        private final String envVariableValue;

        public TestMvnBuilder(String envVariableValue) {
            this.envVariableValue = envVariableValue;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            build.addAction(new TestAction("KEY", envVariableValue));
            return true;
        }
    }
}
