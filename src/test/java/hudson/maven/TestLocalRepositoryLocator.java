package hudson.maven;

import hudson.FilePath;
import hudson.maven.local_repo.LocalRepositoryLocator;
import org.kohsuke.stapler.DataBoundConstructor;

import java.nio.file.Paths;

public class TestLocalRepositoryLocator extends LocalRepositoryLocator
{

    private String path;

    @DataBoundConstructor
    public TestLocalRepositoryLocator( String path )
    {
        this.path = path;
    }

    @Override
    public FilePath locate( AbstractMavenBuild build )
    {
        return new FilePath( Paths.get(path).toFile() );
    }
}
