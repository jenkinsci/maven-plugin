/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Jorg Heymans, Peter Hayes, Red Hat, Inc., Stephen Connolly, id:cactusman
 * Olivier Lamy
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

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Indenter;
import hudson.Plugin;
import hudson.Util;
import hudson.maven.local_repo.DefaultLocalRepositoryLocator;
import hudson.maven.local_repo.LocalRepositoryLocator;
import hudson.maven.local_repo.PerJobLocalRepositoryLocator;
import hudson.maven.reporters.MavenMailer;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Executor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Queue.Task;
import hudson.model.ResourceActivity;
import hudson.model.Result;
import hudson.model.SCMedItem;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.search.CollectionSearchIndex;
import hudson.search.SearchIndexBuilder;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrappers;
import hudson.tasks.Builder;
import hudson.tasks.Fingerprinter;
import hudson.tasks.JavadocArchiver;
import hudson.tasks.Mailer;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.Publisher;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.AlternativeUiTextProvider;
import hudson.util.CopyOnWriteMap;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.Function1;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.mvn.FilePathSettingsProvider;
import jenkins.mvn.GlobalMavenConfig;
import jenkins.mvn.GlobalSettingsProvider;
import jenkins.mvn.GlobalSettingsProviderDescriptor;
import jenkins.mvn.SettingsProvider;
import jenkins.mvn.SettingsProviderDescriptor;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import static hudson.model.ItemGroupMixIn.loadChildren;

