package hudson.maven.reporters;

import hudson.maven.AbstractMavenTestCase;
import org.jvnet.hudson.test.HudsonTestCase;
import hudson.maven.MavenProjectTest;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenSiteArchiverTest extends AbstractMavenTestCase {
    /**
     * Makes sure that the site archiving happens automatically.
     * The actual test resides in {@link MavenProjectTest#testSiteBuild()} 
     */
    public void testSiteArchiving() throws Exception {
    }
}
