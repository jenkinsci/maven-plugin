package hudson.maven.reporters;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.maven.MojoInfo;
import hudson.maven.MojoInfoBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.jvnet.hudson.test.Issue;

class TestMojoTest {

    @Test
    @Issue("JENKINS-16573")
    void testGetReportFilesThrowsNoException() throws Exception {
        // no 'reportsDirectory' or so config value set:
        MojoInfo mojoInfo = MojoInfoBuilder.mojoBuilder("com.some", "testMojo", "test").build();
        
        MavenProject pom = mock(MavenProject.class);
        when(pom.getBasedir()).thenReturn(new File("foo"));
        
        Build build = mock(Build.class);
        when(build.getDirectory()).thenReturn("bar");
        when(pom.getBuild()).thenReturn(build);
        
        for (TestMojo testMojo : TestMojo.values()) {
            testMojo.getReportFiles(pom, mojoInfo);
        }
    }

    @Test
    void testGetReportFilesAndroidMavenPlugin() throws Exception {
        // no 'reportsDirectory' or so config value set:
        MojoInfo mojoInfo = MojoInfoBuilder.mojoBuilder(
        			"com.jayway.maven.plugins.android.generation2",
        			"android-maven-plugin",
        			"internal-integration-test")
        	.version("3.3.0")
        	.build();

        final String testResultsName = "TEST-emulator-5554_device2.1_unknown_google_sdk.xml";
		
		File testDir = hudson.Util.createTempDir();
		File targetDir = new File(testDir, "target");
		File reportsDir = new File(targetDir, "surefire-reports");
		assertTrue(reportsDir.mkdirs());
		
		File testResults = new File(reportsDir, testResultsName);
        try {
		FileWriter fw = new FileWriter(testResults, false);
		fw.write("this is a fake surefire reports output file");
		fw.close();

        MavenProject pom = mock(MavenProject.class);
        when(pom.getBasedir()).thenReturn(testDir);
        
        Build build = mock(Build.class);
        when(build.getDirectory()).thenReturn(targetDir.getAbsolutePath());
        when(pom.getBuild()).thenReturn(build);
        
        TestMojo testMojo = TestMojo.ANDROID_MAVEN_PLUGIN;
        Iterable<File> files = testMojo.getReportFiles(pom, mojoInfo);
        assertNotNull(files, "no report files returned");

        boolean found = false;
        for (File file : files) {
        	assertEquals(testResultsName, file.getName());
        	found = true;
        }
        assertTrue(found, "report file not found");
        } finally {
            testResults.delete();
        }
    }

    @Test
    void testScalatestMavenPluginNoJunitxml() throws Exception {
        runScalatestPluginTestNoEntries(null);
    }

