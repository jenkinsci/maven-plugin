/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
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

import hudson.maven.local_repo.DefaultLocalRepositoryLocator;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.Shell;

import java.io.File;

import jenkins.model.Jenkins;
import jenkins.mvn.DefaultGlobalSettingsProvider;
import jenkins.mvn.DefaultSettingsProvider;
import jenkins.mvn.FilePathGlobalSettingsProvider;
import jenkins.mvn.FilePathSettingsProvider;
import jenkins.mvn.GlobalMavenConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;

import java.net.HttpURLConnection;

import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author huybrechts
 */
@WithJenkins
class MavenProjectTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testOnMaster() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("validate");

        j.buildAndAssertSuccess(project);
        String xml = project.getModules().iterator().next().getConfigFile().asString();
        assertTrue(xml.contains("<maven2"), xml);
        assertFalse(xml.contains("<maven2-module-set"), xml);
    }
    
    @Issue("JENKINS-16499")
    @Test
    void testCopyFromExistingMavenProject() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("abcdefg");
        project.save();
        
        MavenModuleSet copy = (MavenModuleSet) Jenkins.get().copy((AbstractProject<?, ?>)project, "copy" + System.currentTimeMillis());
        assertNotNull(copy, "Copied project must not be null");
        assertEquals(project.getGoals(), copy.getGoals());
    }

    private MavenModuleSet createSimpleProject() throws Exception {
        return createProject("/simple-projects.zip");
    }

    private MavenModuleSet createProject(final String scmResource) throws Exception {
        MavenModuleSet project = j.createProject(MavenModuleSet.class, "p");
        MavenInstallation mi = ToolInstallations.configureMaven35();
        project.setScm(new ExtractResourceSCM(getClass().getResource(
                scmResource)));
        project.setMaven(mi.getName());
        // we don't want to download internet again for unit tests
        // so local repo from user settings
        project.setSettings(new DefaultSettingsProvider());
        //project.setLocalRepository(new PerExecutorLocalRepositoryLocator());
        return project;
    }

    @Test
    void testOnSlave() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("validate");
        project.setAssignedLabel(j.createSlave().getSelfLabel());

        j.buildAndAssertSuccess(project);
    }

    /**
     * Check if the generated site is linked correctly.
     */
    @Issue("JENKINS-3497")
    @Test
    void testSiteBuild() throws Exception {
        MavenModuleSet project = createSimpleProject();
        project.setGoals("site -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");

        j.buildAndAssertSuccess(project);

        // this should succeed
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(project,"site");
        wc.assertFails(project.getUrl() + "site/no-such-file", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Check if the generated site is linked correctly for multi module projects.
     */
    @Test
    void testMultiModuleSiteBuild() throws Exception {
        MavenModuleSet project = createProject("maven-multimodule-site.zip");
        project.setGoals("site -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");

        try {
            j.buildAndAssertSuccess(project);
        } catch (InterruptedException x) {
            // jglick: when using M2 this just hangs on my machine (pool-*-thread-* in java.net.SocketInputStream.socketRead0); sometimes passes in M3, but not consistently, and very very slowly when it does (network dependency)
            return; // TODO use JenkinsRule and throw AssumptionViolatedException
        }

        // this should succeed
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(project, "site");
        wc.getPage(project, "site/core");
        wc.getPage(project, "site/client");

        //@Issue("JENKINS-7577): check that site generation succeeds also if only a single module is build
        MavenModule coreModule = project.getModule("mmtest:core");
        assertEquals("site -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8", coreModule.getGoals());
        try {
            j.buildAndAssertSuccess(coreModule);
        } catch (InterruptedException x) {
            return; // TODO as above
        }
        wc.getPage(project, "site/core");
    }
    
    @Test
    void testNestedMultiModuleSiteBuild() throws Exception {
        MavenModuleSet project = createProject("maven-nested-multimodule-site.zip");
        project.setGoals("site -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");

        try {
            j.buildAndAssertSuccess(project);
        } catch (InterruptedException x) {
            return; // TODO as above
        }

        // this should succeed
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(project, "site");
        wc.getPage(project, "site/core");
        wc.getPage(project, "site/client");
        wc.getPage(project, "site/client/nested1");
        wc.getPage(project, "site/client/nested1/nested2");

    }
    
    /**
     * Check if the the site goal will work when run from an agent.
     */
    @Issue("JENKINS-5943")
    @Test
    void testMultiModuleSiteBuildOnSlave() throws Exception {
        MavenModuleSet project = createProject("maven-multimodule-site.zip");
        project.setGoals("site -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        project.setAssignedLabel(j.createSlave().getSelfLabel());

        try {
            j.buildAndAssertSuccess(project);
        } catch (InterruptedException x) {
            return; // TODO as above
        }

        // this should succeed
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(project, "site");
        wc.getPage(project, "site/core");
        wc.getPage(project, "site/client");
    }

    @Issue("JENKINS-6779")
    @Test
    void testDeleteSetBuildDeletesModuleBuilds() throws Exception {
        MavenModuleSet project = createProject("maven-multimod.zip");
        project.setLocalRepository(new DefaultLocalRepositoryLocator());
        project.setGoals("install -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        j.buildAndAssertSuccess(project);
        j.buildAndAssertSuccess(project.getModule("org.jvnet.hudson.main.test.multimod:moduleB"));
        j.buildAndAssertSuccess(project);
        assertEquals(2, project.getBuilds().size()); // Module build does not add a ModuleSetBuild
        project.getFirstBuild().delete();
        // A#1, B#1 and B#2 should all be deleted too
        assertEquals(1, project.getModule("org.jvnet.hudson.main.test.multimod:moduleA").getBuilds().size());
        assertEquals(1, project.getModule("org.jvnet.hudson.main.test.multimod:moduleB").getBuilds().size());
    }
    @Issue("JENKINS-7261")
    @Test
    void testAbsolutePathPom() throws Exception {
        File pom = new File(this.getClass().getResource("test-pom-7162.xml").toURI());
        MavenModuleSet project = j.createProject(MavenModuleSet.class, "p");
        MavenInstallation mi = Maven36xBuildTest.configureMaven36();
        project.setMaven(mi.getName());
        project.setRootPOM(pom.getAbsolutePath());
        project.setGoals("install -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        j.buildAndAssertSuccess(project);
    }
    
    @Issue("JENKINS-17177")
    @Test
    void testCorrectResultInPostStepAfterFailedPreBuildStep() throws Exception {
        MavenModuleSet p = createSimpleProject();
        MavenInstallation mi = Maven36xBuildTest.configureMaven36();
        p.setMaven(mi.getName());
        p.setGoals("initialize");
        
        Shell pre = new Shell("exit 1"); // must fail to simulate scenario!
        p.getPrebuilders().add(pre);
        ResultExposingBuilder resultExposer = new ResultExposingBuilder();
        p.getPostbuilders().add(resultExposer);
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        assertEquals(Result.FAILURE, resultExposer.getResult(), "The result passed to the post build step was not the one from the pre build step");
    }
    

    /**
     * Config roundtrip test around pre/post build step
     */
    @Test
    void testConfigRoundtrip() throws Exception {
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        Shell b1 = new Shell("1");
        Shell b2 = new Shell("2");
        m.getPrebuilders().add(b1);
        m.getPostbuilders().add(b2);
        j.configRoundtrip((Item)m);

        assertEquals(1,  m.getPrebuilders().size());
        assertNotSame(b1,m.getPrebuilders().get(Shell.class));
        assertEquals("1",m.getPrebuilders().get(Shell.class).getCommand());

        assertEquals(1,  m.getPostbuilders().size());
        assertNotSame(b2,m.getPostbuilders().get(Shell.class));
        assertEquals("2",m.getPostbuilders().get(Shell.class).getCommand());

        for (Result r : new Result[]{Result.SUCCESS, Result.UNSTABLE, Result.FAILURE}) {
            m.setRunPostStepsIfResult(r);
            j.configRoundtrip((Item)m);
            assertEquals(r,m.getRunPostStepsIfResult());
        }
    }
    
    
    @Test
    void testDefaultSettingsProvider() throws Exception {
        {
            MavenModuleSet m = j.createProject(MavenModuleSet.class, "p1");
    
            assertNotNull(m);
            assertEquals(DefaultSettingsProvider.class, m.getSettings().getClass());
            assertEquals(DefaultGlobalSettingsProvider.class, m.getGlobalSettings().getClass());
        }
        
        {
            GlobalMavenConfig globalMavenConfig = GlobalMavenConfig.get();
            assertNotNull(globalMavenConfig, "No global Maven Config available");
            globalMavenConfig.setSettingsProvider(new FilePathSettingsProvider("/tmp/settigns.xml"));
            globalMavenConfig.setGlobalSettingsProvider(new FilePathGlobalSettingsProvider("/tmp/global-settigns.xml"));
            
            MavenModuleSet m = j.createProject(MavenModuleSet.class, "p2");
            assertEquals(FilePathSettingsProvider.class, m.getSettings().getClass());
            assertEquals("/tmp/settigns.xml", ((FilePathSettingsProvider)m.getSettings()).getPath());
            assertEquals("/tmp/global-settigns.xml", ((FilePathGlobalSettingsProvider)m.getGlobalSettings()).getPath());
        }
    }
}
