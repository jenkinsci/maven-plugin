package hudson.maven.reporters;

import static hudson.maven.MojoInfoBuilder.mojoBuilder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import hudson.maven.MojoInfo;
import hudson.maven.MojoInfoBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.Issue;

/**
 * Regression test for the detection of test mojos in {@link SurefireArchiver}.
 * 
 * @author kutzi
 */
class SurefireArchiverDetectTestMojosTest {
    
    private SurefireArchiver surefireArchiver;

    @BeforeEach
    void beforeEach() {
        this.surefireArchiver = new SurefireArchiver();
    }

    @Test
    void shouldDetectMavenSurefire() {
        MojoInfo mojo = mojoBuilder("org.apache.maven.plugins", "maven-surefire-plugin", "test").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectMavenFailsafe() {
        MojoInfo mojo = mojoBuilder("org.apache.maven.plugins", "maven-failsafe-plugin", "verify").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectMavenFailsafe2() {
        MojoInfo mojo = mojoBuilder("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectMavenSurefireSkip() {
        MojoInfoBuilder builder = mojoBuilder("org.apache.maven.plugins", "maven-surefire-plugin", "test");
        MojoInfo mojo = builder.copy()
                .configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy()
                .version("2.4")
                .configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy()
                .version("2.3")
                .configValue("skipExec", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));

        // That's not a valid skip property:
        mojo = builder.copy()
                .configValue("skip--Exec", "true").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectMavenJunitPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.sun.maven", "maven-junit-plugin", "test");
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy()
                .configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectFlexMojoMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.sonatype.flexmojos", "flexmojos-maven-plugin", "test-run");
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy()
                .configValue("skipTest", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectOsgiTestPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.sonatype.tycho", "maven-osgi-test-plugin", "test"); 
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTest", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectTychoSurefirePlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.eclipse.tycho", "tycho-surefire-plugin", "test"); 
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTest", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectMavenAndroidPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.jayway.maven.plugins.android.generation2", "maven-android-plugin", "internal-integration-test")
                .version("3.0.0-alpha-6");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectAndroidMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.jayway.maven.plugins.android.generation2", "android-maven-plugin", "internal-integration-test")
                .version("3.0.0-alpha-6");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectGwtMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.codehaus.mojo", "gwt-maven-plugin", "test")
                .version("1.2");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        // that version of the plugin is too old
        mojo = builder.copy().version("1.1").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectSoapUiMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.smartbear.soapui", "soapui-maven-plugin", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectSoapUiProMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.smartbear.soapui", "soapui-pro-maven-plugin", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectSoapUiExtensionMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.github.redfish4ktc.soapui", "maven-soapui-extension-plugin", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectToolkitResolverPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.terracotta.maven.plugins", "toolkit-resolver-plugin", "toolkit-resolve-test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    void shouldDetectAnyMojoWithATestGoal() {
        MojoInfoBuilder builder = mojoBuilder("some.weird.internal","test-mojo", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    @Issue("JENKINS-31258")
    void shouldDetectAnyMojoWithAJenkinsReportsDirectoryProperty() {
		MojoInfoBuilder builder = mojoBuilder("some.weird.internal", "xxx-mojo", "xxx-goal")
				.executionId("xxx-execution-id").evaluator(new ExpressionEvaluator() {
					@Override
					public Object evaluate(String expression) {
						if ("${jenkins.xxx-execution-id.reportsDirectory}".equals(expression))
							return "target/xxx-test-reports";
						return expression;
					}

					@Override
					public File alignToBaseDirectory(File path) {
						return path;
					}
				});

		MojoInfo mojo = builder.build();
		assertTrue(this.surefireArchiver.isTestMojo(mojo));
	}

    @Test
    void shouldNotDetectNonTestGoal() {
        MojoInfoBuilder builder = mojoBuilder("some.weird.internal","test-mojo", "verify");
        
        MojoInfo mojo = builder.build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }


    @Test
    void shouldDetectMavenQunit() {
        MojoInfo mojo = mojoBuilder("net.kennychua", "phantomjs-qunit-runner","test").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }
}
