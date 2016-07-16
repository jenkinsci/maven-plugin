package hudson.maven;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.hasItems;

import hudson.model.AbstractProject;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.List;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * Tests that Maven jobs are triggered, when snapshot dependencies of them were build.
 *
 * @author Andrew Bayer
 */
public class MavenSnapshotTriggerTest extends AbstractMavenTestCase {
    /**
     * Verifies dependency build ordering of SNAPSHOT dependency.
     * Note - has to build the projects once each first in order to get dependency info.
     */
    public void testSnapshotDependencyBuildTrigger() throws Exception {

        ToolInstallations.configureDefaultMaven();
        MavenModuleSet projA = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-up");
        projA.setGoals("clean install");
        projA.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));
        MavenModuleSet projB = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-down");
        projB.setGoals("clean install");
        projB.setIgnoreUpstremChanges(false);
        projB.setQuietPeriod(0);
        projB.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));

        buildAndAssertSuccess(projA);
        buildAndAssertSuccess(projB);

        projA.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A-changed.zip")));
        buildAndAssertSuccess(projA);

        // at this point runB2 should be in the queue, so wait until that completes.
        waitUntilNoActivityUpTo(90*1000);
        assertEquals("Expected most recent build of second project to be #2", 2, projB.getLastBuild().getNumber());
    }

    /**
     * Verifies dependency build ordering of multiple SNAPSHOT dependencies.
     * Note - has to build the projects once each first in order to get dependency info.
     * B depends on A, C depends on A and B. Build order should be A->B->C.
     */
    public void testMixedTransitiveSnapshotTrigger() throws Exception {
        ToolInstallations.configureDefaultMaven();

        MavenModuleSet projA = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-up");
        projA.setGoals("clean install");
        projA.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));

        MavenModuleSet projB = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-mid");
        projB.setGoals("clean install");
        projB.setIgnoreUpstremChanges(false);
        projB.setQuietPeriod(0);
        projB.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));

        MavenModuleSet projC = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-down");
        projC.setGoals("clean install");
        projC.setIgnoreUpstremChanges(false);
        projC.setQuietPeriod(0);
        projC.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-C.zip")));

        buildAndAssertSuccess(projA);
        buildAndAssertSuccess(projB);
        buildAndAssertSuccess(projC);

        projA.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A-changed.zip")));

        buildAndAssertSuccess(projA);

        waitUntilNoActivityUpTo(90*1000);  // wait until dependency build trickles down
        assertEquals("Expected most recent build of second project to be #2", 2, projB.getLastBuild().getNumber());
        assertEquals("Expected most recent build of third project to be #2", 2, projC.getLastBuild().getNumber());
    }

    /**
     * Verifies SNAPSHOT dependency resolution of multiple SNAPSHOT upstream dependencies. (JENKINS-21014)
     * Note - has to build the projects once each first in order to get dependency info.
     * Artifact C depends on artifacts A and B.
     * For artifact A there exists two projects. Project A1 has install and A2 has verify in its maven goal. A1 is selected as upstream build for artifact C due to higher ranking of install over verify.
     * For artifact B there exists two projects. Projects B1 has compile and B2 has verify in its maven goal. B2 is preferred as upstream dependency for artifact B as the verify goal takes precedence over the name tiebreaker which would prefer project B1.
     */
    public void testMultipleDependencySnapshotTrigger() throws Exception {
        ToolInstallations.configureDefaultMaven();

        // This is only executed to make sure that dependency A is available in repository
        MavenModuleSet prepareProject = jenkins.createProject(MavenModuleSet.class, "prepareProject");
        prepareProject.setGoals("clean install");
        prepareProject.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));
        buildAndAssertSuccess(prepareProject);
        prepareProject.delete();

        // This is only executed to make sure that dependency B is available in repository
        prepareProject = jenkins.createProject(MavenModuleSet.class, "prepareProject");
        prepareProject.setGoals("clean install");
        prepareProject.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));
        buildAndAssertSuccess(prepareProject);
        prepareProject.delete();

        MavenModuleSet projA1 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-A1");
        projA1.setGoals("clean install");
        projA1.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));

        MavenModuleSet projA2 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-A2");
        projA2.setGoals("clean verify");
        projA2.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));

        MavenModuleSet projB1 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-B1");
        projB1.setGoals("clean compile");
        projB1.setIgnoreUpstremChanges(false);
        projB1.setQuietPeriod(0);
        projB1.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));

        MavenModuleSet projB2 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-B2");
        projB2.setGoals("clean verify");
        projB2.setIgnoreUpstremChanges(false);
        projB2.setQuietPeriod(0);
        projB2.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));

        MavenModuleSet projC = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-C");
        projC.setGoals("clean compile");
        projC.setIgnoreUpstremChanges(false);
        projC.setQuietPeriod(0);
        projC.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-C.zip")));

        // Run all project at least once so that artifacts are known by Jenkins and SNAPSHOT dependencies are determined
        buildAndAssertSuccess(projA1);
        buildAndAssertSuccess(projA2);
        buildAndAssertSuccess(projB1);
        buildAndAssertSuccess(projB2);
        buildAndAssertSuccess(projC);

        final List<String> upstreamProjectNames = new ArrayList<String>();
        for (AbstractProject project : projC.getUpstreamProjects()) {
            upstreamProjectNames.add(project.getName());
        }

        assertEquals("Expected number of upstream project for project 'snap-dep-test-down' to be #2", 2, upstreamProjectNames.size());
        assertThat("Expected 'snap-dep-test-A1' and 'snap-dep-test-B2' as upstream project for project 'snap-dep-test-C'", upstreamProjectNames, hasItems("snap-dep-test-A1", "snap-dep-test-B2"));
    }

    /**Verifies ignoreUnsuccessfulUpstreams configuration
     *
     * Dependency tree is the same as in {@code testMultipleDependencySnapshotTrigger()}.<br>
     * All preparations are identical to {@code testMultipleDependencySnapshotTrigger()} after
     * them all builds of project A1 (the upstream of project C) are deleted.
     * Then verifes two scenarios:
     * <ol>
     *     <li>Schedule build of B2 - another upstream of C. Build of C should not be scheduled</li>
     *     <li>Change configuration of C to ignore unsuccessful upstreams builds. Schedule build of B2.
     *     Build of C should be scheduled</li>
     * </ol>
     *
     */
    public void testMultipleDependencySnapshotTriggerIgnoreUnsuccessfullUpstreams() throws Exception {
        ToolInstallations.configureDefaultMaven();

        // This is only executed to make sure that dependency A is available in repository
        MavenModuleSet prepareProject = jenkins.createProject(MavenModuleSet.class, "prepareProject");
        prepareProject.setGoals("clean install");
        prepareProject.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));
        buildAndAssertSuccess(prepareProject);
        prepareProject.delete();

        // This is only executed to make sure that dependency B is available in repository
        prepareProject = jenkins.createProject(MavenModuleSet.class, "prepareProject");
        prepareProject.setGoals("clean install");
        prepareProject.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));
        buildAndAssertSuccess(prepareProject);
        prepareProject.delete();

        MavenModuleSet projA1 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-A1");
        projA1.setGoals("clean install");
        projA1.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));

        MavenModuleSet projA2 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-A2");
        projA2.setGoals("clean verify");
        projA2.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-A.zip")));

        MavenModuleSet projB1 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-B1");
        projB1.setGoals("clean compile");
        projB1.setIgnoreUpstremChanges(false);
        projB1.setQuietPeriod(0);
        projB1.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));

        MavenModuleSet projB2 = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-B2");
        projB2.setGoals("clean verify");
        projB2.setIgnoreUpstremChanges(false);
        projB2.setQuietPeriod(0);
        projB2.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-B.zip")));

        MavenModuleSet projC = jenkins.createProject(MavenModuleSet.class, "snap-dep-test-C");
        projC.setGoals("clean compile");
        projC.setIgnoreUpstremChanges(false);
        projC.setQuietPeriod(0);
        projC.setScm(new ExtractResourceSCM(getClass().getResource("maven-dep-test-C.zip")));

        // Run all project at least once so that artifacts are known by Jenkins and SNAPSHOT dependencies are determined
        buildAndAssertSuccess(projA1);
        buildAndAssertSuccess(projA2);
        buildAndAssertSuccess(projB1);
        buildAndAssertSuccess(projB2);
        buildAndAssertSuccess(projC);

        final List<String> upstreamProjectNames = new ArrayList<String>();
        for (AbstractProject project : projC.getUpstreamProjects()) {
            upstreamProjectNames.add(project.getName());
        }

        assertEquals("Expected number of upstream project for project 'snap-dep-test-down' to be #2", 2, upstreamProjectNames.size());
        assertThat("Expected 'snap-dep-test-A1' and 'snap-dep-test-B2' as upstream project for project 'snap-dep-test-C'", upstreamProjectNames, hasItems("snap-dep-test-A1", "snap-dep-test-B2"));

        // Mark A1 builds as failed
        for( MavenModuleSetBuild build : projA1._getRuns()) {
            build.delete();
        }

        // building another upstream of project C
        buildAndAssertSuccess(projB2);

        waitUntilNoActivityUpTo(90*1000);  // wait until dependency build trickles down
        // Expect default behaviour: build should not be scheduled because upstream A1 has no sucessfull builds
        assertEquals("Expected legacy behaviour, most recent build of project C to be #1", 1, projC.getLastBuild().getNumber());

        // change C configuration
        projC.setIgnoreUnsuccessfulUpstreams(true);

        // build upstream again
        buildAndAssertSuccess(projB2);

        waitUntilNoActivityUpTo(90*1000);  // wait until dependency build trickles down
        // Expect build scheduling because C ignores upstreams without successful builds
        assertEquals("Expected most recent build of project C to be #2", 2, projC.getLastBuild().getNumber());
    }
}
