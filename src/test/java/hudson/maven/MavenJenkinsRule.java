package hudson.maven;

import hudson.EnvVars;
import hudson.slaves.CommandLauncher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

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

    /**
     * Fix the focus issue when a JVM is created to launch a slave by adding {@link #JAVA_HEADLESS_OPT}
     */
    @Override
    public CommandLauncher createComputerLauncher(EnvVars env) throws URISyntaxException, MalformedURLException {
        int sz = this.jenkins.getNodes().size();
        return new CommandLauncher(String.format("\"%s/bin/java\" %s %s -jar \"%s\"",
                new Object[]{System.getProperty("java.home"), JAVA_HEADLESS_OPT, SLAVE_DEBUG_PORT > 0 ?
                        " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=" + (SLAVE_DEBUG_PORT + sz) : "",
                        (new File(this.jenkins.getJnlpJars("slave.jar").getURL().toURI())).getAbsolutePath()}), env);
    }

}
