package hudson.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.maven.local_repo.PerExecutorLocalRepositoryLocator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class MavenModuleSetGlobalConfigurationTest {

//<hudson.maven.MavenModuleSet_-DescriptorImpl plugin="maven-plugin@3.7-SNAPSHOT">
//  <globalMavenOpts>-Xmx200303m</globalMavenOpts>
//  <localRepository class="hudson.maven.local_repo.PerExecutorLocalRepositoryLocator"/>
//</hudson.maven.MavenModuleSet_-DescriptorImpl>

    @Test
    @ConfiguredWithCode("maven-entries.yml")
    void configureEntries(JenkinsConfiguredWithCodeRule r) {
        MavenModuleSet.DescriptorImpl descriptor = r.jenkins.getDescriptorByType(MavenModuleSet.DescriptorImpl.class);
        assertEquals("-Xms2000m", descriptor.getGlobalMavenOpts());
        assertEquals(PerExecutorLocalRepositoryLocator.class, descriptor.getLocalRepository().getClass());
    }

}
