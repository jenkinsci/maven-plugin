package hudson.maven.local_repo;

import hudson.Extension;
import hudson.FilePath;
import hudson.maven.AbstractMavenBuild;
import hudson.model.Executor;
import hudson.model.Node;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Kohsuke Kawaguchi
 */
public class PerExecutorLocalRepositoryLocator  extends LocalRepositoryLocator {
    @DataBoundConstructor
    public PerExecutorLocalRepositoryLocator() {}

    @Override
    public FilePath locate(AbstractMavenBuild build) {
        final Node builtOn = build.getBuiltOn();
        final FilePath rootPath = builtOn != null ? builtOn.getRootPath() : null;
        final Executor executor = Executor.currentExecutor();
        if (rootPath == null || executor == null) {
            return null;
        }
        return rootPath.child("maven-repositories/"+ executor.getNumber());
    }

    @Extension
    public static class DescriptorImpl extends LocalRepositoryLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return "Local to the executor";
        }
    }
}
