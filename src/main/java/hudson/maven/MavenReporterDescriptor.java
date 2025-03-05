/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.maven.reporters.MavenArtifactArchiver;
import hudson.model.Descriptor;
import hudson.model.Describable;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyException;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.jelly.JellyClassTearOff;

import java.util.Collection;

/**
 * {@link Descriptor} for {@link MavenReporter}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class MavenReporterDescriptor extends Descriptor<MavenReporter> {
    protected MavenReporterDescriptor(Class<? extends MavenReporter> clazz) {
        super(clazz);
    }

    /**
     * Infers the type of the corresponding {@link Describable} from the outer class.
     * This version works when you follow the common convention, where a descriptor
     * is written as the static nested class of the describable class.
     *
     * @since 1.278
     */
    protected MavenReporterDescriptor() {
    }

    /**
     * Returns an instance used for automatic {@link MavenReporter} activation.
     *
     * <p>
     * Some {@link MavenReporter}s, such as {@link MavenArtifactArchiver},
     * can work just with the configuration in POM and don't need any additional
     * Hudson configuration. They also don't need any explicit enabling/disabling
     * as they can activate themselves by listening to the callback from the build
     * (for example javadoc archiver can do the work in response to the execution
     * of the javadoc target.)
     *
     * <p>
     * Those {@link MavenReporter}s should return a valid instance
     * from this method. Such instance will then participate into the build
     * and receive event callbacks.
     */
    public MavenReporter newAutoInstance(MavenModule module) {
        return null;
    }

    /**
     * If {@link #hasConfigScreen() the reporter has no configuration screen},
     * this method can safely return null, which is the default implementation.
     */
    @Deprecated
    public MavenReporter newInstance(StaplerRequest2 req) throws FormException {
        return null;
    }

    /**
     * Returns true if this descriptor has <code>config.jelly</code>.
     */
    public final boolean hasConfigScreen() {
        MetaClass c = WebApp.getCurrent().getMetaClass(getClass());
        try {
            JellyClassTearOff tearOff = c.loadTearOff(JellyClassTearOff.class);
            return tearOff.findScript(getConfigPage())!=null;
        } catch(JellyException e) {
            return false;
        }
    }

    /**
     * Lists all the currently registered instances of {@link MavenReporterDescriptor}.
     */
    public static Collection<MavenReporterDescriptor> all() {
        // use getDescriptorList and not getExtensionList to pick up legacy instances
        return Jenkins.get().getDescriptorList(MavenReporter.class);
    }

    /**
     * File patterns this reporter might create when running inside the Maven JVM.
     * Applies to usages from {@link MavenReporter#postBuild} and similar callbacks
     * of {@link MavenBuildProxy#getRootDir}, {@link MavenBuildProxy#getProjectRootDir}, and {@link MavenBuildProxy#getModuleSetRootDir}.
     * (But not {@link MavenBuildProxy#getArtifactsDir}, which should not be used anyway.)
     * @return an Ant-style file pattern of files we expect to copy, or null if not applicable (default)
     */
    @CheckForNull
    public String reportedFilePattern() {
        return null;
    }

}
