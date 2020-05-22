package hudson.maven;

import hudson.maven.local_repo.PerExecutorLocalRepositoryLocator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

public class MavenModuleSetGlobalConfigurationTest
{

    @Rule
    public final JenkinsRule r = new JenkinsConfiguredWithCodeRule();

//<hudson.maven.MavenModuleSet_-DescriptorImpl plugin="maven-plugin@3.7-SNAPSHOT">
//  <globalMavenOpts>-Xmx200303m</globalMavenOpts>
//  <localRepository class="hudson.maven.local_repo.PerExecutorLocalRepositoryLocator"/>
//</hudson.maven.MavenModuleSet_-DescriptorImpl>

    @Test
    @ConfiguredWithCode("maven-entries.yml")
    public void configureEntries() throws Exception {
        MavenModuleSet.DescriptorImpl descriptor = r.jenkins.getDescriptorByType( MavenModuleSet.DescriptorImpl.class );
        assertEquals( "-Xms2000m", descriptor.getGlobalMavenOpts());
        assertEquals( PerExecutorLocalRepositoryLocator.class, descriptor.getLocalRepository().getClass() );
    }

}
