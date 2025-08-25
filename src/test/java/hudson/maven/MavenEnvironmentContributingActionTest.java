package hudson.maven;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ArgumentListBuilder;

import java.util.Collection;
import java.util.Collections;

import net.sf.json.JSONObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * This test case verifies that a maven build also takes EnvironmentContributingAction into account to resolve variables on the command line
 * 
 * @see EnvironmentContributingAction
 * @author Dominik Bartholdi (imod)
 */
@WithJenkins
class MavenEnvironmentContributingActionTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Issue("JENKINS-17555")
    void envVariableFromEnvironmentContributingActionMustBeAvailableInMavenModuleSetBuild() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        final MavenModuleSet p = j.jenkins.createProject(MavenModuleSet.class, "mvn");

        p.setMaven(Maven36xBuildTest.configureMaven36().getName());
        p.setScm(new ExtractResourceSCM(getClass().getResource("maven3-project.zip")));
        p.setGoals("initialize -Dval=${KEY} -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");

        p.getBuildWrappersList().add(new TestMvnBuildWrapper("-Dval=MY_VALUE"));

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
            assertTrue(all.contains(containsString), containsString + " was not found in the goals arguments");
            return cli;
        }

        @Override
        public String getGoalsAndOptions(MavenModuleSetBuild arg0) {
            return null;
        }
    }

    /**
     * This wrapper adds a EnvironmentContributingAction to the build (see TestAction) and also adds the MvnCmdLineVerifier to the build to test whether the variable really got replaced
     */
    public static class TestMvnBuildWrapper extends BuildWrapper {
        private String containsString;

        public TestMvnBuildWrapper(String expectedString) {
            this.containsString = expectedString;
        }

        @Override
        public Collection<? extends Action> getProjectActions(AbstractProject job) {
            return Collections.singletonList(new TestAction("KEY", "MY_VALUE"));
        }

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) {

            build.addAction(new MvnCmdLineVerifier(containsString));

            return new BuildWrapper.Environment() {
            };
        }

        @Extension
        public static class TestMvnBuildWrapperDescriptor extends BuildWrapperDescriptor {
            @Override
            public boolean isApplicable(AbstractProject<?, ?> project) {
                return true;
            }

            @Override
            public BuildWrapper newInstance(StaplerRequest2 req, JSONObject formData) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getDisplayName() {
                return this.getClass().getName();
            }
        }
    }

}
