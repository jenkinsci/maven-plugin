package hudson.maven;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.InternalErrorException;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.jvnet.hudson.maven3.listeners.HudsonMavenExecutionResult;
import org.jvnet.hudson.maven3.listeners.MavenProjectInfo;

/* package */ class Maven3FailureLogger {

    private org.slf4j.Logger slf4jLogger;
	boolean showErrors = false; // default from DefaultMavenExecutionRequest.showErrors
	boolean failNever = false; // default from DefaultMavenExecutionRequest.reactorFailureBehavior

	public Maven3FailureLogger( org.slf4j.Logger slf4jLogger )
    {
        this.slf4jLogger = slf4jLogger;
    }

	// partial copy paste of MavenCli.execute()
    public void logFailures(HudsonMavenExecutionResult result) {
    	if ( !result.getThrowables().isEmpty() )
        {
        	ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<String, String>();

            MavenProject project = null;

            for ( Throwable exception : result.getThrowables() )
            {
                ExceptionSummary summary = handler.handleException( exception );

                logSummary( summary, references, "", showErrors );

                if ( project == null && exception instanceof LifecycleExecutionException )
                {
                    project = ( (LifecycleExecutionException) exception ).getProject();
                }
            }

            slf4jLogger.error( "" );

            if ( !showErrors )
            {
                slf4jLogger.error( "To see the full stack trace of the errors, re-run Maven with the -e switch." );
            }
            if ( !slf4jLogger.isDebugEnabled() ) // TODO fix me, this is not the same log of Maven3 process
            {
                slf4jLogger.error( "Re-run Maven using the -X switch to enable full debug logging." );
            }

            if ( !references.isEmpty() )
            {
                slf4jLogger.error( "" );
                slf4jLogger.error( "For more information about the errors and possible solutions"
                              + ", please read the following articles:" );

                for ( Map.Entry<String, String> entry : references.entrySet() )
                {
                    slf4jLogger.error( entry.getValue() + " " + entry.getKey() );
                }
            }

            if ( project != null && !project.equals( convert( result.getMavenProjectInfos().get( 0 )) ) )
            {
                slf4jLogger.error( "" );
                slf4jLogger.error( "After correcting the problems, you can resume the build with the command" );
                slf4jLogger.error( "  mvn <goals> -rf :" + project.getArtifactId() );
            }

            if ( failNever )
            {
                slf4jLogger.info( "Build failures were ignored." );
            }
        }
    }

    // copy paste of MavenCli.logSummary()
	private void logSummary(ExceptionSummary summary, Map<String, String> references, String indent, boolean showErrors)
	{
		String referenceKey = "";

		if ( StringUtils.isNotEmpty( summary.getReference() ) )
		{
			referenceKey = references.get( summary.getReference() );
			if (referenceKey == null) {
				referenceKey = "[Help " + ( references.size() + 1 ) + "]";
				references.put( summary.getReference(), referenceKey );
			}
		}

		String msg = summary.getMessage();

		if (StringUtils.isNotEmpty( referenceKey ))
		{
			if (msg.indexOf('\n') < 0)
			{
				msg += " -> " + referenceKey;
			}
			else
			{
				msg += "\n-> " + referenceKey;
			}
		}

		String[] lines = msg.split("(\r\n)|(\r)|(\n)");

		for ( int i = 0; i < lines.length; i++ )
		{
			String line = indent + lines[i].trim();

			if ( i == lines.length - 1 && ( showErrors || ( summary.getException() instanceof InternalErrorException ) ) ) {
				slf4jLogger.error( line, summary.getException() );
			} else {
				slf4jLogger.error(line);
			}
		}

		indent += "  ";

		for ( ExceptionSummary child : summary.getChildren() ) {
			logSummary( child, references, indent, showErrors );
		}
	}

	private MavenProject convert(MavenProjectInfo prjInfo)
	{
		MavenProject prj = new MavenProject();
		prj.setArtifactId(prjInfo.getArtifactId());
		prj.setGroupId(prjInfo.getGroupId());
		prj.setVersion(prjInfo.getVersion());

		return prj;
	}

    /*
     * Following values comes from MavenExecutionRequest that is not
     * reachable. Request can easily get into an
     * ExecutionListener.sessionEnded call from MavenSession argument,
     * in this scenario all implementations must be patched to handle
     * errors.
     */
	public void setOptions(List<String> goals)
	{
	    // need to calculate some values as in the maven request.
		showErrors = goals.contains("-" + CLIManager.DEBUG) || goals.contains("-" + CLIManager.ERRORS);
		failNever = goals.contains("-" + CLIManager.FAIL_NEVER);
	}
}