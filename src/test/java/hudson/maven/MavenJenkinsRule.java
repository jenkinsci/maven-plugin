package hudson.maven;

import org.jvnet.hudson.test.JenkinsRule;

/**
 * Customisation of the JenkinsRule to fix the lost of window focus when tests are launched on MacOS.
 */
public class MavenJenkinsRule extends JenkinsRule {

    /**
     * On MacOS it is important to launch JVMs with `-Djava.awt.headless=true` to avoid to have the Java process
     * to appear as desktop app and to steal the window focus when you are in a fullscreen application.
     */
    public static final String JAVA_HEADLESS_OPT = "-Djava.awt.headless=true";

    /**
     * Fix the focus issue when a JVM is created to launch a maven build by adding {@link #JAVA_HEADLESS_OPT} to the global MAVEN_OPTS
     */
    @Override
    public void before() throws Throwable {
        super.before();
        // prevent the annoying Agent and Maven processes from stealing window focus on Mac OS
        jenkins.getDescriptorByType(MavenModuleSet.DescriptorImpl.class).setGlobalMavenOpts(JAVA_HEADLESS_OPT);
    }

}