/**
 * Group of {@link MavenModule}s.
 *
 * <p>
 * This corresponds to the group of Maven POMs that constitute a single
 * tree of projects. This group serves as the grouping of those related
 * modules.
 *
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings("rawtypes")
public class MavenModuleSet extends AbstractMavenProject<MavenModuleSet,MavenModuleSetBuild> implements TopLevelItem, ItemGroup<MavenModule>, SCMedItem, Saveable, BuildableItemWithBuildWrappers {
	
    /**
     * All {@link MavenModule}s, keyed by their {@link MavenModule#getModuleName()} module name}s.
     */
    transient /*final*/ Map<ModuleName,MavenModule> modules = new CopyOnWriteMap.Tree<ModuleName,MavenModule>();

    /**
     * Topologically sorted list of modules. This only includes live modules,
     * since archived ones usually don't have consistent history.
     */
    @CopyOnWrite
    transient List<MavenModule> sortedActiveModules;

    /**
     * Name of the top-level module. Null until the root module is determined.
     */
    private ModuleName rootModule;

    private String rootPOM;

    private String goals;

    /**
     * @deprecated as of 1.481
     *      Subsumed by {@link #settings}, maps to {@link FilePathSettingsProvider}
     */
    private transient String alternateSettings;

    /**
     * Default goals specified in POM. Can be null.
     */
    private String defaultGoals;

    /**
     * Identifies {@link MavenInstallation} to be used.
     * Null to indicate 'default' maven.
     */
    private String mavenName;

    /**
     * Equivalent of CLI <tt>MAVEN_OPTS</tt>. Can be null.
     */
    private String mavenOpts;

    /**
     * If true, the build will be aggregator style, meaning
     * all the modules are executed in a single Maven invocation, as in CLI.
     * False otherwise, meaning each module is built separately and possibly in parallel.
     *
     * @since 1.133
     */
    private boolean aggregatorStyleBuild = true;

    /**
     * If true, and if aggregatorStyleBuild is false and we are using Maven 2.1 or later, the build will
     * check the changeset before building, and if there are changes, only those modules which have changes
     * or those modules which failed or were unstable in the previous build will be built directly, using
     * Maven's make-like reactor mode. Any modules depending on the directly built modules will also be built,
     * but that's controlled by Maven.
     *
     * @since 1.318
     */
    private boolean incrementalBuild = false;

    /**
     * If true, the build will use its own local Maven repository
     * via "-Dmaven.repo.local=...".
     * <p>
     * This would consume additional disk space, but provides isolation with other builds on the same machine,
     * such as mixing SNAPSHOTS. Maven also doesn't try to coordinate the concurrent access to Maven repositories
     * from multiple Maven process, so this helps there too.
     *
     * @since 1.223
     * @deprecated as of 1.448
     *      Subsumed by {@link #localRepository}. false maps to {@link DefaultLocalRepositoryLocator},
     *      and true maps to {@link PerJobLocalRepositoryLocator}
     */
    private transient Boolean usePrivateRepository;

    /**
     * Encapsulates where to run the local repository.
     *
     * If null, inherited from the global configuration.
     * 
     * @since 1.448
     */
    private LocalRepositoryLocator localRepository = null;
    
    /**
     * If true, do not automatically schedule a build when one of the project dependencies is built.
     * <p>
     * See HUDSON-1714.
     */
    private boolean ignoreUpstremChanges = false;

    /**
     * If true build will be scheduled when one of the project dependencies is built even if
     * some other upstreams has no successful builds
     */
    private boolean ignoreUnsuccessfulUpstreams = false;

    /**
     * If true, do not archive artifacts to the master.
     */
    private boolean archivingDisabled = false;
    
    /**
     * If true, do not archive artifacts to the master during site deploy.
     */
    private boolean siteArchivingDisabled = false;

    /**
     * If true, do not fingerprint consumed and produced artifacts.
     */
    private boolean fingerprintingDisabled = false;
    
    /**
     * parameter for pom parsing by default <code>false</code> to be faster
     * @since 1.394
     */
    private boolean resolveDependencies = false;
    
    /**
     * parameter for pom parsing by default <code>false</code> to be faster
     * @since 1.394
     */    
    private boolean processPlugins = false;
    
    /**
     * parameter for validation level during pom parsing by default the one corresponding
     * to the maven version used (2 or 3)
     *
     * @since 1.394
     * @see DescriptorImpl#mavenValidationLevels
     */    
    private int mavenValidationLevel = -1;

    /**
     * Inform jenkins this build don't use UI code and can run without access to graphical environment. Could be used
     * later to select a headless-slave from a pool, but first introduced for JENKINS-9785
     */
    private boolean runHeadless = false;

    /**
     * @since 1.426
     * @deprecated since 1.484 settings are provided by {@link #settings}
     */
    private String settingConfigId;

    /**
     * @since 1.426
     * @deprecated since 1.484 settings are provided by {@link #globalSettings}
     */
    private String globalSettingConfigId;
    
    /**
     * Whether to participate in triggering downstream projects.
     * @since 1.494
     */
    private boolean disableTriggerDownstreamProjects;

    private Boolean blockTriggerWhenBuilding;

    /**
     * used temporary during maven build to store file path
     * @since 1.426
     * @deprecated since 1.484 settings are provided by {@link #globalSettings}
     */
    protected transient String globalSettingConfigPath;

    /**
     * @since 1.491
     */
    private SettingsProvider settings;
    
    /**
     * @since 1.491
     */
    private GlobalSettingsProvider globalSettings;


    /**
     * @since 1.491
     */
    public Object readResolve() {
        // backward compatibility, maven-plugin used to have a dependency to the config-file-provider plugin
        Plugin plugin = null;
        if(StringUtils.isNotBlank(this.settingConfigId) || StringUtils.isNotBlank(this.globalSettingConfigId)) {
            plugin = Jenkins.getInstance().getPlugin("config-file-provider");
            if(plugin == null || !plugin.getWrapper().isEnabled()){
                LOGGER.severe(Messages.MavenModuleSet_readResolve_missingConfigProvider());
            }  
        }
        
        if (this.alternateSettings != null) { 
            this.settings = new FilePathSettingsProvider(alternateSettings);
            this.alternateSettings = null;
        } else if (plugin != null && StringUtils.isNotBlank(this.settingConfigId)) {
            try {
                Class<? extends SettingsProvider> legacySettings = plugin.getWrapper().classLoader.loadClass("org.jenkinsci.plugins.configfiles.maven.job.MvnSettingsProvider").asSubclass(SettingsProvider.class);
                SettingsProvider newInstance = legacySettings.newInstance();
                PropertyUtils.setProperty(newInstance, "settingsConfigId", this.settingConfigId);
                this.settings = newInstance;
                this.settingConfigId = null;
            } catch (Exception e) {
                // The PluginUpdateMonitor is also informing the admin about the update (via hudson.maven.PluginImpl.init())
                LOGGER.severe(Messages.MavenModuleSet_readResolve_updateConfigProvider(settingConfigId));
                e.printStackTrace();
            }
        }
        
        if (plugin != null && StringUtils.isNotBlank(this.globalSettingConfigId)) {
            try {
                Class<? extends GlobalSettingsProvider> legacySettings = plugin.getWrapper().classLoader.loadClass("org.jenkinsci.plugins.configfiles.maven.job.MvnGlobalSettingsProvider").asSubclass(GlobalSettingsProvider.class);
                GlobalSettingsProvider newInstance = legacySettings.newInstance();
                PropertyUtils.setProperty(newInstance, "settingsConfigId", this.globalSettingConfigId);
                this.globalSettings = newInstance;
                this.globalSettingConfigId = null;
            } catch (Exception e) {
                // The PluginUpdateMonitor is also informing the admin about the update (via hudson.maven.PluginImpl.init())
                LOGGER.severe(Messages.MavenModuleSet_readResolve_updateConfigProvider(globalSettingConfigId));
                e.printStackTrace();
            }
        }
        return this;
    }
    
    /**
     * Reporters configured at {@link MavenModuleSet} level. Applies to all {@link MavenModule} builds.
     */
    private DescribableList<MavenReporter,Descriptor<MavenReporter>> reporters =
        new DescribableList<MavenReporter,Descriptor<MavenReporter>>(this);

    /**
     * List of active {@link Publisher}s configured for this project.
     * @since 1.176
     */
    private DescribableList<Publisher,Descriptor<Publisher>> publishers =
        new DescribableList<Publisher,Descriptor<Publisher>>(this);

    /**
     * List of active {@link BuildWrapper}s configured for this project.
     * @since 1.212
     */
    private DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappers =
        new DescribableList<BuildWrapper, Descriptor<BuildWrapper>>(this);

	/**
     * List of active {@link Builder}s configured for this project.
     */
    private DescribableList<Builder,Descriptor<Builder>> prebuilders =
            new DescribableList<Builder,Descriptor<Builder>>(this);
    
    private DescribableList<Builder,Descriptor<Builder>> postbuilders =
            new DescribableList<Builder,Descriptor<Builder>>(this);
	
    private Result runPostStepsIfResult;
    
   
    /**
     * @deprecated
     *      Use {@link #MavenModuleSet(ItemGroup, String)}
     */
    public MavenModuleSet(String name) {
        this(Jenkins.getInstance(), name);
    }

    public MavenModuleSet(ItemGroup parent, String name) {
        super(parent,name);
    }

    /**
     * Builders that are run before the main Maven execution.
     *
     * @since 1.433
     */
	public DescribableList<Builder,Descriptor<Builder>> getPrebuilders() {
        return prebuilders;
    }
	
    /**
     * Builders that are run after the main Maven execution.
     *
     * @since 1.433
     */
	public DescribableList<Builder,Descriptor<Builder>> getPostbuilders() {
        return postbuilders;
    }
	
	void addPostBuilder(Builder builder) throws IOException{
	    postbuilders.add(builder);
	}
	
	/**
     * {@link #postbuilders} are run if the result is better or equal to this threshold.
     *
     * @return
     *      never null
     * @since 1.433
	 */
	public Result getRunPostStepsIfResult() {
		return Functions.defaulted(runPostStepsIfResult,Result.FAILURE);
	}

    public void setRunPostStepsIfResult(Result v) {
        this.runPostStepsIfResult = Functions.defaulted(v,Result.FAILURE);
    }

    public String getUrlChildPrefix() {
        // seemingly redundant "./" is used to make sure that ':' is not interpreted as the scheme identifier
        return ".";
    }

    public Collection<MavenModule> getItems() {
        return modules.values();
    }

    @Exported
    public Collection<MavenModule> getModules() {
        return getItems();
    }

    public MavenModule getItem(String name) {
        try {
            return modules.get(ModuleName.fromString(name));
        } catch (IllegalArgumentException x) {
            return null; // not a Maven module name, ignore
        }
    }

    public MavenModule getModule(String name) {
        return getItem(name);
    }

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, Messages.MavenModuleSet_Pronoun());
    }


    @Override   // to make this accessible from MavenModuleSetBuild
    protected void updateTransientActions() {
        super.updateTransientActions();
    }

    protected List<Action> createTransientActions() {
        List<Action> r = super.createTransientActions();

        // Fix for ISSUE-1149
        for (MavenModule module: modules.values()) {
            module.updateTransientActions();
        }
        
        if(publishers!=null)    // this method can be loaded from within the onLoad method, where this might be null
            for (BuildStep step : publishers)
                r.addAll(step.getProjectActions(this));

        if (buildWrappers!=null)
	        for (BuildWrapper step : buildWrappers)
                r.addAll(step.getProjectActions(this));
        
        if (prebuilders!=null)
            for (Builder builder : prebuilders)
                r.addAll(builder.getProjectActions(this));

        if (postbuilders!=null)
            for (Builder builder : postbuilders)
                r.addAll(builder.getProjectActions(this));

        return r;
    }

    protected void addTransientActionsFromBuild(MavenModuleSetBuild build, List<Action> collection, Set<Class> added) {
        if(build==null)    return;

        for (Action a : build.getActions())
            if(a instanceof MavenAggregatedReport)
                if(added.add(a.getClass()))
                    collection.add(((MavenAggregatedReport)a).getProjectAction(this));

        List<MavenReporter> list = build.projectActionReporters;
        if(list==null)   return;

        for (MavenReporter step : list) {
            if(!added.add(step.getClass()))     continue;   // already added
            Action a = step.getAggregatedProjectAction(this);
            if(a!=null)
                collection.add(a);
        }
    }

    /**
     * Called by {@link MavenModule#doDoDelete(StaplerRequest, StaplerResponse)}.
     * Real deletion is done by the caller, and this method only adjusts the
     * data structure the parent maintains.
     */
    /*package*/ void onModuleDeleted(MavenModule module) {
        modules.remove(module.getModuleName());
    }

    /**
     * Returns true if there's any disabled module.
     */
    public boolean hasDisabledModule() {
        for (MavenModule m : modules.values()) {
            if(m.isDisabled())
                return true;
        }
        return false;
    }

    /**
     * Possibly empty list of all disabled modules (if disabled==true)
     * or all enabeld modules (if disabled==false)
     */
    public List<MavenModule> getDisabledModules(boolean disabled) {
        if(!disabled && sortedActiveModules!=null)
            return sortedActiveModules;

        List<MavenModule> r = new ArrayList<MavenModule>();
        for (MavenModule m : modules.values()) {
            if(m.isDisabled()==disabled)
                r.add(m);
        }
        return r;
    }

    public Indenter<MavenModule> createIndenter() {
        return new Indenter<MavenModule>() {
            protected int getNestLevel(MavenModule job) {
                return job.nestLevel;
            }
        };
    }

    public boolean isIncrementalBuild() {
        return incrementalBuild;
    }

    public boolean isAggregatorStyleBuild() {
        return aggregatorStyleBuild;
    }

    /**
     * @deprecated as of 1.448
     *      Use {@link #getLocalRepository()}
     */
    public boolean usesPrivateRepository() {
        return !(getLocalRepository() instanceof DefaultLocalRepositoryLocator);
    }

    /** @deprecated see {@link MavenMailer#perModuleEmail} */
    @Deprecated
    public boolean isPerModuleEmail() {
        MavenMailer m = reporters.get(MavenMailer.class);
        return m != null ? m.perModuleEmail : true;
    }
    
    public boolean ignoreUpstremChanges() {
        return ignoreUpstremChanges;
    }

    public boolean ignoreUnsuccessfulUpstreams() {
        return ignoreUnsuccessfulUpstreams;
    }

    public boolean runHeadless() {
        return runHeadless;
    }

    public boolean isArchivingDisabled() {
        return archivingDisabled;
    }

    public boolean isSiteArchivingDisabled() {
        return siteArchivingDisabled;
    }

    public boolean isFingerprintingDisabled() {
        return fingerprintingDisabled;
    }

    public void setIncrementalBuild(boolean incrementalBuild) {
        this.incrementalBuild = incrementalBuild;
    }

    public void setAggregatorStyleBuild(boolean aggregatorStyleBuild) {
        this.aggregatorStyleBuild = aggregatorStyleBuild;
    }

    /**
     * @deprecated as of 1.448.
     *      Use {@link #setLocalRepository(LocalRepositoryLocator)} instead
     */
    public void setUsePrivateRepository(boolean usePrivateRepository) {
        setLocalRepository(usePrivateRepository?new PerJobLocalRepositoryLocator() : new DefaultLocalRepositoryLocator());
    }

    /**
     * @return 
     *      never null
     */
    public LocalRepositoryLocator getLocalRepository() {
        return localRepository!=null ? localRepository : getDescriptor().getLocalRepository();
    }

    /**
     * Undefaulted locally configured value with taking inheritance from the global configuration into account.
     */
    public LocalRepositoryLocator getExplicitLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(LocalRepositoryLocator localRepository) {
        this.localRepository = localRepository;
    }
    
    /**
     * @since 1.491
     */
    public void setSettings(SettingsProvider settings) {
        this.settings = settings;
    }

    /**
     * @since 1.491
     */
    public void setGlobalSettings(GlobalSettingsProvider globalSettings) {
        this.globalSettings = globalSettings;
    }

    public void setIgnoreUpstremChanges(boolean ignoreUpstremChanges) {
        this.ignoreUpstremChanges = ignoreUpstremChanges;
    }

    public void setIgnoreUnsuccessfulUpstreams(boolean ignoreUnsuccessfulUpstreams) {
        this.ignoreUnsuccessfulUpstreams = ignoreUnsuccessfulUpstreams;
    }

    public void setRunHeadless(boolean runHeadless) {
        this.runHeadless = runHeadless;
    }

    public void setIsArchivingDisabled(boolean archivingDisabled) {
        this.archivingDisabled = archivingDisabled;
    }
    
    public void setIsSiteArchivingDisabled(boolean siteArchivingDisabled) {
        this.siteArchivingDisabled = siteArchivingDisabled;
    }

    public void setIsFingerprintingDisabled(boolean fingerprintingDisabled) {
        this.fingerprintingDisabled = fingerprintingDisabled;
    }

    public boolean isResolveDependencies()
    {
        return resolveDependencies;
    }

    public void setResolveDependencies( boolean resolveDependencies ) {
        this.resolveDependencies = resolveDependencies;
    }

    public boolean isProcessPlugins() {
        return processPlugins;
    }

    public void setProcessPlugins( boolean processPlugins ) {
        this.processPlugins = processPlugins;
    }    

    public int getMavenValidationLevel() {
        return mavenValidationLevel;
    }

    /**
     * @since 1.481
     */
    public SettingsProvider getSettings() {
        return settings != null ? settings : GlobalMavenConfig.get().getSettingsProvider();
    }

    /**
     * @since 1.481
     */
    public GlobalSettingsProvider getGlobalSettings() {
        return globalSettings != null ? globalSettings : GlobalMavenConfig.get().getGlobalSettingsProvider();
    }

    /**
     * List of active {@link MavenReporter}s that should be applied to all module builds.
     */
    public DescribableList<MavenReporter, Descriptor<MavenReporter>> getReporters() {
        return reporters;
    }

    /**
     * List of active {@link Publisher}s. Can be empty but never null.
     */
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishers() {
        return publishers;
    }

    @Override
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        return publishers;
    }

    public DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrappersList() {
        return buildWrappers;
    }

    /**
     * List of active {@link BuildWrapper}s. Can be empty but never null.
     *
     * @deprecated as of 1.335
     *      Use {@link #getBuildWrappersList()} to be consistent with other subtypes of {@link AbstractProject}.
     */
    public DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrappers() {
        return buildWrappers;
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        if(ModuleName.isValid(token))
            return getModule(token);
        return super.getDynamic(token,req,rsp);
    }

    public File getRootDirFor(MavenModule child) {
        return new File(getModulesDir(),child.getModuleName().toFileSystemName());
    }

    public void onRenamed(MavenModule item, String oldName, String newName) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void onDeleted(MavenModule item) throws IOException {
        // noop
    }

    public Collection<Job> getAllJobs() {
        Set<Job> jobs = new HashSet<Job>(getItems());
        jobs.add(this);
        return jobs;
    }

    @Override
    protected Class<MavenModuleSetBuild> getBuildClass() {
        return MavenModuleSetBuild.class;
    }

    @Override
    protected SearchIndexBuilder makeSearchIndex() {
        return super.makeSearchIndex()
            .add(new CollectionSearchIndex<MavenModule>() {// for computers
                protected MavenModule get(String key) {
                    if (modules.values() == null) return null;
                    for (MavenModule m : modules.values()) {
                        if(StringUtils.equals( m.getDisplayName(),key))
                            return m;
                    }
                    return null;
                }
                protected Collection<MavenModule> all() {
                    return modules.values();
                }
                protected String getName(MavenModule o) {
                    return o.getName();
                }
            });
    }

    @Override
    public boolean isFingerprintConfigured() {
        return !isFingerprintingDisabled() || getPublishersList().get(Fingerprinter.class) != null;
    }

    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        modules = Collections.emptyMap(); // needed during load
        super.onLoad(parent, name);

        modules = loadChildren(this, getModulesDir(),new Function1<ModuleName,MavenModule>() {
            public ModuleName call(MavenModule module) {
                return module.getModuleName();
            }
        });
        // update the transient nest level field.
        MavenModule root = getRootModule();
        if(root!=null && root.getChildren()!=null) {
            List<MavenModule> sortedList = new ArrayList<MavenModule>();
            Stack<MavenModule> q = new Stack<MavenModule>();
            root.nestLevel = 0;
            q.push(root);
            while(!q.isEmpty()) {
                MavenModule p = q.pop();
                sortedList.add(p);
                List<MavenModule> children = p.getChildren();
                if(children!=null) {
                    for (MavenModule m : children)
                        m.nestLevel = p.nestLevel+1;
                    for( int i=children.size()-1; i>=0; i--) {   // add them in the reverse order
                        MavenModule item = children.get(i);
                        // If the maven project exists already then we should not reload it.
                        if (!sortedList.contains(item)) {
                            q.push(item);
                        }
                        else {
                            LOGGER.log(Level.SEVERE, "Maven project " + name +
                                    " already contains a module called " + item.getName());
                        }
                    }
                }
            }
            this.sortedActiveModules = sortedList;
        } else {
            this.sortedActiveModules = getDisabledModules(false);
        }

        if(reporters==null){
            reporters = new DescribableList<MavenReporter, Descriptor<MavenReporter>>(this);
        }
        reporters.setOwner(this);
        if(publishers==null){
            publishers = new DescribableList<Publisher,Descriptor<Publisher>>(this);
        }
        publishers.setOwner(this);
        if(buildWrappers==null){
            buildWrappers = new DescribableList<BuildWrapper, Descriptor<BuildWrapper>>(this);
        }
        buildWrappers.setOwner(this);
        if(prebuilders==null){
        	prebuilders = new DescribableList<Builder,Descriptor<Builder>>(this);
        }
        prebuilders.setOwner(this);
        if(postbuilders==null){
        	postbuilders = new DescribableList<Builder,Descriptor<Builder>>(this);
        }
        postbuilders.setOwner(this);
        
        if (Boolean.TRUE.equals(usePrivateRepository)) {
            this.localRepository = new PerJobLocalRepositoryLocator();
            usePrivateRepository = null;
        }
        
        updateTransientActions();
    }

    private File getModulesDir() {
        return new File(getRootDir(),"modules");
    }

    /**
     * To make it easy to grasp relationship among modules
     * and the module set, we'll align the build numbers of
     * all the modules.
     *
     * <p>
     * This method is invoked from {@link Executor#run()},
     * and because of the mutual exclusion among {@link MavenModuleSetBuild}
     * and {@link MavenBuild}, we can safely touch all the modules.
     */
    public synchronized int assignBuildNumber() throws IOException {
        // determine the next value
        updateNextBuildNumber();

        return super.assignBuildNumber();
    }

    public void logRotate() throws IOException, InterruptedException {
        super.logRotate();
        // perform the log rotation of modules
        for (MavenModule m : modules.values())
            m.logRotate();
    }

    /**
     * The next build of {@link MavenModuleSet} must have
     * the build number newer than any of the current module build.
     */
    /*package*/ void updateNextBuildNumber() throws IOException {
        int next = this.nextBuildNumber;
        for (MavenModule m : modules.values())
            next = Math.max(next,m.getNextBuildNumber());

        if(this.nextBuildNumber!=next) {
            this.nextBuildNumber=next;
            this.saveNextBuildNumber();
        }
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        // the modules are already rebuild by DependencyGraph#init !
//      Collection<MavenModule> modules = getModules();
//      for (MavenModule m : modules) {
//          m.buildDependencyGraph(graph);
//      }
        
        super.buildDependencyGraph(graph);
        publishers.buildDependencyGraph(this,graph);
        buildWrappers.buildDependencyGraph(this,graph);
        prebuilders.buildDependencyGraph(this,graph);
        postbuilders.buildDependencyGraph(this,graph);
    }
    
    public boolean isDisableTriggerDownstreamProjects() {
      return disableTriggerDownstreamProjects;
    }

    public void setDisableTriggerDownstreamProjects(boolean disableTriggerDownstreamProjects) {
      this.disableTriggerDownstreamProjects = disableTriggerDownstreamProjects;
    }

    public boolean getBlockTriggerWhenBuilding() {
        return blockTriggerWhenBuilding == null || blockTriggerWhenBuilding;
    }

    public void setBlockTriggerWhenBuilding(boolean blockTriggerWhenBuilding) {
        this.blockTriggerWhenBuilding = blockTriggerWhenBuilding;
    }

    public MavenModule getRootModule() {
        if(rootModule==null)    return null;
        return modules.get(rootModule);
    }

    public MavenInstallation inferMavenInstallation() {
        return getMaven();
    }

    @Override
    protected Set<ResourceActivity> getResourceActivities() {
        final Set<ResourceActivity> activities = new HashSet<ResourceActivity>();

        activities.addAll(super.getResourceActivities());
        activities.addAll(Util.filter(publishers, ResourceActivity.class));
        activities.addAll(Util.filter(buildWrappers, ResourceActivity.class));
        activities.addAll(Util.filter(prebuilders, ResourceActivity.class));
        activities.addAll(Util.filter(postbuilders, ResourceActivity.class));

        return activities;
    }

    /**
     * @deprecated for backward comp only
     */
    public String getRootPOM(){
        return getRootPOM( null );
    }

    /**
     * Gets the location of top-level <tt>pom.xml</tt> relative to the workspace root.
     * @since 1.467
     */
    public String getRootPOM(EnvVars env) {
        if (rootPOM == null) return "pom.xml";
        // JENKINS-13822
        if (env == null) return rootPOM;
        return env.expand(rootPOM);
    }

    public void setRootPOM(String rootPOM) {
        this.rootPOM = rootPOM;
    }

    public AbstractProject<?,?> asProject() {
        return this;
    }

    /**
     * Gets the list of goals to execute.
     */
    public String getGoals() {
        if(goals==null) {
            if(defaultGoals!=null)  return defaultGoals;
            return "install";
        }
        return goals;
    }

    public void setGoals(String goals) {
        this.goals = goals;
    }

    private boolean checkMavenOption(String shortForm, String longForm) {
        for (String t : Util.tokenize(getGoals())) {
            if(t.equals(shortForm) || t.equals(longForm))
                return true;
	}
	return false;
    }
	
    private List<String> getMavenArgument(String shortForm, String longForm) {
        List<String> args = new ArrayList<String>();
        boolean switchFound=false;
        for (String t : Util.tokenize(getGoals())) {
            if(switchFound) {
                args.add(t);
                switchFound = false;
            }
            else
            if(t.equals(shortForm) || t.equals(longForm))
                switchFound=true;
            else
            if(t.startsWith(shortForm)) {
                args.add(t.substring(shortForm.length()));
            }
            else
            if(t.startsWith(longForm)) {
                args.add(t.substring(longForm.length()));
            }
        }
        return args;
    }

    /**
     * Gets the workspace-relative path to an alternative Maven settings.xml file.
     * @deprecated as of 1.481
     */
    public String getAlternateSettings() {
        return alternateSettings;
    }

    /**
     * Sets the workspace-relative path to an alternative Maven settings.xml file.
     * @deprecated as of 1.481
     */
    public void setAlternateSettings(String alternateSettings) throws IOException {
        this.alternateSettings = alternateSettings;
    }

    /**
     * If the list of configured goals contain the "-P" option,
     * return the configured profiles. Otherwise null.
     */
    public String getProfiles() {
        return Util.join(getMavenArgument("-P","--activate-profiles"),",");
    }

    /**
     * Gets the system properties explicitly set in the Maven command line (the "-D" option.)
     */
    public Properties getMavenProperties() {
        Properties props = new Properties();
        for (String arg : getMavenArgument("-D","--define")) {
            int idx = arg.indexOf('=');
            if(idx<0)   props.put(arg,"true");
            else        props.put(arg.substring(0,idx),arg.substring(idx+1));
        }
        return props;
    }

    /**
     * Check for "-N" or "--non-recursive" in the Maven goals/options.
     */
    public boolean isNonRecursive() {
        return checkMavenOption("-N", "--non-recursive");
    }

    /**
     * Possibly null, whitespace-separated (including TAB, NL, etc) VM options
     * to be used to launch Maven process.
     *
     * If mavenOpts is null or empty, we'll return the globally-defined MAVEN_OPTS.
     *
     * <p>
     * This method returns a configured value as-is, which can include variabl references.
     * At runtime, use {@link AbstractMavenBuild#getMavenOpts(TaskListener, EnvVars)} to obtain
     * a fully resolved value.
     */
    public String getMavenOpts() {
        if ((mavenOpts!=null) && (mavenOpts.trim().length()>0)) { 
            return getFormattedMavenOpts(mavenOpts);
        }
        else {
            String globalOpts = getDescriptor().getGlobalMavenOpts();
            if (globalOpts!=null) {
                return getFormattedMavenOpts(globalOpts);
            }
            else {
                return globalOpts;
            }
        }
    }
    
    private String getFormattedMavenOpts(String mavenOpts) {
        return mavenOpts == null? null: mavenOpts.replaceAll("[\t\r\n]+"," ");
    }

    /**
     * Set mavenOpts. If the new mavenOpts are equals to the global mavenOpts,
     * job mavenOpts are set to null.
     */
    public void setMavenOpts(String mavenOpts) {
        String globalMavenOpts = getFormattedMavenOpts(getDescriptor().getGlobalMavenOpts());
        
        if (mavenOpts != null && !getFormattedMavenOpts(mavenOpts).equals(globalMavenOpts)) {
            this.mavenOpts = mavenOpts;
        } else {
            this.mavenOpts = null;
        }
    }

    /**
     * Gets the Maven to invoke.
     * If null, we pick any random Maven installation.
     */
    public MavenInstallation getMaven() {
        MavenInstallation[] installations = getDescriptor().getMavenDescriptor().getInstallations();
        for( MavenInstallation i : installations) {
            if(mavenName==null || i.getName().equals(mavenName))
                return i;
        }
        if (installations.length==1)
            return installations[0];
        return null;
    }

    public void setMaven(String mavenName) {
        this.mavenName = mavenName;
    }

    /**
     * Returns the {@link MavenModule}s that are in the queue.
     */
    public List<Queue.Item> getQueueItems() {
        return filter(Arrays.asList(Jenkins.getInstance().getQueue().getItems()));
    }

    /**
     * Returns the {@link MavenModule}s that are in the queue.
     */
    public List<Queue.Item> getApproximateQueueItemsQuickly() {
        return filter(Jenkins.getInstance().getQueue().getApproximateItemsQuickly());
    }

    private List<Queue.Item> filter(Collection<Queue.Item> base) {
        List<Queue.Item> r = new ArrayList<Queue.Item>();
        for( Queue.Item item : base) {
            Task t = item.task;
            if((t instanceof MavenModule && ((MavenModule)t).getParent()==this) || t ==this)
                r.add(item);
        }
        return r;
    }

    /**
     * Gets the list of goals specified by the user,
     * without taking inheritance and POM default goals
     * into account.
     *
     * <p>
     * This is only used to present the UI screen, and in
     * all the other cases {@link #getGoals()} should be used.
     */
    public String getUserConfiguredGoals() {
        return goals;
    }
    
    @Override
    protected List<MavenModuleSetBuild> getEstimatedDurationCandidates() {
        return super.getEstimatedDurationCandidates();
    }

    /*package*/ void reconfigure(PomInfo rootPom) throws IOException {
        if(this.rootModule!=null && this.rootModule.equals(rootPom.name))
            return; // no change
        this.rootModule = rootPom.name;
        this.defaultGoals = rootPom.defaultGoal;
        save();
    }

