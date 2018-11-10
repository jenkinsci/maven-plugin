package hudson.maven.util;

import hudson.maven.FolderResourceSCM;
import hudson.maven.MavenJenkinsRule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.*;
import hudson.tasks.Maven;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class VariableExpanderTest {

    @Rule
    public JenkinsRule j = new MavenJenkinsRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Map<String, String> envVarsMap = new HashMap<>();
        envVarsMap.put("ENV_EMAILS", "user@company.com");

        return Arrays.asList(new Object[][] {
                new Object[]{ null, new HashMap<>(), null },
                new Object[]{
                        "john.doe@nowhere.com",
                        new HashMap<>(),
                        "john.doe@nowhere.com"
                },
                new Object[]{
                        "Variable JOB_NAME = $JOB_NAME",
                        envVarsMap,
                        "Variable JOB_NAME = test"
                },
                new Object[]{
                        "Variable $NOT_IN_ENV_VARS = $NOT_IN_ENV_VARS",
                        envVarsMap,
                        "Variable $NOT_IN_ENV_VARS = $NOT_IN_ENV_VARS"
                }
        });
    }

    private final String rawString;
    private final Map<String, String> envVarsMap;
    private final String expectedString;

    public VariableExpanderTest(
            final String rawString,
            final Map<String, String> envVarsMap,
            final String expectedString) {
        this.rawString = rawString;
        this.envVarsMap = envVarsMap;
        this.expectedString = expectedString;
    }

    @Test
    public void expand() throws Exception {
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "test");
        Maven.MavenInstallation mavenInstallation = ToolInstallations.configureDefaultMaven();
        m.setMaven( mavenInstallation.getName() );
        m.setScm(new FolderResourceSCM( "src/test/projects/several-modules-in-directory"));
        m.setGoals( "clean validate" );

        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertEquals(expectedString ,new VariableExpander(mmsb, TaskListener.NULL).expand(rawString));
    }

}