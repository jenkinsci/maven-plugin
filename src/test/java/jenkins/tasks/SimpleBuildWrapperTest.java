/*
 * The MIT License
 *
 * Copyright 2015 Jesse Glick.
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

package jenkins.tasks;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.BuildWatcherExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.Serial;

@WithJenkins
class SimpleBuildWrapperTest {

    @SuppressWarnings("unused")    
    @RegisterExtension
    private static final BuildWatcherExtension BUILD_WATCHER = new BuildWatcherExtension();

    private JenkinsRule r;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void disposerWithMaven() throws Exception {
        MavenInstallation maven = ToolInstallations.configureDefaultMaven();
        r.jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(maven);
        MavenModuleSet p = r.createProject(MavenModuleSet.class, "p");
        p.getBuildWrappersList().add(new PreCheckoutWrapperWithDisposer());
        p.setIsFingerprintingDisabled(true);
        p.setIsArchivingDisabled(true);
        p.setScm(new ExtractResourceSCM(getClass().getResource("/simple-projects.zip")));
        MavenModuleSetBuild b = p.scheduleBuild2(0).get();
        r.assertLogContains("ran DisposerImpl #1", b);
        r.assertLogNotContains("ran DisposerImpl #2", b);
    }

    public static class WrapperWithDisposer extends SimpleBuildWrapper {
        @Override public void setUp(Context context, Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) {
            context.setDisposer(new DisposerImpl());
        }
        private static final class DisposerImpl extends Disposer {
            @Serial
            private static final long serialVersionUID = 1;
            private int tearDownCount = 0;
            @Override public void tearDown(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
                listener.getLogger().println("ran DisposerImpl #" + (++tearDownCount));
            }
        }
        @TestExtension("disposerWithMaven") public static class DescriptorImpl extends BuildWrapperDescriptor {
            @Override public boolean isApplicable(AbstractProject<?,?> item) {
                return true;
            }
        }
    }
    public static class PreCheckoutWrapperWithDisposer extends WrapperWithDisposer {
        @Override
        protected boolean runPreCheckout() {
            return true;
        }
        @TestExtension("disposerWithMaven") public static class DescriptorImpl extends BuildWrapperDescriptor {
            @Override public boolean isApplicable(AbstractProject<?,?> item) {
                return true;
            }
        }
    }
}
