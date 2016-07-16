/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Yahoo! Inc.
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

import hudson.maven.reporters.SurefireReport;
import hudson.model.Result;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResult;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

public class CaseResultTest {

    @Rule public JenkinsRule j = new MavenJenkinsRule();

    /**
     * Verifies that the error message and stacktrace from a failed junit test actually render properly.
     */
    @Bug(4257)
    @Test public void mavenErrorMsgAndStacktraceRender() throws Exception {
        ToolInstallations.configureMaven3();
        MavenModuleSet m = j.jenkins.createProject(MavenModuleSet.class, "maven-render-test");
        m.setScm(new ExtractResourceSCM(m.getClass().getResource("maven-test-failure-findbugs.zip")));
        m.setGoals("clean test");
        MavenModuleSetBuild b = j.assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());
        MavenBuild modBuild = b.getModuleLastBuilds().get(m.getModule("test:test"));
        TestResult tr = modBuild.getAction(SurefireReport.class).getResult();
        assertEquals(1,tr.getFailedTests().size());
        CaseResult cr = tr.getFailedTests().get(0);
        assertEquals("test.AppTest",cr.getClassName());
        assertEquals("testApp",cr.getName());
        assertNotNull("Error details should not be null", cr.getErrorDetails());
        assertNotNull("Error stacktrace should not be null", cr.getErrorStackTrace());
    }

}
