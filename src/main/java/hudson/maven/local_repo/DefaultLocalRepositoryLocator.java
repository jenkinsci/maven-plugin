package hudson.maven.local_repo;

import hudson.Extension;
import hudson.FilePath;
import hudson.maven.AbstractMavenBuild;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Uses Maven's default local repository, which is usually <code>~/.m2/repository</code>,
 * or the value of 'localRepository' in Maven's settings file, if defined.
 *
 * @see <a href="https://maven.apache.org/settings.html#Settings_Details">https://maven.apache.org/settings.html#Settings_Details</a>
 *
 * @author Kohsuke Kawaguchi
 */
public class DefaultLocalRepositoryLocator extends LocalRepositoryLocator {
    @DataBoundConstructor
    public DefaultLocalRepositoryLocator() {
    }

    @Override
    public FilePath locate(AbstractMavenBuild build) {
        return null;
    }

    @Extension
    public static class DescriptorImpl extends LocalRepositoryLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return "Default (\"~/.m2/repository\", or the value of 'localRepository' in Maven's settings file, if defined)";
        }
    }
}
