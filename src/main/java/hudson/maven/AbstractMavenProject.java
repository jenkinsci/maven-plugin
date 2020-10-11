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

import hudson.Util;
import hudson.console.ModelHyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.DependencyGraph;
import jenkins.model.Jenkins;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.DependencyGraph.Dependency;
import hudson.tasks.Maven.ProjectWithMaven;
import hudson.triggers.Trigger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common part between {@link MavenModule} and {@link MavenModuleSet}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractMavenProject<P extends AbstractProject<P,R>,R extends AbstractBuild<P,R>> extends AbstractProject<P,R>
    implements ProjectWithMaven {

	protected static class MavenModuleDependency extends Dependency {

    	public MavenModuleDependency(AbstractMavenProject<?,?> upstream,
    			AbstractProject<?,?> downstream) {
    		super(upstream, downstream);
    	}

    	@Override
    	public boolean shouldTriggerBuild(AbstractBuild build,
    			TaskListener listener, List<Action> actions) {
    		/**
    		 * Schedules all the downstream builds.
    		 * Returns immediately if build result doesn't meet the required level
    		 * (as specified by {@link BuildTrigger}, or {@link Result#SUCCESS} if none).
    		 *
    		 * @param listener
    		 *      Where the progress reports go.
    		 */
    		if (build.getResult().isWorseThan(Result.SUCCESS)) return false;
    		// trigger dependency builds
    		AbstractProject<?,?> downstreamProject = getDownstreamProject();

    		boolean ignoreUnsuccessfulUpstreams = ignoreUnsuccessfulUpstreams(downstreamProject);
		
		MavenModuleSetBuild mavenModuleSetBuild = null;
		if (build instanceof MavenModuleSetBuild) {
			mavenModuleSetBuild = (MavenModuleSetBuild)build;
		}

    		// if the downstream module depends on multiple modules,
    		// only trigger them when all the upstream dependencies are updated.

    		// Check to see if any of its upstream dependencies are already building or in queue.
    		AbstractMavenProject<?,?> parent = (AbstractMavenProject<?,?>) getUpstreamProject();
    		if (areUpstreamsBuilding(downstreamProject, parent, listener)) {
    			return false;
    		}
		// Check to see if is a release so we don't trigger the downstream project
		else if (mavenModuleSetBuild != null && mavenModuleSetBuild.isRelease()) {
			return false;
		}
    		// Check to see if any of its upstream dependencies are in this list of downstream projects.
    		else if (inDownstreamProjects(downstreamProject)) {
                listener.getLogger().println("Not triggering " + ModelHyperlinkNote.encodeTo(downstreamProject) + " because it has dependencies in the downstream project list");
    			return false;
    		}
    		else {
    			AbstractBuild<?,?> dlb = downstreamProject.getLastBuild(); // can be null.
    			for (AbstractMavenProject up : Util.filter(downstreamProject.getUpstreamProjects(),AbstractMavenProject.class)) {
    				Run ulb;
    				if(up==parent) {
    					// the current build itself is not registered as lastSuccessfulBuild
    					// at this point, so we have to take that into account. ugly.
    					if(build.getResult()==null || !build.getResult().isWorseThan(Result.UNSTABLE))
    						ulb = build;
    					else
    						ulb = up.getLastSuccessfulBuild();
    				} else
    					ulb = up.getLastSuccessfulBuild();
    				if(ulb==null) {
    					if( !ignoreUnsuccessfulUpstreams ) {
    						// if no usable build is available from the upstream,
    						// then we have to wait at least until this build is ready
    						listener.getLogger().println("Not triggering " + ModelHyperlinkNote.encodeTo(downstreamProject) + " because another upstream " + ModelHyperlinkNote.encodeTo(up) + " has no successful build");
    						return false;
    					} else {
    						listener.getLogger().println("Another upstream " + ModelHyperlinkNote.encodeTo(up) + " has no successful build but " + ModelHyperlinkNote.encodeTo(downstreamProject)+ " is configured to ignore this.");
    					}
    				}

    				// if no record of the relationship in the last build
    				// is available, we'll just have to assume that the condition
    				// for the new build is met, or else no build will be fired forever.
    				if(dlb==null)   continue;
    				int n = dlb.getUpstreamRelationship(up);
    				if(n==-1)   continue;

    				if( !ignoreUnsuccessfulUpstreams ) {
    					assert ulb.getNumber() >= n;
    				}
    			}
    		}			    
            // No real need to print a message that downstreamProject is being triggered, since BuildTrigger will note this anyway
    		return true;
    	}

		/**
		 * Determines whether any of the upstream project are either
		 * building or in the queue.
		 *
		 * This means eventually there will be an automatic triggering of
		 * the given project (provided that all builds went smoothly.)
		 *
		 * @param downstreamProject
		 *      The AbstractProject we want to build.
		 * @param excludeProject
		 *      An AbstractProject to exclude - if we see this in the transitive
		 *      dependencies, we're not going to bother checking to see if it's
		 *      building. For example, pass the current parent project to be sure
		 *      that it will be ignored when looking for building dependencies.
		 * @return
		 *      True if any upstream projects are building or in queue, false otherwise.
		 */
		@SuppressWarnings("rawtypes")
        private boolean areUpstreamsBuilding(AbstractProject<?,?> downstreamProject,
				AbstractProject<?,?> excludeProject, TaskListener listener) {
			DependencyGraph graph = Jenkins.get().getDependencyGraph();
			Set<AbstractProject> tups = graph.getTransitiveUpstream(downstreamProject);
			for (AbstractProject tup : tups) {
                if (tup != excludeProject && (tup.isBuilding() || tup.isInQueue())) {
                    AbstractProject<?,?> tupr = tup.getRootProject();
                    if (tupr instanceof MavenModuleSet && ((MavenModuleSet) tupr).getBlockTriggerWhenBuilding()) {
                        listener.getLogger().println("Not triggering " + ModelHyperlinkNote.encodeTo(downstreamProject) + " because it has a dependency " + ModelHyperlinkNote.encodeTo(tup) + " already building or in queue");
                        return true;
                    } else {
                        listener.getLogger().println("Could be blocking trigger of " + ModelHyperlinkNote.encodeTo(downstreamProject) + " (due to a dependency on " + ModelHyperlinkNote.encodeTo(tup) + ") but the upstream is not configured to block while building.");
                        return false; // do not bother printing messages about other upstreams
                    }
                }
			}
			return false;
		}

    	/**
    	 * Determines wheter upstream without successful builds should prevent downstream build
    	 * scheduling
    	 *
    	 * @param downstreamProject
    	 * @return false -  downstream build should not be scheduled when some of its upstreams has
    	 * 					no successfull builds. Default return.<br>
    	 * 		   true  - 	downstream build may be scheduled even if some or many of its upstreams
    	 * 		   			has no successful builds
    	 */
    	private boolean ignoreUnsuccessfulUpstreams(AbstractProject<?,?> downstreamProject) {
    		MavenModuleSet mavenModuleSet = null;
    		if( downstreamProject instanceof MavenModuleSet ) {
    			mavenModuleSet = (MavenModuleSet)downstreamProject;
    		}
    		if( downstreamProject instanceof MavenModule ) {
    			mavenModuleSet = ((MavenModule)downstreamProject).getParent();
    		}
    		if( mavenModuleSet != null ) {
    			return mavenModuleSet.ignoreUnsuccessfulUpstreams();
    		}
    		return false;
    	}

		private boolean inDownstreamProjects(AbstractProject<?,?> downstreamProject) {
			DependencyGraph graph = Jenkins.get().getDependencyGraph();
			Set<AbstractProject> tups = graph.getTransitiveUpstream(downstreamProject);
		
			for (AbstractProject tup : tups) {
				List<AbstractProject<?,?>> downstreamProjects = getUpstreamProject().getDownstreamProjects();
				for (AbstractProject<?,?> dp : downstreamProjects) {
					if(dp!=getUpstreamProject() && dp!=downstreamProject && dp==tup) 
						return true;
				}
			}
			return false;
		}
    }

    protected AbstractMavenProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    protected List<Action> createTransientActions() {
        List<Action> r = super.createTransientActions();

        // if we just pick up the project actions from the last build,
        // and if the last build failed very early, then the reports that
        // kick in later (like test results) won't be displayed.
        // so pick up last successful build, too.
        Set<Class> added = new HashSet<>();
        addTransientActionsFromBuild(getLastBuild(),r,added);
        addTransientActionsFromBuild(getLastSuccessfulBuild(),r,added);

        for (Trigger<?> trigger : triggers())
            r.addAll(trigger.getProjectActions());

        return r;
    }

    /**
     * @param collection
     *      Add the transient actions to this collection.
     */
    protected abstract void addTransientActionsFromBuild(R lastBuild, List<Action> collection, Set<Class> added);
    
    // TODO for some reason the default implementations added to ParameterizedJob in 2.61 are not found by javac:

    @Deprecated
    @Override
    public boolean scheduleBuild() {
        return super.scheduleBuild();
    }

    @Deprecated
    @Override
    public boolean scheduleBuild(int quietPeriod) {
        return super.scheduleBuild(quietPeriod);
    }

    @Override
    public boolean scheduleBuild(Cause c) {
        return super.scheduleBuild(c);
    }

    @Override
    public boolean scheduleBuild(int quietPeriod, Cause c) {
        return super.scheduleBuild(quietPeriod, c);
    }

}
