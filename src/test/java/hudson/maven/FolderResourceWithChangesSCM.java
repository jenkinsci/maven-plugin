/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc. Olivier Lamy
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

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.ExtractChangeLogParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * {@link SCM} useful for testing that extracts the given resource as a zip file.
 *
 * @author Kohsuke Kawaguchi
 * @author Olivier Lamy
 */
public class FolderResourceWithChangesSCM extends NullSCM {
    private final String firstDirectory;
    private final String secondDirectory;
    private final String moduleRoot;

    public FolderResourceWithChangesSCM(String firstDirectory, String secondDirectory) {
        if ((firstDirectory == null) || (secondDirectory == null))
            throw new IllegalArgumentException();
        this.firstDirectory = firstDirectory;
        this.secondDirectory = secondDirectory;
        this.moduleRoot = null;
    }

    public FolderResourceWithChangesSCM(String firstDirectory, String secondDirectory, String moduleRoot) {
        if ((firstDirectory == null) || (secondDirectory == null))
            throw new IllegalArgumentException();
        this.firstDirectory = firstDirectory;
        this.secondDirectory = secondDirectory;
        this.moduleRoot = moduleRoot;
    }

    @Override
    public FilePath getModuleRoot(FilePath workspace) {
        if (moduleRoot!=null) {
            return workspace.child(moduleRoot);
        }
        return workspace;
    }

    @Override
    public boolean checkout(AbstractBuild<?,?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
        if (workspace.exists()) {
            listener.getLogger().println("Deleting existing workspace " + workspace.getRemote());
            workspace.deleteRecursive();
        }
        listener.getLogger().println("Staging first directory: " + firstDirectory);
        FileUtils.copyDirectory( new File( this.firstDirectory), //
                                 new File( workspace.getRemote()));
        listener.getLogger().println("Staging second directory: " + secondDirectory);
        FileUtils.copyDirectory( new File( this.secondDirectory), //
                                 new File( workspace.getRemote()));

        // Get list of files changed in secondZip.
        ExtractChangeLogParser.ExtractChangeLogEntry changeLog = new ExtractChangeLogParser.ExtractChangeLogEntry( secondDirectory);
        Stream<Path> paths = Files.list( Paths.get( this.secondDirectory));
        paths.forEach( path -> changeLog.addFile(new ExtractChangeLogParser.FileInZip(path.toString())));
        saveToChangeLog(changeLogFile, changeLog);

        return true;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ExtractChangeLogParser();
    }

    private static String escapeForXml(String string) {
        return Util.xmlEscape(Util.fixNull(string));
    }

    public void saveToChangeLog(File changeLogFile, ExtractChangeLogParser.ExtractChangeLogEntry changeLog) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(changeLogFile);

        PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<extractChanges>");
        stream.println("<entry>");
        stream.println("<zipFile>" + escapeForXml(changeLog.getZipFile()) + "</zipFile>");

        for (String fileName : changeLog.getAffectedPaths()) {
            stream.println("<file>");
            stream.println("<fileName>" + escapeForXml(fileName) + "</fileName>");
            stream.println("</file>");
        }

        stream.println("</entry>");
        stream.println("</extractChanges>");

        stream.close();
    }

    /**
     * Don't write 'this', so that subtypes can be implemented as anonymous class.
     */
    private Object writeReplace() { return new Object(); }

    @Override public SCMDescriptor<?> getDescriptor() {
        return new SCMDescriptor<FolderResourceWithChangesSCM>(FolderResourceWithChangesSCM.class, null) {
            @Override // TODO 1.635+ delete
            public String getDisplayName() {
                return "FolderResourceWithChangesSCM";
            }
        };
    }

}
