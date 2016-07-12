/*
 * Copyright Olivier Lamy
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package hudson.maven;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.NullSCM;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Simple scm which
 */
public class FolderResourceSCM extends NullSCM
{
    private final String directory;

    public FolderResourceSCM( String directory ) {
        this.directory = directory;
    }

    public boolean checkout( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changeLogFile)
        throws IOException, InterruptedException {
        if(workspace.exists()) {
            listener.getLogger().println("Deleting existing workspace " + workspace.getRemote());
            workspace.deleteRecursive();
        }
        listener.getLogger().println("Staging " + this.directory);

        FileUtils.copyDirectory( new File( this.directory), //
                                 new File( workspace.getRemote()));


        return true;
    }
}
