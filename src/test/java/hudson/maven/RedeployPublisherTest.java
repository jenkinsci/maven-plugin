/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.maven;

import hudson.model.Result;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.tasks.RedeployBatchTaskPublisher;
import hudson.tasks.Maven.MavenInstallation;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import jenkins.mvn.FilePathSettingsProvider;
import org.apache.commons.lang3.StringUtils;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import org.jvnet.hudson.test.*;
import org.jvnet.hudson.test.junit.jupiter.BuildWatcherExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class RedeployPublisherTest {

    @SuppressWarnings("unused")    
    @RegisterExtension
    private static final BuildWatcherExtension BUILD_WATCHER = new BuildWatcherExtension();

    @TempDir
    private File tmp;

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @RandomlyFails("Not a v4.0.0 POM. for project org.jvnet.maven-antrun-extended-plugin:maven-antrun-extended-plugin at /home/jenkins/.m2/repository/org/jvnet/maven-antrun-extended-plugin/maven-antrun-extended-plugin/1.39/maven-antrun-extended-plugin-1.39.pom")
    @Issue("JENKINS-2593")
    @Test
    void testBug2593() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m2 = j.jenkins.createProject(MavenModuleSet.class, "p");
        File repo = tmp;

        // a fake build
        m2.setScm(new SingleFileSCM("pom.xml",getClass().getResource("big-artifact.pom")));
        m2.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),true, false));

        MavenModuleSetBuild b = m2.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);

        // TODO: confirm that the artifacts use a consistent timestamp
        // TODO: we need to somehow introduce a large delay between deploy since timestamp is only second precision
        // TODO: or maybe we could use a btrace like capability to count the # of invocations?

        System.out.println(repo);
    }

    @Test
    void testConfigRoundtrip() throws Exception {
        MavenModuleSet p = j.jenkins.createProject(MavenModuleSet.class, "p");
        RedeployPublisher rp = new RedeployPublisher("theId", "http://some.url/", true, true);
        p.getPublishersList().add(rp);
        j.submit(j.createWebClient().getPage(p,"configure").getFormByName("config"));
        j.assertEqualBeans(rp,p.getPublishersList().get(RedeployPublisher.class),"id,url,uniqueVersion,evenIfUnstable");
    }

    /**
     * Are we having a problem in handling file names with multiple extensions, like ".tar.gz"?
     */
    @Email("http://www.nabble.com/tar.gz-becomes-.gz-after-Hudson-deployment-td25391364.html")
    @Issue("JENKINS-3814")
    @Test
    void testTarGz() throws Exception {
        ToolInstallations.configureMaven35();
        MavenModuleSet m2 = j.jenkins.createProject(MavenModuleSet.class, "p");
        File repo = tmp;

        // a fake build
        m2.setScm(new SingleFileSCM("pom.xml",getClass().getResource("targz-artifact.pom")));
        m2.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),false, false));

        MavenModuleSetBuild b = m2.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);

        String[] files =
            new File(repo,"test/test/0.1-SNAPSHOT").list((dir, name) -> name.startsWith("test-0.1-") && name.endsWith("-1-bin.tar.gz"));

        assertEquals(1, files.length, "tar.gz doesn't exist");
    }

    @Test
    void testTarGzUniqueVersionTrue() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m2 = j.jenkins.createProject(MavenModuleSet.class, "p");
        File repo = tmp;
        
        // a fake build
        m2.setScm(new SingleFileSCM("pom.xml",getClass().getResource("targz-artifact.pom")));
        m2.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),true, false));

        MavenModuleSetBuild b = m2.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);

        File artifactDir = new File(repo,"test/test/0.1-SNAPSHOT/");
        String[] files = artifactDir.list((dir, name) -> {
            System.out.print("deployed file " + name);
            return name.contains("-bin.tar.gz") || name.endsWith(".jar") || name.endsWith("-bin.zip");
        });
        System.out.println("deployed files " + Arrays.asList(files));
        assertFalse(new File(repo,"test/test/0.1-SNAPSHOT/test-0.1-SNAPSHOT-bin.tar.gz").exists(),"tar.gz doesn't exist");
        assertFalse(files[0].contains("SNAPSHOT"), "tar.gz doesn't exist");
        for (String file : files) {
            if (file.endsWith("-bin.tar.gz")) {
                String ver = StringUtils.remove(file, "-bin.tar.gz");
                ver = ver.substring(ver.length() - 1);
                assertEquals("1", ver, "-bin.tar.gz not ended with 1 , file " + file);
            }
            if (file.endsWith(".jar")) {
                String ver = StringUtils.remove(file, ".jar");
                ver = ver.substring(ver.length() - 1);
                assertEquals("1", ver, ".jar not ended with 1 , file " + file);
            }            
            if (file.endsWith("-bin.zip")) {
                String ver = StringUtils.remove(file, "-bin.zip");
                ver = ver.substring(ver.length() - 1);
                assertEquals("1", ver, "-bin.zip not ended with 1 , file " + file);
            }            
        }
        assertEquals(1, b.getModuleBuilds().size());
        assertEquals(1, b.getModuleBuilds().values().iterator().next().size());
        assertEquals(5, b.getModuleBuilds().values().iterator().next().iterator().next().getArtifacts().size());
    }

    @Test
    void testTarGzMaven3() throws Exception {
        MavenModuleSet m3 = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mvn = Maven36xBuildTest.configureMaven36();
        m3.setMaven(mvn.getName());
        File repo = tmp;
        // a fake build
        m3.setScm(new SingleFileSCM("pom.xml",getClass().getResource("targz-artifact.pom")));
        m3.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),false, false));

        MavenModuleSetBuild b = m3.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);

        assertTrue(MavenUtil.maven3orLater(b.getMavenVersionUsed()));
        File artifactDir = new File(repo,"test/test/0.1-SNAPSHOT/");
        String[] files = artifactDir.list((dir, name) -> name.endsWith("tar.gz"));
        assertFalse(new File(repo,"test/test/0.1-SNAPSHOT/test-0.1-SNAPSHOT-bin.tar.gz").exists(),"tar.gz doesn't exist");
        assertFalse(files[0].contains("SNAPSHOT"), "tar.gz doesn't exist");
    }

    @Test
    void testTarGzUniqueVersionTrueMaven3() throws Exception {
        MavenModuleSet m3 = j.jenkins.createProject(MavenModuleSet.class, "p");
        MavenInstallation mvn = Maven36xBuildTest.configureMaven36();
        m3.setMaven(mvn.getName());        
        File repo = tmp;
        // a fake build
        m3.setScm(new SingleFileSCM("pom.xml",getClass().getResource("targz-artifact.pom")));
        m3.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),true, false));

        MavenModuleSetBuild b = m3.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, b);
        
        assertTrue(MavenUtil.maven3orLater(b.getMavenVersionUsed()));
        
        File artifactDir = new File(repo,"test/test/0.1-SNAPSHOT/");
        String[] files = artifactDir.list((dir, name) -> name.contains("-bin.tar.gz") || name.endsWith(".jar") || name.endsWith("-bin.zip"));
        System.out.println("deployed files " + Arrays.asList(files));
        assertFalse(new File(repo,"test/test/0.1-SNAPSHOT/test-0.1-SNAPSHOT-bin.tar.gz").exists(),"tar.gz doesn't exist");
        assertFalse(files[0].contains("SNAPSHOT"), "tar.gz doesn't exist");
        for (String file : files) {
            if (file.endsWith("-bin.tar.gz")) {
                String ver = StringUtils.remove(file, "-bin.tar.gz");
                ver = ver.substring(ver.length() - 1);
                assertEquals("1", ver, "-bin.tar.gz not ended with 1 , file " + file);
            }
            if (file.endsWith(".jar")) {
                String ver = StringUtils.remove(file, ".jar");
                ver = ver.substring(ver.length() - 1);
                assertEquals("1", ver, ".jar not ended with 1 , file " + file);
            }            
            if (file.endsWith("-bin.zip")) {
                String ver = StringUtils.remove(file, "-bin.zip");
                ver = ver.substring(ver.length() - 1);
                assertEquals("1", ver, "-bin.zip not ended with 1 , file " + file);
            }            
        }
    }

    @Issue("JENKINS-3773")
    @Test
    void testDeployUnstable() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m2 = j.jenkins.createProject(MavenModuleSet.class, "p");
        File repo = tmp;
        // a build with a failing unit tests
        m2.setScm(new ExtractResourceSCM(getClass().getResource("maven-test-failure-findbugs.zip")));
        m2.getPublishersList().add(new RedeployPublisher("",repo.toURI().toString(),false, true));

        MavenModuleSetBuild b = m2.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.UNSTABLE, b);

        assertTrue(new File(repo,"test/test/1.0-SNAPSHOT").isDirectory(),
            // exact filename unpreductable in M3, e.g. test-1.0-20160317.213607-1.jar
                   "Artifact should have been published even when the build is unstable");
    }

    @Issue("JENKINS-7010")
    @Test
    void testSettingsInsidePromotion() throws Exception {
        ToolInstallations.configureMaven35();
        MavenModuleSet m2 = j.jenkins.createProject(MavenModuleSet.class, "p");
        File repo = tmp;
        URL resource = RedeployPublisherTest.class.getResource("settings.xml");
        File customUserSettings = new File(resource.toURI().getPath());

        // Let's configure a maven build with a custom settings.xml file
        m2.setScm(new SingleFileSCM("pom.xml", getClass().getResource("simple-pom.xml")));
        m2.setSettings(new FilePathSettingsProvider(customUserSettings.getAbsolutePath()));

        // Let's configure a promotion step automatically executed after the build and launching a RedeployBatchTaskPublisher
        JobPropertyImpl property = new JobPropertyImpl(m2);
        PromotionProcess promotionProcess = property.addProcess("deploy");
        promotionProcess.conditions.add(new SelfPromotionCondition(false));
        promotionProcess.getBuildSteps().add(new RedeployBatchTaskPublisher("", repo.toURI().toString(), false));
        m2.addProperty(property);

        // Let's launch the build
        MavenModuleSetBuild b = m2.scheduleBuild2(0).get();
        // It should pass
        j.assertBuildStatus(Result.SUCCESS, b);

        // Let's verify the promotion
        PromotedBuildAction promotedBuildAction = b.getAction(PromotedBuildAction.class);

        while (promotedBuildAction.getPromotion("deploy").getLast() == null ||
                promotedBuildAction.getPromotion("deploy").getLast().isBuilding()) {
            // Let's wait for the end of the promotion
            Thread.sleep(100);
        }

        // We should have one promotion
        assertEquals(1, promotedBuildAction.getPromotions().size(), "1 promotion expected");

        // The promotion should succeed
        assertEquals(Result.SUCCESS, promotedBuildAction.getPromotion("deploy").getLast().getResult(), "promotion succeeded");

        // no more unique version but only timestamped
        String[] poms = new File(repo,"test/maven/simple-pom/1.0-SNAPSHOT/").list((dir, name) ->
            name.startsWith("simple-pom-1.0-") && name.endsWith("-1.pom"));
        // It should have deployed the artifact
        assertEquals(1, poms.length, "Artifact should have been published");
        // The RedeployPublisher should display the information that it used the custom settings.xml file
        // There is no easy solution to test that the custom settings are used excepted the log
        j.assertLogContains(customUserSettings.getAbsolutePath(),promotedBuildAction.getPromotion("deploy").getLast());
    }
}
