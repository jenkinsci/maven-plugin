package hudson.maven;

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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * This test case verifies that a maven build also takes EnvironmentContributingAction into account to resolve variables on the command line
 * 
 * @see hudson.model.EnvironmentContributingAction
 * @author Marcin Cylke (mcl)
 */
public class MavenEnvironmentContributingActionFromBuilderTest {

    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    @Test
    @Bug(20844)
    public void builderInjectedVariableFromEnvironmentContributingActionMustBeAvailableInMavenModuleSetBuild() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.jenkins.createProject(MavenModuleSet.class, "mvn");

        p.setMaven(ToolInstallations.configureMaven3().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("maven3-project.zip")));
        p.setGoals("initialize -Dval=${KEY}");

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
            Assert.assertTrue(containsString + " was not found in the goals arguments(" + all + ")",
                all.contains(containsString));
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
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

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
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            build.addAction(new TestAction("KEY", envVariableValue));
            return true;
        }
    }
}
