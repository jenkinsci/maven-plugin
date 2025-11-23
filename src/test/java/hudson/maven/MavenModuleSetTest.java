package hudson.maven;

import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.maven.reporters.MavenFingerprinter;
import hudson.model.Item;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Maven;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.jvnet.hudson.test.JenkinsRule.NO_PROPERTIES;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class MavenModuleSetTest {
    
    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }
    
    @Test
    void testConfigRoundtripLocalRepository() throws Exception {
        MavenModuleSet p = j.createProject(MavenModuleSet.class, "p");
        j.configRoundtrip((Item) p);
        
        assertNull(p.getExplicitLocalRepository());

        // make sure it roundtrips
        PerJobLocalRepositoryLocator before = new PerJobLocalRepositoryLocator();
        p.setLocalRepository(before);
        j.configRoundtrip((Item)p);
        j.assertEqualDataBoundBeans(p.getLocalRepository(),before);
        assertNotSame(before, p.getLocalRepository());
    }

    @Issue("JENKINS-17402")
    @Test
    void testGetItem() throws Exception {
        assertNull(j.createProject(MavenModuleSet.class, "p").getItem("invalid"));
    }

    @Test
    void testExplicitFingerprints() throws Exception {
        Maven.MavenInstallation mvn = Maven36xBuildTest.configureMaven36();
        Maven.MavenInstallation m3 = new Maven.MavenInstallation("apache-maven-3.1.0", mvn.getHome(), NO_PROPERTIES);
        j.jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(m3);
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-opts-echo.zip")));
        assertFalse(m.isArchivingDisabled());
        assertFalse(m.isSiteArchivingDisabled());
        assertFalse(m.isFingerprintingDisabled());
        assertTrue(m.isFingerprintConfigured());
        Fingerprinter.FingerprintAction fa = j.buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class); // determines root module as a side effect
        assertNotNull(fa);
        assertEquals("[junit:junit-3.8.1.jar, test:test:pom.xml]", new TreeSet<>(fa.getFingerprints().keySet()).toString());
        assertNotNull(j.jenkins.getDescriptorByType(MavenFingerprinter.DescriptorImpl.class).newAutoInstance(m.getRootModule()));
        m.setIsArchivingDisabled(true);
        m.setIsSiteArchivingDisabled(true);
        m.setIsFingerprintingDisabled(true);
        // Worth testing configRoundtrip since MMS uses old, deprecated, pre-auto-form-binding idioms to configure itself.
        // (And the setter names do not even follow JavaBeans conventions.)
        j.configRoundtrip(m);
        assertTrue(m.isArchivingDisabled());
        assertTrue(m.isSiteArchivingDisabled());
        assertTrue(m.isFingerprintingDisabled());
        assertFalse(m.isFingerprintConfigured());
        assertNull(j.buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class));
        assertNull(j.jenkins.getDescriptorByType(MavenFingerprinter.DescriptorImpl.class).newAutoInstance(m.getRootModule()));
        m.getPublishersList().add(new Fingerprinter("pom.xml", false));
        j.configRoundtrip(m);
        assertTrue(m.isFingerprintingDisabled());
        assertTrue(m.isFingerprintConfigured());
        assertNotNull(m.getPublishersList().get(Fingerprinter.class));
        fa = j.buildAndAssertSuccess(m).getAction(Fingerprinter.FingerprintAction.class);
        assertNotNull(fa);
        assertEquals("[pom.xml]", new TreeSet<>(fa.getFingerprints().keySet()).toString());
        assertNull(j.jenkins.getDescriptorByType(MavenFingerprinter.DescriptorImpl.class).newAutoInstance(m.getRootModule()));
    }

    @Issue("JENKINS-21903")
    @Test
    void testConfigRoundtripTriggers() throws Exception {
        // New project defaults to trigger with blocks:
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        assertFalse(m.isDisableTriggerDownstreamProjects());
        assertTrue(m.getBlockTriggerWhenBuilding());
        j.configRoundtrip(m);
        assertFalse(m.isDisableTriggerDownstreamProjects());
        assertTrue(m.getBlockTriggerWhenBuilding());
        // No trigger:
        m.setDisableTriggerDownstreamProjects(true);
        j.configRoundtrip(m);
        assertTrue(m.isDisableTriggerDownstreamProjects());
        // blockTriggerWhenBuilding irrelevant in this case, I think (perhaps not in exotic case involving multiple upstreams)
        // Unconditional trigger:
        m.setDisableTriggerDownstreamProjects(false);
        m.setBlockTriggerWhenBuilding(false);
        j.configRoundtrip(m);
        assertFalse(m.isDisableTriggerDownstreamProjects());
        assertFalse(m.getBlockTriggerWhenBuilding());
    }


}
