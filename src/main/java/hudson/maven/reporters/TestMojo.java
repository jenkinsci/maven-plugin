package hudson.maven.reporters;

import hudson.Util;
import hudson.maven.MojoInfo;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

/**
 * Description of a mojo which can run tests.
 * 
 * @author kutzi
 */
enum TestMojo {
    
    /**
     * Fallback to this if we have no exact match
     */
    FALLBACK("","","","reportsDirectory") {
        @Override
        protected boolean is(String artifactId, String groupId, String goal) {
            // never match anything implicitly
            return false;
        }
    },
    
    MAVEN_SUREFIRE("org.apache.maven.plugins", "maven-surefire-plugin","test","reportsDirectory"),
    MAVEN_QUNIT("net.kennychua", "phantomjs-qunit-runner","test","null"){
        @Override
        public Iterable<File> getReportFiles(MavenProject pom, MojoInfo mojo)
                throws ComponentConfigurationException {

            String buildDirectory = mojo.getConfigurationValue("buildDirectory", String.class);

            if (buildDirectory == null || buildDirectory.trim().length() == 0) {
                return null;
            }

            File junitDir = new File(buildDirectory, "junitxml");
            if (junitDir.exists()) {
                return super.getReportFiles(junitDir, super.getFileSet(junitDir));
            }
            return null;
        }
    },
    MAVEN_FAILSAFE("org.apache.maven.plugins", "maven-failsafe-plugin", "integration-test","reportsDirectory"),
    MAVEN_FAILSAFE_B("org.apache.maven.plugins", "maven-failsafe-plugin", "verify","reportsDirectory"),
    
    MAVEN_JUNIT("com.sun.maven", "maven-junit-plugin", "test","reportsDirectory"),
    FLEXMOJOS("org.sonatype.flexmojos", "flexmojos-maven-plugin", "test-run",null),
    
    MAVEN_OSGI_TEST("org.sonatype.tycho", "maven-osgi-test-plugin", "test","reportsDirectory"),
    TYCHO_SUREFIRE("org.eclipse.tycho", "tycho-surefire-plugin", "test","reportsDirectory"),
    
    MAVEN_ANDROID_PLUGIN("com.jayway.maven.plugins.android.generation2", "maven-android-plugin",
            "internal-integration-test",null,"3.0.0-alpha-6"),
    ANDROID_MAVEN_PLUGIN("com.jayway.maven.plugins.android.generation2", "android-maven-plugin",
            "internal-integration-test",null,"3.0.0-alpha-6"),
    ANDROID_MAVEN_PLUGIN_2("com.simpligility.maven.plugins", "android-maven-plugin", "internal-integration-test", null),

    GWT_MAVEN_PLUGIN("org.codehaus.mojo", "gwt-maven-plugin", "test","reportsDirectory","1.2"),
    
    MAVEN_SOAPUI_PLUGIN("com.smartbear.soapui", "soapui-maven-plugin", "test", "outputFolder"),
    MAVEN_SOAPUI_PRO_PLUGIN("com.smartbear.soapui", "soapui-pro-maven-plugin", "test","outputFolder"),
    MAVEN_SOAPUI_EXTENSION_PLUGIN("com.github.redfish4ktc.soapui", "maven-soapui-extension-plugin", "test","outputFolder"),
    
