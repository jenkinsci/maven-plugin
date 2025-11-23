package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.Serial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.ExtractChangeLogSet;
import org.jvnet.hudson.test.ExtractResourceWithChangesSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrew Bayer
 */
@WithJenkins
class MavenMultiModuleTestIncremental {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Issue("JENKINS-7684")
    @Test
    void testRelRootPom() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet m = j.createProject(MavenModuleSet.class, "p");
        m.setRootPOM("parent/pom.xml");
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceWithChangesSCM(getClass().getResource("maven-multimod-rel-base.zip"),
						   getClass().getResource("maven-multimod-changes.zip")));
        
    	j.buildAndAssertSuccess(m);
            
    	// Now run a second build with the changes.
    	m.setIncrementalBuild(true);
        j.buildAndAssertSuccess(m);
            
    	MavenModuleSetBuild pBuild = m.getLastBuild();
    	ExtractChangeLogSet changeSet = (ExtractChangeLogSet) pBuild.getChangeSet();
            
    	assertFalse(changeSet.isEmptySet(), "ExtractChangeLogSet should not be empty.");
    
    	for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
    	    String parentModuleName = modBuild.getParent().getModuleName().toString();
            switch (parentModuleName) {
                case "org.jvnet.hudson.main.test.multimod:moduleA" ->
                        assertEquals(Result.NOT_BUILT, modBuild.getResult(), "moduleA should have Result.NOT_BUILT");
                case "org.jvnet.hudson.main.test.multimod:moduleB" ->
                        assertEquals(Result.SUCCESS, modBuild.getResult(), "moduleB should have Result.SUCCESS");
                case "org.jvnet.hudson.main.test.multimod:moduleC" ->
                        assertEquals(Result.SUCCESS, modBuild.getResult(), "moduleC should have Result.SUCCESS");
            }
    	}	
    	
        long summedModuleDuration = 0;
        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            summedModuleDuration += modBuild.getDuration();
        }
        assertTrue(pBuild.getDuration() >= summedModuleDuration,
                   "duration of moduleset build should be greater-equal than sum of the module builds");
    }

    private static class TestReporter extends MavenReporter {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) {
            assertNotNull(build.getWorkspace());
            return true;
        }
    }
}
