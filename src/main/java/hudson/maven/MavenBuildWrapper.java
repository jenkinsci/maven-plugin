package hudson.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Map;

public class MavenBuildWrapper extends BuildWrapper {

    private final String mavenInstallationName;

    @DataBoundConstructor
    public MavenBuildWrapper(String mavenInstallationName) {
        this.mavenInstallationName = mavenInstallationName;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        MavenInstallation installation = getMavenInstallation();
        if (installation != null) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(build.getBuildVariables());

            // Get the Maven version for this node, installing it if necessary
            installation = installation.forNode(Computer.currentComputer().getNode(), listener).forEnvironment(env);
        }

        final MavenInstallation inst = installation;
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                if (inst != null) {
                    EnvVars envVars = new EnvVars();
                    inst.buildEnvVars(envVars);
                    env.putAll(envVars);
                }
            }
        };
    }

    public String getMavenInstallationName() {
        return mavenInstallationName;
    }

    private MavenInstallation getMavenInstallation() {
        for (MavenInstallation i : ((DescriptorImpl) getDescriptor()).getInstallations()) {
            if (i.getName().equals(mavenInstallationName)) {
                return i;
            }
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.MavenBuildWrapper_DisplayName();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP")
        public MavenInstallation[] getInstallations() {
            return Jenkins.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations();
        }
    }
}
