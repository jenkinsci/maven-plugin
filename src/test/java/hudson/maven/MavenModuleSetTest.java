package hudson.maven;

import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.maven.reporters.MavenFingerprinter;
import hudson.model.Item;
import hudson.tasks.Fingerprinter;
import java.util.TreeSet;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenModuleSetTest extends HudsonTestCase {
    public void testConfigRoundtripLocalRepository() throws Exception {
        MavenModuleSet p = createMavenProject();
        configRoundtrip((Item) p);
        
        assertNull(p.getExplicitLocalRepository());

        // make sure it roundtrips
        PerJobLocalRepositoryLocator before = new PerJobLocalRepositoryLocator();
        p.setLocalRepository(before);
        configRoundtrip((Item)p);
        assertEqualDataBoundBeans(p.getLocalRepository(),before);
        assertTrue(before!=p.getLocalRepository());
    }

    @Bug(17402)
    public void testGetItem() throws Exception {
        assertNull(createMavenProject().getItem("invalid"));
    }

    public void testExplicitFingerprints() throws Exception {
        configureMaven31();
        MavenModuleSet m = createMavenProject();
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        assertFalse(m.isArchivingDisabled());
        assertFalse(m.isSiteArchivingDisabled());
        assertFalse(m.isFingerprintingDisabled());
        assertTrue(m.isFingerprintConfigured());
        Fingerprinter.FingerprintAction fa = buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class); // determines root module as a side effect
        assertNotNull(fa);
        assertEquals("[junit:junit-3.8.1.jar, test:test:pom.xml]", new TreeSet<String>(fa.getFingerprints().keySet()).toString());
        assertNotNull(jenkins.getDescriptorByType(MavenFingerprinter.DescriptorImpl.class).newAutoInstance(m.getRootModule()));
        m.setIsArchivingDisabled(true);
        m.setIsSiteArchivingDisabled(true);
        m.setIsFingerprintingDisabled(true);
        // Worth testing configRoundtrip since MMS uses old, deprecated, pre-auto-form-binding idioms to configure itself.
        // (And the setter names do not even follow JavaBeans conventions.)
        configRoundtrip(m);
        assertTrue(m.isArchivingDisabled());
        assertTrue(m.isSiteArchivingDisabled());
        assertTrue(m.isFingerprintingDisabled());
        assertFalse(m.isFingerprintConfigured());
        assertNull(buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class));
        assertNull(jenkins.getDescriptorByType(MavenFingerprinter.DescriptorImpl.class).newAutoInstance(m.getRootModule()));
        m.getPublishersList().add(new Fingerprinter("pom.xml", false));
        configRoundtrip(m);
        assertTrue(m.isFingerprintingDisabled());
        assertTrue(m.isFingerprintConfigured());
        assertNotNull(m.getPublishersList().get(Fingerprinter.class));
        fa = buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class);
        assertNotNull(fa);
        assertEquals("[pom.xml]", new TreeSet<String>(fa.getFingerprints().keySet()).toString());
        assertNull(jenkins.getDescriptorByType(MavenFingerprinter.DescriptorImpl.class).newAutoInstance(m.getRootModule()));
    }

}