    JASMINE("com.github.searls","jasmine-maven-plugin","test",null) {
        @Override
        public Collection<File> getReportFiles(MavenProject pom,MojoInfo mojo)
                throws ComponentConfigurationException {
            // jasmine just creates a single JUnit result file
            File reportsDir = mojo.getConfigurationValue("jasmineTargetDir", File.class);
            String junitFileName = mojo.getConfigurationValue("junitXmlReportFileName", String.class);
            
            if (reportsDir != null && junitFileName != null) {
                return Collections.singleton(new File(reportsDir,junitFileName));
            }
            return null;
        }
    },
    TOOLKIT_RESOLVER_PLUGIN("org.terracotta.maven.plugins", "toolkit-resolver-plugin", "toolkit-resolve-test","reportsDirectory"),
    SCALATEST_MAVEN_PLUGIN("org.scalatest", "scalatest-maven-plugin", "test", null) {
        @Override
        public Iterable<File> getReportFiles(MavenProject pom, MojoInfo mojo)
                throws ComponentConfigurationException {
            /* scalatest-maven-plugin has a configuration entry 'junitxml' which is a
             * comma-separated list of directories; commas may be escaped with a backslash
             * (\,). Each directory is taken relative to the reportsDirectory.
             */
            File reportsDir = mojo.getConfigurationValue("reportsDirectory", File.class);
            String junitDirs = mojo.getConfigurationValue("junitxml", String.class);

            if (junitDirs == null || junitDirs.trim().length() == 0) {
                return null;
            }

            // split along non-escaped commas
            String[] junitDirsList = junitDirs.trim().split("(?<!\\\\),");
            for (String dir : junitDirsList) {
                if (dir.trim().length() > 0) {
                    // unescape escaped commas
                    String junitDirName = dir.trim().replaceAll("\\\\,", ",");
                    File junitDir = new File(reportsDir, junitDirName);
                    if (junitDir.exists()) {
                        return super.getReportFiles(junitDir, super.getFileSet(junitDir));
                    }
                }
            }

            return null;
        }
    },
    PLAY2_MAVEN_PLUGIN("org.nanoko.playframework", "play2-maven-plugin", "test", null) {
        @Override
        public Iterable<File> getReportFiles(MavenProject pom, MojoInfo mojo)
                throws ComponentConfigurationException {
            /* play2-maven-plugin just run `play test` - this one dumps test reports to target/test-reports */
            File junitDir = new File(pom.getBasedir(), "target/test-reports");
            if (junitDir.exists()) {
                return super.getReportFiles(junitDir, super.getFileSet(junitDir));
            }
            return null;
        }        
    };

    private String reportDirectoryConfigKey;
    private Key key;
    private String minimalRequiredVersion;
    
    private TestMojo(String artifactId, String groupId, String goal,
            String reportDirectoryConfigKey) {
        this.key = new Key(artifactId,groupId,goal);
        this.reportDirectoryConfigKey = reportDirectoryConfigKey;
    }
    
    private TestMojo(String artifactId, String groupId, String goal,
            String reportDirectoryConfigKey,String minimalRequiredVersion) {
        this.key = new Key(artifactId,groupId,goal);
        this.reportDirectoryConfigKey = reportDirectoryConfigKey;
        this.minimalRequiredVersion = minimalRequiredVersion;
    }
    
    public Key getKey() {
        return this.key;
    }
    
    /**
     * Says if this mojo can run tests.
     * Can e.g. return false if the version of the plugin is too old to create output in JUnit format.
     */
    public boolean canRunTests(MojoInfo mojo) {
        if (this.minimalRequiredVersion == null) {
            return true;
        }
        
        return mojo.pluginName.version.compareTo(this.minimalRequiredVersion) >= 0;
    }
    
    @CheckForNull public Iterable<File> getReportFiles(MavenProject pom, MojoInfo mojo) throws ComponentConfigurationException {
        File reportsDir = getReportsDirectory(pom, mojo);
        if (reportsDir.exists()) {
            return getReportFiles(reportsDir, getFileSet(reportsDir));
        }
        
        return null;
    }

    /**
     * Returns the location of test reports created by the specified MOJO.
     * @param pom The project model.
     * @param mojo The MOJO.
     * @return The directory containing the test reports.
     * @throws ComponentConfigurationException if unable to retrieve the report directory from the MOJO configuration.
     */
    @Nonnull protected File getReportsDirectory(MavenProject pom, MojoInfo mojo) throws ComponentConfigurationException {
        // [JENKINS-31258] Allow unknown MOJOs to contribute test results in arbitrary locations by setting a Maven property.
        String reportsDirectoryOverride = getReportsDirectoryOverride(mojo);
        if (reportsDirectoryOverride != null)
            return mojo.expressionEvaluator.alignToBaseDirectory(new File(reportsDirectoryOverride));

        if (this.reportDirectoryConfigKey != null) {
            File reportsDir = mojo.getConfigurationValue(this.reportDirectoryConfigKey, File.class);
            if (reportsDir != null)
                return reportsDir;
        }

        // some plugins just default to this:        
        return new File(pom.getBuild().getDirectory(), "surefire-reports");
    }
    
