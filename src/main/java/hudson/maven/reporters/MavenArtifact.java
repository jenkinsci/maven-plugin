/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, CloudBees, Inc.
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

import com.google.common.collect.Maps;
import hudson.Util;
import hudson.maven.MavenBuild;
import hudson.maven.MavenBuildProxy;
import hudson.model.Api;
import hudson.model.BuildListener;
import jenkins.model.StandardArtifactManager;
import jenkins.util.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Captures information about an artifact created by Maven and archived by
 * Jenkins, so that we can later deploy it to repositories of our choice.
 *
 * <p>
 * This object is created within the Maven process and sent back to the master,
 * so it shouldn't contain anything non-serializable as fields.
 *
 * <p>
 * Once it's constructed, the object should be considered final and immutable.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.189
 */
@ExportedBean
public final class MavenArtifact implements Serializable {

    /**
     * Basic parameters of a Maven artifact.
     */
    @Exported
    public final String groupId, artifactId, version, classifier, type;

    /**
     * File name (without directory portion) of this artifact in the Hudson archive.
     * Remembered explicitly because some times this doesn't follow the
     * standard naming convention, due to {@code <finalName>} setting in POM.
     *
     * <p>
     * This name is taken directly from the name of the file as used during the build
     * (thus POM would be most likely just <code>pom.xml</code> and artifacts would
     * use their <code>finalName</code> if one is configured.) This is often
     * different from {@link #canonicalName}.
     */
    @Exported
    public final String fileName;

    /**
     * The canonical artifact file name, used by Maven in the repository.
     * This is <code>artifactId-version[-classifier].extension</code>.
     *
     * <p>
     * The reason we persist this is that the extension is only available
     * through {@link ArtifactHandler}. 
     */
    @Exported
    public final String canonicalName;

    /**
     * The md5sum for this artifact.
     */
    @Exported
    public final String md5sum;

    public MavenArtifact(Artifact a) throws IOException {
        this.groupId = a.getGroupId();
        this.artifactId = a.getArtifactId();
        this.version = a.getVersion();
        this.classifier = a.getClassifier();
        this.type = a.getType();
        this.fileName = a.getFile().getName();
        this.md5sum = Util.getDigestOf(a.getFile());
        String extension;
        if(a.getArtifactHandler()!=null) // don't know if this can be null, but just to be defensive.
            extension = a.getArtifactHandler().getExtension();
        else
            extension = a.getType();

        canonicalName = getSeed(extension);
    }