    @Test
    void testScalatestMavenPluginEmptyJunitxml() throws Exception {
        runScalatestPluginTestNoEntries("");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasListOfEmptyEntries() throws Exception {
        runScalatestPluginTestNoEntries(", ");
    }

    @Test
    void testScalatestMavenPluginJunitxmlEntryDoesntExist() throws Exception {
        runScalatestPluginTestNoEntries("foo");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasOneEntry() throws Exception {
        runScalatestPluginTestOneEntry("ut-xml", "ut-xml");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasOneEntryAndTrailingDummy() throws Exception {
        runScalatestPluginTestOneEntry("ut-xml,", "ut-xml");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasOneEntryAndLeadingDummy() throws Exception {
        runScalatestPluginTestOneEntry(",ut-xml", "ut-xml");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasOneEntryAndDummiesOnBothEnds() throws Exception {
        runScalatestPluginTestOneEntry(",ut-xml,", "ut-xml");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasMultipleEntries() throws Exception {
        runScalatestPluginTestOneEntry("ut-xml, foo, baz", "ut-xml");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasMultipleEntriesAndFirstOneInvalid()
        throws Exception {
        runScalatestPluginTestOneEntry("foo, ut-xml, baz", "ut-xml");
    }

    @Test
    void testScalatestMavenPluginJunitxmlHasMultipleEntriesWithEscapedCommas()
        throws Exception {
        runScalatestPluginTestOneEntry("ut\\,xml, foo, baz", "ut,xml");
    }

    private void runScalatestPluginTestNoEntries(final String junitxml) throws Exception {
        runScalatestPluginTest("", (ScalatestPluginTest) (mojoBuilder, pom, testResultsName) -> {
            if (junitxml != null)
                mojoBuilder = mojoBuilder.configValue("junitxml", junitxml);
            MojoInfo mojoInfo = mojoBuilder.build();

            Iterable<File> files =
                    TestMojo.SCALATEST_MAVEN_PLUGIN.getReportFiles(pom, mojoInfo);
            assertNull(files, "unexpected report files returned");
        });
    }

    private void runScalatestPluginTestOneEntry(final String junitxml, String expectedDir)
            throws Exception {
        runScalatestPluginTest(expectedDir, (ScalatestPluginTest) (mojoBuilder, pom, testResultsName) -> {
            MojoInfo mojoInfo = mojoBuilder.configValue("junitxml", junitxml).build();

            Iterable<File> files =
                    TestMojo.SCALATEST_MAVEN_PLUGIN.getReportFiles(pom, mojoInfo);
            assertHasOneFile(files, testResultsName);
        });
    }

    private void assertHasOneFile(Iterable<File> files, String expectedFile) {
        assertNotNull(files, "no report files returned");

        boolean found = false;
        for (File file : files) {
            assertFalse(found, "unexpected report files returned: " + file);
            assertEquals(expectedFile, file.getName());
            found = true;
        }
        assertTrue(found, "report file not found");
    }

    private void runScalatestPluginTest(String junitXmlDirName, ScalatestPluginTest test)
            throws Exception {
        final String testResultsName = "TEST-are-we-foobared.xml";

        File testDir = hudson.Util.createTempDir();
        File targetDir = new File(testDir, "target");
        File reportsDir = new File(targetDir, "scalatest-reports");
        File junitXmlDir = new File(reportsDir, junitXmlDirName);
        assertTrue(junitXmlDir.mkdirs());

        MojoInfoBuilder mojoBuilder =
                MojoInfoBuilder.mojoBuilder("org.scalatest", "scalatest-maven-plugin", "test")
                               .configValue("reportsDirectory", reportsDir.toString());

        File testResults = new File(junitXmlDir, testResultsName);
        try {
            FileWriter fw = new FileWriter(testResults, false);
            fw.write("this is a fake junit reports file");
            fw.close();

            MavenProject pom = mock(MavenProject.class);
            when(pom.getBasedir()).thenReturn(testDir);

            Build build = mock(Build.class);
            when(build.getDirectory()).thenReturn(targetDir.getAbsolutePath());
            when(pom.getBuild()).thenReturn(build);

            test.run(mojoBuilder, pom, testResultsName);
        } finally {
            testResults.delete();
        }
    }

    private interface ScalatestPluginTest {
        void run(MojoInfoBuilder mojoBuilder, MavenProject pom,
                 String testResultsName) throws Exception;
    }

    @Test
    @Issue("JENKINS-31258")
    void testGetReportFilesUnknownTestPlugin() throws Exception {
        final String testResultsName = "TEST-some.test.results.xml";

        final File baseDir = hudson.Util.createTempDir();
        final File targetDir = new File(baseDir, "target");
        final File reportsDir = new File(targetDir, "some-test-reports");
        assertTrue(reportsDir.mkdirs());

        MojoInfoBuilder builder = MojoInfoBuilder.mojoBuilder("com.some", "unknown-test-capable-plugin", "somegoal")
                .evaluator(new ExpressionEvaluator() {
                    @Override
                    public Object evaluate(String expression) {
                        if ("${jenkins.some-test-execution-id.reportsDirectory}".equals(expression))
                            return "target/some-test-reports";
                        return expression;
                    }

                    @Override
                    public File alignToBaseDirectory(File path) {
                        return new File(baseDir, path.getPath());
                    }
                });

        MojoInfo nonReportingMojo = builder.executionId("some-non-test-execution-id").build();
        TestMojo testMojo = TestMojo.lookup(nonReportingMojo);
        assertNull(testMojo, "misreported an unknown, unconfigured MOJO as test capable");

        MojoInfo reportingMojo = builder.executionId("some-test-execution-id").build();
        testMojo = TestMojo.lookup(reportingMojo);
        assertNotNull(testMojo, "failed to recognize correctly configured unknown test capable MOJO");

        MavenProject pom = mock(MavenProject.class);
        when(pom.getBasedir()).thenReturn(baseDir);

        Build build = mock(Build.class);
        when(build.getDirectory()).thenReturn(targetDir.getAbsolutePath());
        when(pom.getBuild()).thenReturn(build);

        assertEquals(new File(baseDir, "target/some-test-reports"),
                testMojo.getReportsDirectory(pom, reportingMojo),
                "test mojo returned incorrect reports directory");

        File testResults = new File(reportsDir, testResultsName);
        try {
            FileWriter fw = new FileWriter(testResults, false);
            fw.write("this is a fake surefire reports output file");
            fw.close();

            Iterable<File> files = testMojo.getReportFiles(pom, reportingMojo);
            assertNotNull(files, "no report files returned");

            boolean found = false;
            for (File file : files) {
                assertEquals(testResultsName, file.getName());
                found = true;
            }
            assertTrue(found, "report file not found");
        } finally {
            hudson.Util.deleteRecursive(baseDir);
        }
    }

}
