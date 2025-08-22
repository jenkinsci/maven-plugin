/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt, CloudBees, Inc.
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

import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlPage;

import hudson.maven.MavenModuleSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LogRecorder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JobPropertyTest {

    @SuppressWarnings("unused")
    private final LogRecorder logRecorder = new LogRecorder();

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Issue("JENKINS-2398")
    void jobPropertySummaryIsShownInMavenModuleSetIndexPage() throws Exception {
        assertJobPropertySummaryIsShownInIndexPage(MavenModuleSet.class);
    }

    private void assertJobPropertySummaryIsShownInIndexPage(Class<? extends TopLevelItem> type)
            throws Exception {
        JobPropertyImpl jp = new JobPropertyImpl("NeedleInPage");
        Job<?, ?> project = (Job<?, ?>) j.jenkins.createProject(type, "job-test-case");
        project.addProperty(jp);
        project.save();

        HtmlPage page = j.createWebClient().goTo("job/job-test-case");
        WebAssert.assertTextPresent(page, "NeedleInPage");
    }

    public static class JobPropertyImpl extends JobProperty<Job<?, ?>> {
        private final String propertyString;

        public JobPropertyImpl(String propertyString) {
            this.propertyString = propertyString;
        }

        public String getPropertyString() {
            return propertyString;
        }

        @TestExtension
        public static class DescriptorImpl extends JobPropertyDescriptor {
            @Override
            public boolean isApplicable(Class<? extends Job> jobType) {
                return false;
            }
        }
    }
}