    public MavenArtifact(String groupId, String artifactId, String version, String classifier, String type, String fileName, String md5sum) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
        this.fileName = fileName;
        this.canonicalName = getSeed(type);
        this.md5sum = md5sum;
    }

    /**
     * Convenience method to check if the given {@link Artifact} object contains
     * enough information suitable for recording, and if so, create {@link MavenArtifact}.
     */
    public static MavenArtifact create(Artifact a) throws IOException {
        File file = a.getFile();
        if(file==null)
            return null; // perhaps build failed and didn't leave an artifact
        if(!file.isFile())
            return null; // file doesn't exist or artifact points to a directory
        return new MavenArtifact(a);
    }

    public boolean isPOM() {
        return fileName.endsWith(".pom")||"pom.xml".equals(fileName);   // hack
    }

    /**
     * @deprecated only works with {@link StandardArtifactManager} and subclasses; use {@link #toCloseableArtifact} instead
     */
    @Deprecated
    public Artifact toArtifact(ArtifactHandlerManager handlerManager, ArtifactFactory factory, MavenBuild build) throws IOException {
        return toCloseableArtifact(handlerManager, factory, build).get();
    }

    /**
     * {@link Artifact} holder that can be released in a {@code finally}-block.
     * @since 2.0.3
     */
    public static final class CloseableArtifact implements Closeable {
        private final Artifact artifact;
        private final Closeable closeable;
        CloseableArtifact(Artifact artifact, Closeable closeable) {
            this.artifact = artifact;
            this.closeable = closeable;
        }
        public Artifact get() {
            return artifact;
        }
        @Override public void close() throws IOException {
            closeable.close();
        }
    }

    /**
     * Creates a Maven {@link Artifact} back from the persisted data.
     */
    public CloseableArtifact toCloseableArtifact(ArtifactHandlerManager handlerManager, ArtifactFactory factory, MavenBuild build) throws IOException {
        // Hack: presence of custom ArtifactHandler during builds could influence the file extension
        // in the repository during deployment. So simulate that behavior if that's necessary.
        final String canonicalExtension = canonicalName.substring(canonicalName.lastIndexOf('.')+1);
        ArtifactHandler ah = handlerManager.getArtifactHandler(type);
        Map<String,ArtifactHandler> handlers = Maps.newHashMap();
        
        handlers.put( type, new DefaultArtifactHandler(type) {
                        public String getExtension() {
                            return canonicalExtension;
                        } } );
        // Fix for HUDSON-3814 - changed from comparing against canonical extension to canonicalName.endsWith.
        if(!canonicalName.endsWith(ah.getExtension())) {
            handlerManager.addHandlers(handlers);
        }

        Artifact a = factory.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);
        TemporaryFile file = getTemporaryFile(build);
        a.setFile(file.getFile());
        return new CloseableArtifact(a, file);
    }

    /**
     * Computes the file name seed by taking &lt;finalName> POM entry into consideration.
     */
    private String getSeed(String extension) {
        String name = artifactId+'-'+version;
        if(Util.fixEmpty(classifier)!=null)
            name += '-'+classifier;
        name += '.'+extension;
        return name;
    }

    /**
     * Obtains the {@link File} representing the archived artifact.
     * @throws FileNotFoundException if the archived artifact was missing
     * @deprecated only works with {@link StandardArtifactManager} and subclasses; use {@link #getTemporaryFile} instead
     */
    @Deprecated
    public File getFile(MavenBuild build) throws IOException {
        @SuppressWarnings("deprecation") File artifactsDir = build.getArtifactsDir();
        File f = new File(new File(new File(new File(artifactsDir, groupId), artifactId), version), canonicalName);
        if (!f.exists()) {
            throw new FileNotFoundException("Archived artifact is missing: " + f);
        }
        return f;
    }

    /**
     * Gets an archived artifact.
     * @param build a Maven build that might have archived this
     * @return a representation of the artifact
     * @since 2.0.3
     */
    public @Nonnull TemporaryFile getTemporaryFile(MavenBuild build) {
        return new TemporaryFile(build);
    }

    /**
     * Representation of an archived artifact that might be accessed in various ways.
     * Release in a {@code finally}-block.
     * @since 2.0.3
     */
    public final class TemporaryFile implements Closeable {

        private final MavenBuild build;
        private File copy;

        TemporaryFile(MavenBuild build) {
            this.build = build;
        }

        /**
         * Gets the artifact from whatever storage mechanism is appropriate.
         * This is the preferred method for code that does not need to deal with {@link File} specifically.
         * @return the purported location of the artifact (might no longer exist if it has since been deleted)
         */
        public @Nonnull VirtualFile getVirtualFile() {
            return build.getArtifactManager().root().child(artifactPath());
        }

        /**
         * Gets the artifact as a local file, perhaps creating a temporary copy as needed.
         * You must {@link #close} it when finished; do not delete the result file yourself.
         * @return either the original artifact, or a copy thereof; may not exist if it has since been deleted
         */
        public @Nonnull synchronized File getFile() throws IOException {
            if (copy == null) {
                try {
                    return MavenArtifact.this.getFile(build);
                } catch (FileNotFoundException x) {
                    File f = File.createTempFile("jenkins-", canonicalName);
                    f.deleteOnExit();

                    try(OutputStream os = new FileOutputStream(f)) {
                        IOUtils.copy(getVirtualFile().open(), os);
                    }
                    copy = f;
                }
            }
            return copy;
        }

        public synchronized void close() throws IOException {
            if (copy != null) {
                try {
                    if (!copy.delete()) {
                        throw new IOException("could not delete " + copy);
                    }
                } finally {
                    copy = null;
                }
            }
        }

    }

    /**
     * Serve the file.
     *
     * TODO: figure out how to make this URL more discoverable to the remote API.
     */
    public HttpResponse doFile(final @AncestorInPath MavenArtifactRecord parent) throws IOException {
        return ( req, rsp, node ) ->
                IOUtils.copy(parent.parent.getArtifactManager().root().child(artifactPath()).open(), rsp.getCompressedOutputStream(req));
    }

    private String artifactPath() {
        return groupId + '/' + artifactId + '/' + version + '/' + canonicalName;
    }

    /**
     * Called from within Maven to archive an artifact in Hudson.
     */
    public void archive(MavenBuildProxy build, File file, BuildListener listener) throws IOException, InterruptedException {
        if (build.isArchivingDisabled()) {
            LOGGER.fine("Archiving disabled - not archiving " + file);
        }
        else {
            build.queueArchiving(artifactPath(), file.getAbsolutePath());
        }
    }

    public Api getApi() {
        return new Api(this);
    }

    private static final Logger LOGGER = Logger.getLogger(MavenArtifact.class.getName());

    private static final long serialVersionUID = 1L;
}
