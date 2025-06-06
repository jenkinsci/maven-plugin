/**
 *
 */
package hudson.maven;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.reporters.MavenFingerprinter;
import hudson.model.BuildListener;
import hudson.tasks.LogRotator;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import org.jvnet.hudson.test.ExtractResourceWithChangesSCM;

/**
 *
 * Test that looks in jobs archive with 2 builds. When LogRotator set as build
 * discarder with settings to keep only 1 build with artifacts, test searches
 * for jars in archive for build one and build two, expecting no jars in build 1
 * and expecting jars in build 2.
 *
 *
 */
public class MavenMultiModuleLogRotatorCleanArtifactsTest
{


    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    private MavenModuleSet m;

    private FilePath jobs;

    private static class TestReporter
        extends MavenReporter
    {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end( MavenBuild build, Launcher launcher, BuildListener listener )
            throws InterruptedException, IOException
        {
            Assert.assertNotNull( build.getProject().getSomeWorkspace() );
            Assert.assertNotNull( build.getWorkspace() );
            return true;
        }
    }

    @Before
    public void setUp()
        throws Exception
    {
        Maven36xBuildTest.configureMaven36();
        m = j.jenkins.createProject( MavenModuleSet.class, "p" );
        m.setBuildDiscarder( new LogRotator( "-1", "2", "-1", "1" ) );
        m.getReporters().add( new TestReporter() );
        m.getReporters().add( new MavenFingerprinter() );
        m.setScm( new ExtractResourceWithChangesSCM( getClass().getResource( "maven-multimod.zip" ),
                                                      getClass().getResource( "maven-multimod-changes.zip" ) ) );
        j.buildAndAssertSuccess( m );
        // Now run a second build with the changes.
        m.setIncrementalBuild( false );
        j.buildAndAssertSuccess( m );
        FilePath workspace = m.getSomeWorkspace();
        FilePath parent = workspace.getParent().getParent();
        jobs = new FilePath( parent, "jobs" );
    }

    @Test
    @Bug( 17508 )
    @For( { MavenModuleSetBuild.class, LogRotator.class } )
    @SuppressWarnings( "unchecked" )
    public void testArtifactsAreDeletedInBuildOneWhenBuildDiscarderRun()
        throws Exception
    {
        File directory = new File( new FilePath( jobs, "p/builds/1" ).getRemote() );
        await("Found jars in previous build, that should not happen").until(() -> FileUtils.listFiles(directory, new String[] {"jar"}, true), empty());
        await("No jars in last build ALERT!").until(() -> FileUtils.listFiles(new File(new FilePath(jobs, "p/builds/2").getRemote()), new String[] {"jar"}, true), not(empty()));
    }

    /**
     * Performs a third build and expecting build one to be deleted
     * @throws Exception
     */
    @For( { MavenModuleSetBuild.class, LogRotator.class } )
    @Test
    public void testArtifactsOldBuildsDeletedWhenBuildDiscarderRun()
        throws Exception
    {
        j.buildAndAssertSuccess( m );
        File directory = new File( new FilePath( jobs, "p/builds/1" ).getRemote() );
        await("oops the build should have been deleted").until(directory::exists, is(false));
    }

}
