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

import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

// Note: copied from package org.apache.maven.cli with just one minor adaption for Maven3Mojo
public class ExecutionEventLogger
	extends org.apache.maven.cli.event.ExecutionEventLogger
{
    private final Logger logger;
    private final String mojoNote;

    private static final int LINE_LENGTH = 72;
    private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;
    private static final int MAX_PROJECT_NAME_LENGTH = 52;

    public ExecutionEventLogger()
    {
    	super();
        logger = LoggerFactory.getLogger( ExecutionEventLogger.class );
        mojoNote = null; // Maven 3.1+, so unused; cf. JenkinsEventSpy
    }

    @Deprecated
    public ExecutionEventLogger( Logger logger )
    {
        this(logger, null);
    }

    public ExecutionEventLogger( Logger logger, String mojoNote )
    {
        super(logger);
        this.logger = logger;
        this.mojoNote = mojoNote;
    }

    /**
     * <pre>--- mojo-artifactId:version:goal (mojo-executionId) @ project-artifactId ---</pre>
     */
    @Override
    public void mojoStarted( ExecutionEvent event )
    {
        if ( mojoNote != null && logger.isInfoEnabled() )
        {
            final StringBuilder buffer = new StringBuilder( 128 );
            buffer.append(mojoNote);
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

    @Override
    public void sessionEnded( ExecutionEvent event )
    {
        //super.sessionEnded( event );
        if ( logger.isInfoEnabled() )
        {
            if ( event.getSession().getProjects().size() > 1 )
            {
                logReactorSummary( event.getSession() );
            }

            logResult( event.getSession() );

            logStats( event.getSession() );

            infoLine( '-' );
        }
    }

    private void logReactorSummary( MavenSession session )
    {
        boolean isSingleVersion = isSingleVersionedReactor( session );

        infoLine( '-' );

        StringBuilder summary = new StringBuilder( "Reactor Summary" );
        if ( isSingleVersion )
        {
            summary.append( " for " );
            summary.append( session.getTopLevelProject().getName() );
            summary.append( " " );
            summary.append( session.getTopLevelProject().getVersion() );
        }
        summary.append( ":" );
        infoMain( summary.toString() );

        logger.info( "" );

        MavenExecutionResult result = session.getResult();

        List<MavenProject> projects = session.getProjects();

        for ( MavenProject project : projects )
        {
            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( project.getName() );
            buffer.append( ' ' );

            if ( !isSingleVersion )
            {
                buffer.append( project.getVersion() );
                buffer.append( ' ' );
            }

            if ( buffer.length() <= MAX_PROJECT_NAME_LENGTH )
            {
                while ( buffer.length() < MAX_PROJECT_NAME_LENGTH )
                {
                    buffer.append( '.' );
                }
                buffer.append( ' ' );
            }

            BuildSummary buildSummary = result.getBuildSummary( project );

            if ( buildSummary == null )
            {
                buffer.append( buffer().warning( "SKIPPED" ) );
            }
            else if ( buildSummary instanceof BuildSuccess )
            {
                buffer.append( buffer().success( "SUCCESS" ) );
                buffer.append( " [" );
                String buildTimeDuration = formatDuration( buildSummary.getTime() );
                int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
                if ( padSize > 0 )
                {
                    buffer.append( chars( ' ', padSize ) );
                }
                buffer.append( buildTimeDuration );
                buffer.append( ']' );
            }
            else if ( buildSummary instanceof BuildFailure )
            {
                buffer.append( buffer().failure( "FAILURE" ) );
                buffer.append( " [" );
                String buildTimeDuration = formatDuration( buildSummary.getTime() );
                int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
                if ( padSize > 0 )
                {
                    buffer.append( chars( ' ', padSize ) );
                }
                buffer.append( buildTimeDuration );
                buffer.append( ']' );
            }

            logger.info( buffer.toString() );
        }
    }

    private void logResult( MavenSession session )
    {
        infoLine( '-' );
        MessageBuilder buffer = buffer();

        if ( session.getResult().hasExceptions() )
        {
            buffer.failure( "BUILD FAILURE" );
        }
        else
        {
            buffer.success( "BUILD SUCCESS" );
        }
        logger.info( buffer.toString() );
    }

    private void logStats( MavenSession session )
    {
        infoLine( '-' );

        long finish = System.currentTimeMillis();

        long time = finish - session.getRequest().getStartTime().getTime();

        // JENKINS-54530  the method has been recently added to Maven so we want to avoid java.lang.NoSuchMethodError
        // when using older Maven version
        String wallClock = "";// session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";

        logger.info( "Total time:  " + formatDuration( time ) + wallClock );

        logger.info( "Finished at: " + formatTimestamp( finish ) );
    }

    private void infoLine( char c )
    {
        infoMain( chars( c, LINE_LENGTH ) );
    }

    private void infoMain( String msg )
    {
        logger.info( buffer().strong( msg ).toString() );
    }

    private static String chars(char c, int count) {
        StringBuilder buffer = new StringBuilder(count);

        for(int i = count; i > 0; --i) {
            buffer.append(c);
        }

        return buffer.toString();
    }

    private boolean isSingleVersionedReactor( MavenSession session )
    {
        boolean result = true;

        MavenProject topProject = session.getTopLevelProject();
        List<MavenProject> sortedProjects = session.getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            if ( !topProject.getVersion().equals( mavenProject.getVersion() ) )
            {
                result = false;
                break;
            }
        }

        return result;
    }

    private static String formatDuration(long duration) {
        long ms = duration % 1000L;
        long s = duration / 1000L % 60L;
        long m = duration / 60000L % 60L;
        long h = duration / 3600000L % 24L;
        long d = duration / 86400000L;
        String format;
        if (d > 0L) {
            format = "%d d %02d:%02d h";
        } else if (h > 0L) {
            format = "%2$02d:%3$02d h";
        } else if (m > 0L) {
            format = "%3$02d:%4$02d min";
        } else {
            format = "%4$d.%5$03d s";
        }

        return String.format(format, d, h, m, s, ms);
    }

    private static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssXXX");
        return sdf.format(new Date( timestamp));
    }
}
