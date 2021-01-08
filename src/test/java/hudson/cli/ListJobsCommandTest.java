/*
 * The MIT License
 *
 * Copyright 2018 Victor Martinez.
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

package hudson.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

public class ListJobsCommandTest {

    @Rule public JenkinsRule j = new JenkinsRule();
    private CLICommandInvoker command;

    @Before
    public void setUp() {
        CLICommand listJobsCommand = new ListJobsCommand();
        command = new CLICommandInvoker(j, listJobsCommand);
    }

    @Issue("JENKINS-18393")
    @Test
    public void getAllJobsFromFolderWithMavenModuleSet() throws Exception {
        MockFolder folder = j.createFolder("Folder");

        folder.createProject(FreeStyleProject.class, "job1");
        folder.createProject(FreeStyleProject.class, "job2");
        folder.createProject(MavenModuleSet.class, "mvn");

        CLICommandInvoker.Result result = command.invokeWithArgs("Folder");
        assertThat(result, CLICommandInvoker.Matcher.succeeded());
        assertThat(result.stdout(), containsString("job1"));
        assertThat(result.stdout(), containsString("job2"));
        assertThat(result.stdout(), containsString("mvn"));
    }
}
