package hudson.maven.reporters;

import static hudson.maven.MojoInfoBuilder.mojoBuilder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import hudson.maven.MojoInfo;
import hudson.maven.MojoInfoBuilder;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Regression test for the detection of test mojos in {@link SurefireArchiver}.
 * 
 * @author kutzi
 */
public class SurefireArchiverDetectTestMojosTest {
    
    private SurefireArchiver surefireArchiver;

    @Before
    public void before() {
        this.surefireArchiver = new SurefireArchiver();
    }
    
    @Test
    public void shouldDetectMavenSurefire() {
        MojoInfo mojo = mojoBuilder("org.apache.maven.plugins", "maven-surefire-plugin", "test").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectMavenFailsafe() {
        MojoInfo mojo = mojoBuilder("org.apache.maven.plugins", "maven-failsafe-plugin", "verify").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectMavenFailsafe2() {
        MojoInfo mojo = mojoBuilder("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectMavenSurefireSkip() {
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
    public void shouldDetectMavenJunitPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.sun.maven", "maven-junit-plugin", "test");
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy()
                .configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectFlexMojoMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.sonatype.flexmojos", "flexmojos-maven-plugin", "test-run");
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy()
                .configValue("skipTest", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectOsgiTestPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.sonatype.tycho", "maven-osgi-test-plugin", "test"); 
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTest", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectTychoSurefirePlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.eclipse.tycho", "tycho-surefire-plugin", "test"); 
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTest", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectMavenAndroidPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.jayway.maven.plugins.android.generation2", "maven-android-plugin", "internal-integration-test")
                .version("3.0.0-alpha-6");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectAndroidMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.jayway.maven.plugins.android.generation2", "android-maven-plugin", "internal-integration-test")
                .version("3.0.0-alpha-6");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectGwtMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.codehaus.mojo", "gwt-maven-plugin", "test")
                .version("1.2");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        // that version of the plugin is too old
        mojo = builder.copy().version("1.1").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectSoapUiMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.smartbear.soapui", "soapui-maven-plugin", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectSoapUiProMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.smartbear.soapui", "soapui-pro-maven-plugin", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }

    @Test
    public void shouldDetectSoapUiExtensionMavenPlugin() {
        MojoInfoBuilder builder = mojoBuilder("com.github.redfish4ktc.soapui", "maven-soapui-extension-plugin", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skip", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectToolkitResolverPlugin() {
        MojoInfoBuilder builder = mojoBuilder("org.terracotta.maven.plugins", "toolkit-resolver-plugin", "toolkit-resolve-test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
        
        mojo = builder.copy().configValue("skipTests", "true").build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    public void shouldDetectAnyMojoWithATestGoal() {
        MojoInfoBuilder builder = mojoBuilder("some.weird.internal","test-mojo", "test");
        
        MojoInfo mojo = builder.build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }
    
    @Test
    @Issue("JENKINS-31258")
	public void shouldDetectAnyMojoWithAJenkinsReportsDirectoryProperty() {
		MojoInfoBuilder builder = mojoBuilder("some.weird.internal", "xxx-mojo", "xxx-goal")
				.executionId("xxx-execution-id").evaluator(new ExpressionEvaluator() {
					@Override
					public Object evaluate(String expression) throws ExpressionEvaluationException {
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
    public void shouldNotDetectNonTestGoal() {
        MojoInfoBuilder builder = mojoBuilder("some.weird.internal","test-mojo", "verify");
        
        MojoInfo mojo = builder.build();
        assertFalse(this.surefireArchiver.isTestMojo(mojo));
    }


    @Test
    public void shouldDetectMavenQunit() {
        MojoInfo mojo = mojoBuilder("net.kennychua", "phantomjs-qunit-runner","test").build();
        assertTrue(this.surefireArchiver.isTestMojo(mojo));
    }
}
