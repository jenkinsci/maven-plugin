/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Bruce Chapman
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

import hudson.Launcher;
import hudson.Extension;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.model.BuildListener;
import hudson.tasks.MailSender;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Sends out an e-mail notification for Maven build result.
 * @author Kohsuke Kawaguchi
 */
public class MavenMailer extends MavenReporter {

    private static final Logger LOGGER = Logger.getLogger(MavenMailer.class.getName());

    public String recipients;
    /** not data-bound; set by {@link MavenModule} */
    public String mavenRecipients;
    /** negative sense is historical */
    public boolean dontNotifyEveryUnstableBuild;
    public boolean sendToIndividuals;
    public boolean perModuleEmail;

    @Deprecated public MavenMailer() {}

    @DataBoundConstructor public MavenMailer(String recipients, boolean notifyEveryUnstableBuild, boolean sendToIndividuals, boolean perModuleEmail) {
        this.recipients = recipients;
        this.dontNotifyEveryUnstableBuild = !notifyEveryUnstableBuild;
        this.sendToIndividuals = sendToIndividuals;
        this.perModuleEmail = perModuleEmail;
    }

    public boolean isNotifyEveryUnstableBuild() {
        return !dontNotifyEveryUnstableBuild;
    }

    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if(perModuleEmail) {
            LOGGER.log(Level.FINE, "for {0} potentially mailing to {1} plus {2}", new Object[] {build, recipients, mavenRecipients});
            if (sendToIndividuals) {
                LOGGER.log(Level.FINE, "would also include {0}", build.getCulprits());
            }
            new MailSender(getAllRecipients(),dontNotifyEveryUnstableBuild,sendToIndividuals).execute(build,listener);
        }
        return true;
    }
    
    public String getAllRecipients() {
        StringBuilder sb = new StringBuilder();
        
        if (this.recipients != null) {
            sb.append(this.recipients);
        }
        if (this.mavenRecipients != null) {
            sb.append(" ");
            sb.append(this.mavenRecipients);
        }
        
    	return sb.toString().trim();
    }

    @Extension
    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public String getDisplayName() {
            return Messages.MavenMailer_DisplayName();
        }

    }

    private static final long serialVersionUID = 1L;
}