    private Iterable<File> getReportFiles(final File baseDir, FileSet set) {
        final String[] includedFiles = set.getDirectoryScanner().getIncludedFiles();
        
        return new Iterable<File>() {
            public Iterator<File> iterator() {
                return Iterators.transform(
                    Iterators.forArray(includedFiles),
                    new Function<String, File>() {
                        @Override
                        public File apply(String file) {
                            return new File(baseDir,file);
                        }
                    });
            }
        };
    }
    
    /**
     * Returns the appropriate FileSet for the selected baseDir
     * @param baseDir
     * @return
     */
    private FileSet getFileSet(File baseDir) {
        return Util.createFileSet(baseDir, "*.xml","testng-results.xml,testng-failed.xml");
    }
    
    protected boolean is(String artifactId, String groupId, String goal) {
        return key.artifactId.equals(artifactId) && key.groupId.equals(groupId)
                && key.goal.equals(goal);
    }
    
    public static TestMojo lookup(String artifactId, String groupId, String goal) {
        for (TestMojo mojo : values()) {
            if (mojo.is(artifactId,groupId,goal)) {
                return mojo;
            }
        }
        
        if (goal.equals("test") || goal.equals("test-run") || goal.equals("integration-test")) {
            return FALLBACK;
        }
        
        return null;
    }
    
    public static TestMojo lookup(MojoInfo mojo) {
        TestMojo testMojo = lookup(mojo.pluginName.groupId, mojo.pluginName.artifactId, mojo.getGoal());

        if (testMojo == null && getReportsDirectoryOverride(mojo) != null) {
            testMojo = FALLBACK;
        }

        if (testMojo != null && testMojo.canRunTests(mojo)) {
            return testMojo;
        }

        return null;
    }

    /**
     * For an unknown test-capable MOJO, returns the path to its reports directory as configured by a Maven property.
     * @param mojo An unknown MOJO.
     * @return the value of the expression <code>${jenkins.&lt;mojo-execution-id&gt;.reportsDirectory}</code>.
     */
    @CheckForNull private static String getReportsDirectoryOverride(MojoInfo mojo) {
    	// TODO: if and when this MOJO ceases to be tested against earlier Maven versions lacking MojoExecution.getLifecyclePhase(),
    	// Reinstate the following validity check:
        // Validity of the override property should be constrained to "test" and "integration-test" phases but there seems to be no
        // way to check this, as MojoExecution.getLifecyclePhase() (added 2009-05-12) causes tests to throw NoSuchMethodError!!!
        // Until then, we're obliged to assume that the property is configured for a valid test life cycle phase.
//        String phase = mojo.mojoExecution.getLifecyclePhase();
//        if ("test".equals(phase) || "integration-test".equals(phase)) {
        try {
            String reportsDirExpr = "${jenkins." + mojo.mojoExecution.getExecutionId() + ".reportsDirectory}";
            Object result = mojo.expressionEvaluator.evaluate(reportsDirExpr);
            if (result != null && !result.equals(reportsDirExpr) && !result.toString().trim().isEmpty())
                return result.toString().trim();
        } catch (ExpressionEvaluationException e) {
            // Note: no access to private MavenProject.logger.
        	e.printStackTrace();
        } 
//        }
        return null;
    }

    static class Key {
        private String artifactId;
        private String groupId;
        private String goal;
        
        public Key(String artifactId, String groupId, String goal) {
            super();
            this.artifactId = artifactId;
            this.groupId = groupId;
            this.goal = goal;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((goal == null) ? 0 : goal.hashCode());
            result = prime * result
                    + ((groupId == null) ? 0 : groupId.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (artifactId == null) {
                if (other.artifactId != null)
                    return false;
            } else if (!artifactId.equals(other.artifactId))
                return false;
            if (goal == null) {
                if (other.goal != null)
                    return false;
            } else if (!goal.equals(other.goal))
                return false;
            if (groupId == null) {
                if (other.groupId != null)
                    return false;
            } else if (!groupId.equals(other.groupId))
                return false;
            return true;
        }
    }

}
