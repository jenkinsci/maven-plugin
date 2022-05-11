/*
 * The MIT License
 *
 * Copyright 2013 Red Hat, Inc.
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

package hudson.model;

import static org.junit.Assert.assertEquals;

import hudson.maven.MavenModuleSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RunLoadCounter;

import java.util.concurrent.Callable;

/**
 * @author Lucie Votypkova
 */
public class NodeTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Before
    public void before() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
    }

    /** Verify that the Label#getTiedJobCount does not perform a lazy loading operation. */
    @Issue("JENKINS-26391")
    @Test
    public void testGetAssignedLabelWithJobs() throws Exception {
        final Node node = j.createSlave("label1 label2", null);
        MavenModuleSet mavenProject = j.jenkins.createProject(MavenModuleSet.class, "p");
        mavenProject.setAssignedLabel(j.jenkins.getLabel("label1"));
        RunLoadCounter.prepare(mavenProject);
        j.assertBuildStatus(Result.FAILURE, mavenProject.scheduleBuild2(0).get());
        Integer labelCount = RunLoadCounter.assertMaxLoads(mavenProject, 0, new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                final Label label = j.jenkins.getLabel("label1");
                label.reset(); // Make sure cached value is not used
                return label.getTiedJobCount();
            }
        });

        assertEquals("Should have only one job tied to label.", 1, labelCount.intValue());
    }

    /**
     * Create two projects which have the same label and verify that both are accounted for when
     * getting a count of the jobs tied to the current label.
     */
    @Issue("JENKINS-26391")
    @Test
    public void testGetAssignedLabelMultipleSlaves() throws Exception {
        final Node node1 = j.createSlave("label1", null);
        final Node node2 = j.createSlave("label1", null);

        MavenModuleSet project = j.jenkins.createProject(MavenModuleSet.class, "p1");
        final Label label = j.jenkins.getLabel("label1");
        project.setAssignedLabel(label);
        j.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0).get());

        MavenModuleSet project2 = j.jenkins.createProject(MavenModuleSet.class, "p2");
        project2.setAssignedLabel(label);
        j.assertBuildStatus(Result.FAILURE, project2.scheduleBuild2(0).get());

        label.reset(); // Make sure cached value is not used
        assertEquals("Two jobs should be tied to this label.", 2, label.getTiedJobCount());
    }

    /**
     * Verify that when a label is removed from a job that the tied job count does not include the
     * removed job.
     */
    @Issue("JENKINS-26391")
    @Test
    public void testGetAssignedLabelWhenLabelRemoveFromProject() throws Exception {
        final Node node = j.createSlave("label1", null);

        MavenModuleSet project = j.jenkins.createProject(MavenModuleSet.class, "p");
        final Label label = j.jenkins.getLabel("label1");
        project.setAssignedLabel(label);
        j.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0).get());

        project.setAssignedLabel(null);
        label.reset(); // Make sure cached value is not used
        assertEquals("Label1 should have no tied jobs after the job label was removed.", 0, label.getTiedJobCount());
    }
}
