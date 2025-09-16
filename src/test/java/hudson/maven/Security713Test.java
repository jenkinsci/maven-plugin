package hudson.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.Functions;
import hudson.tasks.BatchFile;
import hudson.tasks.Maven;
import hudson.tasks.Shell;
import hudson.util.Secret;
import org.jenkinsci.plugins.credentialsbinding.impl.SecretBuildWrapper;
import org.jenkinsci.plugins.credentialsbinding.impl.StringBinding;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@WithJenkins
class Security713Test {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Issue("SECURITY-713") @Test
    void maskingMavenSecrets() throws Exception {
        Maven.MavenInstallation mavenInstallation = ToolInstallations.configureMaven35();
        String id = "creds";
        String pwd = "p4$$word";
        CredentialsProvider.lookupStores(j.jenkins).iterator() //
            .next() //
            .addCredentials(Domain.global(), //
                             new StringCredentialsImpl(CredentialsScope.GLOBAL, id, "", Secret.fromString(pwd)));
        MavenModuleSet p = j.createProject(MavenModuleSet.class);
        p.setMaven(mavenInstallation.getName());
        j.jenkins.getWorkspaceFor(p) //
            .child("pom.xml") //
            .write("<project><modelVersion>4.0.0</modelVersion><groupId>x</groupId><artifactId>y</artifactId><version>0-" + pwd + "</version></project>", null);
        p.setGoals("help:evaluate -Dexpression=project.version");
        p.getPostbuilders().add(Functions.isWindows() ? new BatchFile("echo %PASS%") : new Shell("echo \"$PASS\""));
        p.getBuildWrappersList().add(new SecretBuildWrapper(Collections.singletonList(new StringBinding("PASS", id))));
        MavenModuleSetBuild b = j.buildAndAssertSuccess(p);
        j.assertLogNotContains(pwd, b);
        j.assertLogContains("****", b);
        Map<MavenModule, List<MavenBuild>> moduleBuilds = b.getModuleBuilds();
        assertEquals(1, moduleBuilds.size());
        List<MavenBuild> theBuilds = moduleBuilds.values().iterator().next();
        assertEquals(1, theBuilds.size());
        j.assertLogNotContains(pwd, theBuilds.get(0));
        j.assertLogContains("****", theBuilds.get(0));
    }

}
