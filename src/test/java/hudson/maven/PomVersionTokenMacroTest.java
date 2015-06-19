/*
 * The MIT License
 *
 * Copyright (c) 2014, Diabol AB, Patrik Boström
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

import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import static org.junit.Assert.assertEquals;

public class PomVersionTokenMacroTest {

    @Rule
    public final JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testPomVersionExpansion() throws Exception {
        StreamTaskListener listener = StreamTaskListener.fromStdout();
        jenkins.configureDefaultMaven();
        MavenModuleSet project = jenkins.createMavenProject();
        project.setScm(new SingleFileSCM("pom.xml", getClass().getResource("big-artifact.pom")));
        MavenModuleSetBuild build = jenkins.buildAndAssertSuccess(project);
        assertEquals("0.1-SNAPSHOT", TokenMacro.expand(build, listener, "${POM_VERSION}"));
        assertEquals("0.1", TokenMacro.expand(build, listener, "${POM_VERSION, stripSnapshot=true}"));

    }

}
