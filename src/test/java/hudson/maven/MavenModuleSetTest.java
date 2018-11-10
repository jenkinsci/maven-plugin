package hudson.maven;

import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.model.Item;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Maven;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.util.TreeSet;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenModuleSetTest {
    @Rule
    public JenkinsRule jenkins = new MavenJenkinsRule();
    public void testConfigRoundtripLocalRepository() throws Exception {
        MavenModuleSet p = jenkins.createProject(MavenModuleSet.class, "p");
        jenkins.configRoundtrip((Item) p);
        
        assertNull(p.getExplicitLocalRepository());

        // make sure it roundtrips
        PerJobLocalRepositoryLocator before = new PerJobLocalRepositoryLocator();
        p.setLocalRepository(before);
        jenkins.configRoundtrip((Item)p);
        jenkins.assertEqualDataBoundBeans(p.getLocalRepository(),before);
        assertTrue(before!=p.getLocalRepository());
    }

    @Bug(17402)
    @Test
    public void testGetItem() throws Exception {
        assertNull(jenkins.createProject(MavenModuleSet.class, "p").getItem("invalid"));
    }

    @Test
    public void testExplicitFingerprints() throws Exception {
        Maven.MavenInstallation mvn = ToolInstallations.configureDefaultMaven("apache-maven-3.1.0", Maven.MavenInstallation.MAVEN_30);
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        assertFalse(m.isArchivingDisabled());
        assertFalse(m.isSiteArchivingDisabled());
        assertFalse(m.isFingerprintingDisabled());
        assertTrue(m.isFingerprintConfigured());
        Fingerprinter.FingerprintAction fa = jenkins.buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class); // determines root module as a side effect
        assertNotNull(fa);
        assertEquals("[junit:junit-3.8.1.jar, test:test:pom.xml]", new TreeSet<String>(fa.getFingerprints().keySet()).toString());
        m.setIsArchivingDisabled(true);
        m.setIsSiteArchivingDisabled(true);
        m.setIsFingerprintingDisabled(true);
        // Worth testing configRoundtrip since MMS uses old, deprecated, pre-auto-form-binding idioms to configure itself.
        // (And the setter names do not even follow JavaBeans conventions.)
        jenkins.configRoundtrip(m);
        assertTrue(m.isArchivingDisabled());
        assertTrue(m.isSiteArchivingDisabled());
        assertTrue(m.isFingerprintingDisabled());
        assertFalse(m.isFingerprintConfigured());
        assertNull(jenkins.buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class));
        m.getPublishersList().add(new Fingerprinter("pom.xml", false));
        jenkins.configRoundtrip(m);
        assertTrue(m.isFingerprintingDisabled());
        assertTrue(m.isFingerprintConfigured());
        assertNotNull(m.getPublishersList().get(Fingerprinter.class));
        fa = jenkins.buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class);
        assertNotNull(fa);
        assertEquals("[pom.xml]", new TreeSet<String>(fa.getFingerprints().keySet()).toString());
    }

    @Bug(21903)
    @Test
    public void testConfigRoundtripTriggers() throws Exception {
        // New project defaults to trigger with blocks:
        MavenModuleSet m = jenkins.createProject(MavenModuleSet.class, "p");
        assertFalse(m.isDisableTriggerDownstreamProjects());
        assertTrue(m.getBlockTriggerWhenBuilding());
        jenkins.configRoundtrip(m);
        assertFalse(m.isDisableTriggerDownstreamProjects());
        assertTrue(m.getBlockTriggerWhenBuilding());
        // No trigger:
        m.setDisableTriggerDownstreamProjects(true);
        jenkins.configRoundtrip(m);
        assertTrue(m.isDisableTriggerDownstreamProjects());
        // blockTriggerWhenBuilding irrelevant in this case, I think (perhaps not in exotic case involving multiple upstreams)
        // Unconditional trigger:
        m.setDisableTriggerDownstreamProjects(false);
        m.setBlockTriggerWhenBuilding(false);
        jenkins.configRoundtrip(m);
        assertFalse(m.isDisableTriggerDownstreamProjects());
        assertFalse(m.getBlockTriggerWhenBuilding());
    }

}
