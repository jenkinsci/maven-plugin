package hudson.maven.util;

import hudson.maven.Maven36xBuildTest;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.*;
import hudson.tasks.Maven;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ParameterizedClass
@MethodSource("data")
@WithJenkins
class VariableExpanderTest {

    static Stream<Arguments> data() {
        Map<String, String> envVarsMap = new HashMap<>();
        envVarsMap.put("ENV_EMAILS", "user@company.com");

        return Stream.of(
                Arguments.of(null, new HashMap<>(), null),
                Arguments.of(
                        "john.doe@nowhere.com",
                        new HashMap<>(),
                        "john.doe@nowhere.com"
                ),
                Arguments.of(
                        "Variable JOB_NAME = $JOB_NAME",
                        envVarsMap,
                        "Variable JOB_NAME = test"
                ),
                Arguments.of(
                        "Variable $NOT_IN_ENV_VARS = $NOT_IN_ENV_VARS",
                        envVarsMap,
                        "Variable $NOT_IN_ENV_VARS = $NOT_IN_ENV_VARS"
                )
        );
    }

    @Parameter(0)
    private String rawString;
    @Parameter(1)
    private Map<String, String> envVarsMap;
    @Parameter(2)
    private String expectedString;

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void expand() throws Exception {
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "test");
        Maven.MavenInstallation mavenInstallation = Maven36xBuildTest.configureMaven36();
        m.setMaven(mavenInstallation.getName());
        m.setScm(new ExtractResourceSCM(getClass().getResource("/hudson/maven/several-modules-in-directory.zip")));
        m.setGoals("clean validate");

        MavenModuleSetBuild mmsb =  j.buildAndAssertSuccess(m);
        assertEquals(expectedString ,new VariableExpander(mmsb, TaskListener.NULL).expand(rawString));
    }

}
