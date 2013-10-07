package hudson.maven.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import hudson.tasks._maven.Maven3MojoNote;

import java.io.IOException;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Note: copied from package org.apache.maven.cli with just one minor adaption for Maven3Mojo
public class ExecutionEventLogger
	extends org.apache.maven.cli.event.ExecutionEventLogger
{
    private final Logger logger;

    public ExecutionEventLogger()
    {
    	super();
        logger = LoggerFactory.getLogger( ExecutionEventLogger.class );
    }

    public ExecutionEventLogger( Logger logger )
    {
        super(logger);
        this.logger = logger;
    }

    /**
     * <pre>--- mojo-artifactId:version:goal (mojo-executionId) @ project-artifactId ---</pre>
     */
    @Override
    public void mojoStarted( ExecutionEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            final Maven3MojoNote note = new Maven3MojoNote();
            final StringBuilder buffer = new StringBuilder( 128 );
            try {
                buffer.append( note.encode() );
            } catch ( IOException e ) {
                // As we use only memory buffers this should not happen, ever.
                throw new RuntimeException( "Could not encode note?", e );
            }
            buffer.append( "--- " );
            append( buffer, event.getMojoExecution() );
            append( buffer, event.getProject() );
            buffer.append( " ---" );

            logger.info( "" );
            logger.info( buffer.toString() );
        }
    }

    private void append( StringBuilder buffer, MojoExecution me )
    {
        buffer.append( me.getArtifactId() ).append( ':' ).append( me.getVersion() );
        buffer.append( ':' ).append( me.getGoal() );
        if ( me.getExecutionId() != null )
        {
            buffer.append( " (" ).append( me.getExecutionId() ).append( ')' );
        }
    }

    private void append( StringBuilder buffer, MavenProject project )
    {
        buffer.append( " @ " ).append( project.getArtifactId() );
    }

}
