/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc.
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
package hudson.maven.reporters;

import hudson.maven.Maven36xBuildTest;
import hudson.maven.MavenJenkinsRule;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import java.io.File;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

public class MavenJavadocArchiverTest
{

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule r = new MavenJenkinsRule();

    @Issue("JENKINS-57244")
    @Test
    public void simple() throws Exception {
        Maven36xBuildTest.configureMaven36();
        MavenModuleSet mms = r.createProject(MavenModuleSet.class, "p");
        mms.setAssignedNode(r.createSlave());
        mms.setScm(new ExtractResourceSCM(getClass().getResource("../maven-multimod.zip")));
        mms.setGoals("install org.apache.maven.plugins:maven-javadoc-plugin:3.2.0:javadoc -DdetectJavaApiLink=false -Dmaven.compiler.target=1.8 -Dmaven.compiler.source=1.8");
        MavenModuleSetBuild b = r.buildAndAssertSuccess(mms);
        MavenModule mm = mms.getModule("org.jvnet.hudson.main.test.multimod$moduleA");
        assertNotNull(mm);
        AbstractMavenJavadocArchiver.MavenJavadocAction mja = mm.getAction(AbstractMavenJavadocArchiver.MavenJavadocAction.class);
        assertNotNull(mja);
        assertEquals(new File(mm.getRootDir(), "javadoc"), mja.dir());
        assertTrue(new File(mja.dir(), "index.html").isFile());
    }

    // TODO test aggregated
    // TODO test Javadoc for test roots

}