//
//
// Web methods
//
//

    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req,rsp);
        JSONObject json = req.getSubmittedForm();

        rootPOM = Util.fixEmpty(req.getParameter("rootPOM").trim());
        if(rootPOM!=null && rootPOM.equals("pom.xml"))   rootPOM=null;   // normalization

        goals = Util.fixEmpty(req.getParameter("goals").trim());
        setMavenOpts(Util.fixEmpty(req.getParameter("mavenOpts").trim()));
        settings = SettingsProvider.parseSettingsProvider(req);
        globalSettings = GlobalSettingsProvider.parseSettingsProvider(req);

        mavenName = req.getParameter("maven_version");
        aggregatorStyleBuild = !req.hasParameter("maven.perModuleBuild");
        if (json.optBoolean("usePrivateRepository"))
            localRepository = req.bindJSON(LocalRepositoryLocator.class,json.getJSONObject("explicitLocalRepository"));
        else
            localRepository = null;
        ignoreUpstremChanges = !json.has("triggerByDependency");
        if ( !ignoreUpstremChanges ) {
            JSONObject ignoreUpstremChangesConfig = json.optJSONObject("triggerByDependency");
            ignoreUnsuccessfulUpstreams = null != ignoreUpstremChangesConfig && ignoreUpstremChangesConfig.has("ignoreUnsuccessfulUpstreams");
        } else {
            ignoreUnsuccessfulUpstreams = false;
        }
        runHeadless = req.hasParameter("maven.runHeadless");
        incrementalBuild = req.hasParameter("maven.incrementalBuild");
        archivingDisabled = req.hasParameter("maven.archivingDisabled");
        siteArchivingDisabled = req.hasParameter("maven.siteArchivingDisabled");
        fingerprintingDisabled = req.hasParameter("maven.fingerprintingDisabled");
        resolveDependencies = req.hasParameter( "maven.resolveDependencies" );
        processPlugins = req.hasParameter( "maven.processPlugins" );
        mavenValidationLevel = NumberUtils.toInt(req.getParameter("maven.validationLevel"), -1);
        reporters.rebuild(req,json,MavenReporters.getConfigurableList());
        publishers.rebuildHetero(req, json, Publisher.all(), "publisher");
        buildWrappers.rebuild(req, json, BuildWrappers.getFor(this));
        JSONObject enableTriggerDownstreamProjects = json.optJSONObject("enableTriggerDownstreamProjects");
        if (enableTriggerDownstreamProjects == null) {
            disableTriggerDownstreamProjects = true;
            blockTriggerWhenBuilding = null;
        } else {
            disableTriggerDownstreamProjects = false;
            blockTriggerWhenBuilding = enableTriggerDownstreamProjects.has("blockTriggerWhenBuilding");
        }

        runPostStepsIfResult = Result.fromString(req.getParameter( "post-steps.runIfResult"));
        prebuilders.rebuildHetero(req,json, Builder.all(), "prebuilder");
        postbuilders.rebuildHetero(req,json, Builder.all(), "postbuilder");
    }

    /**
     * Delete all disabled modules.
     */
    public HttpResponse doDoDeleteAllDisabledModules() throws IOException, InterruptedException {
        checkPermission(DELETE);
        for( MavenModule m : getDisabledModules(true))
            m.delete();
        return HttpResponses.redirectToDot();
    }
    
    public AbstractTestResultAction<?> getTestResultAction() {
        MavenModuleSetBuild b = getLastBuild();
        return b != null ? b.getAction(AbstractTestResultAction.class) : null;
    }

    /**
     * Check the location of the POM, alternate settings file, etc - any file.
     */
    public FormValidation doCheckFileInWorkspace(@QueryParameter String value) throws IOException, ServletException {
        MavenModuleSetBuild lb = getLastBuild();
        if (lb!=null) {
            FilePath ws = lb.getModuleRoot();
            if(ws!=null)
                return ws.validateRelativePath(value,true,true);
        }
        return FormValidation.ok();
    }

    @Override
    public ContextMenu doChildrenContextMenu(StaplerRequest request, StaplerResponse response) throws Exception {
        ContextMenu menu = new ContextMenu();
        for (MavenModule mm : getModules()) {
            menu.add(mm);
        }
        return menu;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Descriptor is instantiated as a field purely for backward compatibility.
     * Do not do this in your code. Put @Extension on your DescriptorImpl class instead.
     */
    @Restricted(NoExternalUse.class)
    @Extension(ordinal=900)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractProjectDescriptor implements IconSpec {
        /**
         * Globally-defined MAVEN_OPTS.
         */
        private String globalMavenOpts;
        
        /**
         * @since 1.394
         */
        private Map<String, Integer> mavenValidationLevels = new LinkedHashMap<String, Integer>();

        /**
         * @since 1.448
         */
        private LocalRepositoryLocator localRepository = new DefaultLocalRepositoryLocator();

        public DescriptorImpl() {
            super();
            load();
            mavenValidationLevels.put( "DEFAULT", -1 );
            mavenValidationLevels.put( "LEVEL_MINIMAL", ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
            mavenValidationLevels.put( "LEVEL_MAVEN_2_0", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0 );
            mavenValidationLevels.put( "LEVEL_MAVEN_3_0", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );
            mavenValidationLevels.put( "LEVEL_MAVEN_3_1", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1 );
            mavenValidationLevels.put( "LEVEL_STRICT", ModelBuildingRequest.VALIDATION_LEVEL_STRICT );
        }

        @Override
        public String getHelpFile(String fieldName) {
            String v = super.getHelpFile(fieldName);
            if (v!=null)    return v;
            if (fieldName == null) {
                return null;
            }
            return Jenkins.getInstance().getDescriptor(Maven.class).getHelpFile(fieldName);
        }

        public List<SettingsProviderDescriptor> getSettingsProviders() {
            return Jenkins.getInstance().getDescriptorList(SettingsProvider.class);
        }
        
        public List<GlobalSettingsProviderDescriptor> getGlobalSettingsProviders() {
            return Jenkins.getInstance().getDescriptorList(GlobalSettingsProvider.class);
        }

        public String getGlobalMavenOpts() {
            return globalMavenOpts;
        }

        public void setGlobalMavenOpts(String globalMavenOpts) {
            this.globalMavenOpts = globalMavenOpts;
            save();
        }

        /**
         * @return never null.
         */
        public LocalRepositoryLocator getLocalRepository() {
            return localRepository!=null ? localRepository : new DefaultLocalRepositoryLocator();
        }

        public void setLocalRepository(LocalRepositoryLocator localRepository) {
            this.localRepository = localRepository;
            save();
        }

        public String getDisplayName() {
            return Messages.MavenModuleSet_DiplayName();
        }

        /**
         * Needed if it wants Maven projects are categorized in Jenkins 2.x.
         *
         * TODO: Override when the baseline is upgraded to 2.x
         *
         * @return A string it represents a ItemCategory identifier.
         */
        public String getCategoryId() {
            return "standalone-projects";
        }

        /**
         * Needed if it wants Maven projects are categorized in Jenkins 2.x.
         *
         * TODO: Override when the baseline is upgraded to 2.x
         *
         * @return A string with the Item description.
         */
        public String getDescription() {
            return Messages.MavenModuleSet_Description();
        }

        /**
         * Needed if it wants Maven projects are categorized in Jenkins 2.x.
         *
         * TODO: Override when the baseline is upgraded to 2.x
         *
         * @return A string it represents a URL pattern to get the Item icon in different sizes.
         */
        public String getIconFilePathPattern() {
            return "plugin/maven-plugin/images/:size/mavenmoduleset.png";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getIconClassName() {
            return "icon-maven-moduleset";
        }


        public MavenModuleSet newInstance(ItemGroup parent, String name) {
            MavenModuleSet mms = new MavenModuleSet(parent,name);
            mms.setSettings(GlobalMavenConfig.get().getSettingsProvider());
            mms.setGlobalSettings(GlobalMavenConfig.get().getGlobalSettingsProvider());
            return mms;
        }

        public Maven.DescriptorImpl getMavenDescriptor() {
            return Jenkins.getInstance().getDescriptorByType(Maven.DescriptorImpl.class);
        }
        
        /**
         * @since 1.394
         * @return
         */
        public Map<String, Integer> getMavenValidationLevels() {
            return mavenValidationLevels;
        }

        @Override
        public boolean configure( StaplerRequest req, JSONObject o ) {
            globalMavenOpts = Util.fixEmptyAndTrim(o.getString("globalMavenOpts"));
            localRepository = req.bindJSON(LocalRepositoryLocator.class,o.getJSONObject("localRepository"));
            save();

            return true;
        }

        @Override
        public boolean isApplicable(Descriptor descriptor) {
            return !NOT_APPLICABLE_TYPES.contains(descriptor.clazz);
        }

        @SuppressWarnings("unchecked")
        private static final Set<Class> NOT_APPLICABLE_TYPES = new HashSet<Class>(Arrays.asList(
            JavadocArchiver.class,  // this kicks in automatically
            Mailer.class,           // for historical reasons, Maven uses MavenMailer
            JUnitResultArchiver.class // done by SurefireArchiver
        ));

        static {
            IconSet.icons.addIcon(
                    new Icon("icon-maven-moduleset icon-sm",
                            "plugin/maven/images/16x16/mavenmoduleset.png", Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-maven-moduleset icon-md",
                            "plugin/maven/images/24x24/mavenmoduleset.png", Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-maven-moduleset icon-lg",
                            "plugin/maven/images/32x32/mavenmoduleset.png", Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-maven-moduleset icon-xlg",
                            "plugin/maven/images/48x48/mavenmoduleset.png", Icon.ICON_XLARGE_STYLE));
        }
    }
    
    private static final Logger LOGGER = Logger.getLogger(MavenModuleSet.class.getName());
}
