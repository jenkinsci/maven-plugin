/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Olivier Lamy
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

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.remoting.Channel;
import hudson.tasks.Maven.MavenInstallation;
import jenkins.maven3.agent.Maven32Main;
import jenkins.security.MasterToSlaveCallable;

import org.jvnet.hudson.maven3.launcher.Maven32Interceptor;
import org.jvnet.hudson.maven3.listeners.HudsonMavenExecutionResult;

import java.io.IOException;
import java.net.URL;

/**
 * {@link hudson.maven.AbstractMavenProcessFactory} for Maven 3.2.x
 *
 * @author Olivier Lamy
 */
public class Maven32ProcessFactory extends Maven3ProcessFactory
{

    Maven32ProcessFactory( MavenModuleSet mms, AbstractMavenBuild<?, ?> build, Launcher launcher, EnvVars envVars,
                           String mavenOpts, FilePath workDir ) {
        super( mms, build, launcher, envVars, mavenOpts, workDir );
    }

    @Override
    protected String getMainClassName()
    {
        return Maven32Main.class.getName();
    }

    @Override
    protected String getMavenAgentClassPath(MavenInstallation mvn, FilePath slaveRoot, BuildListener listener) throws IOException, InterruptedException {
        String classWorldsJar = getLauncher().getChannel().call(new Maven3ProcessFactory.GetClassWorldsJar(mvn.getHome(),listener));
        String path = classPathEntry(slaveRoot, Maven32Main.class, "maven32-agent", listener) +
            (getLauncher().isUnix()?":":";")+classWorldsJar;

        // TODO this configurable??
        path += (getLauncher().isUnix()?":":";")+mvn.getHomeDir().getPath()+"/conf/logging";

        return path;
    }

    @Override
    protected void applyPlexusModuleContributor(Channel channel, AbstractMavenBuild<?, ?> context) throws InterruptedException, IOException {
        channel.call(new InstallPlexusModulesTask(context));
    }

    private static final class InstallPlexusModulesTask extends MasterToSlaveCallable<Void,IOException>
    {
        PlexusModuleContributor c;

        public InstallPlexusModulesTask(AbstractMavenBuild<?, ?> context) throws IOException, InterruptedException {
            c = PlexusModuleContributorFactory.aggregate(context);
        }

        public Void call() throws IOException {
            Maven32Main.addPlexusComponents( c.getPlexusComponentJars().toArray( new URL[0] ) );
            return null;
        }

        private static final long serialVersionUID = 1L;
    }
    
    @Override
    protected String getMavenInterceptorClassPath(MavenInstallation mvn, FilePath slaveRoot, BuildListener listener) throws IOException, InterruptedException {
        return classPathEntry(slaveRoot, Maven32Interceptor.class, "maven32-interceptor", listener);
    }

    protected String getMavenInterceptorCommonClassPath(MavenInstallation mvn, FilePath slaveRoot, BuildListener listener) throws IOException, InterruptedException {
        return classPathEntry(slaveRoot, HudsonMavenExecutionResult.class, "maven3-interceptor-commons", listener);
    }


}
