## New release available here  [https://github.com/jenkinsci/maven-plugin/releases](https://github.com/jenkinsci/maven-plugin/releases)

## Release Notes

### Version 3.6 ()

- Do not bundle commons-io in this plugin's HPI file [#135](https://github.com/jenkinsci/maven-plugin/pull/135)

### Version 3.5 (March 19, 2020)

- upgrade some librairies to fix PCT testing

### Version 3.4 (July 31, 2019)

-   [Fix security issue](https://jenkins.io/security/advisory/2019-07-31/#SECURITY-713)

### Version 3.3 (June 14, 2019)

-   ![(tick)](docs/images/check.svg) Fixed: maven-plugin random socket leak, leading to
    threads leak on slave and master
    ([JENKINS-57119](https://issues.jenkins-ci.org/browse/JENKINS-57119))
-   ![(tick)](docs/images/check.svg) Fixed: [java.io](http://java.io).NotSerializableException:
    The calling thread Thread has no associated
    channel ([JENKINS-57244](https://issues.jenkins-ci.org/browse/JENKINS-57244))
-   ![(tick)](docs/images/check.svg) Fixed: Upgrade maven embedder from 3.1.0 to at least
    3.5.4 ([JENKINS-54530](https://issues.jenkins-ci.org/browse/JENKINS-54530)

### Version 3.2 (November 30, 2018)

-   ![(tick)](docs/images/check.svg) Fixed: remove anonymous classes
    ([JENKINS-53481](https://issues.jenkins-ci.org/browse/JENKINS-53481)

### Version 3.1.2 (March 27, 2018)

-   ![(tick)](docs/images/check.svg) Fixed: JEP-200 failure to serialize Notifier, used
    when \<ciManagement\> specifies email configuration. ([JENKINS-50251](https://issues.jenkins-ci.org/browse/JENKINS-50251) -
    Getting issue details... STATUS  ) - Take 2 - fix a potential NPE

### Version 3.1.1 (March 25, 2018)

-   ![(tick)](docs/images/check.svg) Fixed: JEP-200 failure to serialize Notifier, used
    when \<ciManagement\> specifies email configuration. ([JENKINS-50251](https://issues.jenkins-ci.org/browse/JENKINS-50251) -
    Getting issue details... STATUS  )

### Version 3.1.1 (January 24, 2018)

-   ![(tick)](docs/images/check.svg) Fixed: the perform method should be passing the
    loop-variable 'moduleBuild' to tdp.getTestData ([PR\#12](https://github.com/jenkinsci/maven-plugin/pull/12))
-   ![(tick)](docs/images/check.svg) Fixed: UnsupportedOperationException: Refusing to
    marshal org.apache.maven.artifact.versioning.DefaultArtifactVersion
    for security reasons ( [JENKINS-49089](https://issues.jenkins-ci.org/browse/JENKINS-49089) -
    Getting issue details... STATUS  )
-   ![(tick)](docs/images/check.svg) Fixed: Forbid nested references to model objects ( 
    [JENKINS-45892](https://issues.jenkins-ci.org/browse/JENKINS-45892) -
    Getting issue details... STATUS  )
-   ![(info)](docs/images/information.svg) Internal: Update to Apache HttpComponents
    Client API 4.5.3-2.1
-   ![(info)](docs/images/information.svg) Internal: Update Lib Maven Embedder to 3.13
-   Includes: [PR\#12](https://github.com/jenkinsci/maven-plugin/pull/12), [PR\#105](https://github.com/jenkinsci/maven-plugin/pull/105), [PR\#106](https://github.com/jenkinsci/maven-plugin/pull/106), [PR\#108](https://github.com/jenkinsci/maven-plugin/pull/108), [PR\#109](https://github.com/jenkinsci/maven-plugin/pull/109), [PR\#111](https://github.com/jenkinsci/maven-plugin/pull/111), [PR\#112](https://github.com/jenkinsci/maven-plugin/pull/112)

### Version 3.0 (October 6, 2017)

-   ![(tick)](docs/images/check.svg)
     Fixed: Global environment variables are not being
    resolved in Email Notification Recipients list for maven 2/3
    projects ([JENKINS-13277](https://issues.jenkins-ci.org/browse/JENKINS-13277)
-   ![(tick)](docs/images/check.svg)
     Fixed: Allow to automatically trigger jobs when a
    project import the dependencyManagement of another
    ([JENKINS-15883](https://issues.jenkins-ci.org/browse/JENKINS-15883)
-   ![(info)](docs/images/information.svg)
     Internal: Update HttpClient and use the
    client from [Apache HttpComponents Client 4.x API Plugin](https://wiki.jenkins.io/display/JENKINS/Apache+HttpComponents+Client+4.x+API+Plugin) ([JENKINS-46053](https://issues.jenkins-ci.org/browse/JENKINS-46053),
    [JENKINS-46210](https://issues.jenkins-ci.org/browse/JENKINS-46210)
-   ![(info)](docs/images/information.svg)
    Internal: Use the JSch library from the [JSch Plugin](https://wiki.jenkins.io/display/JENKINS/JSch+Plugin)
-   ![(info)](docs/images/information.svg)
     Internal: Fix Parent
    POM ([JENKINS-45271](https://issues.jenkins-ci.org/browse/JENKINS-45271)
-   ![(info)](docs/images/information.svg)
     Internal: Maven Plugin uses obsolete maven
    methods
    ([JENKINS-46148](https://issues.jenkins-ci.org/browse/JENKINS-46148)  
-   ![(tick)](docs/images/check.svg)
     Stop bundling libraries, which are provided by the
    core (e.g. Guava)
    -   Note that some of the changes may lead to compatibility issues
        in the plugin dependencies

### Version 2.17 (July 10, 2017)

-   ![(tick)](docs/images/check.svg)
     Fixed: Maven version detection fails on same agent
    with 'java.lang.IllegalStateException: zip file closed' exception.  
    Update the Maven Embedder Lib to 3.12.1 :
    -   [JENKINS-42549](https://issues.jenkins-ci.org/browse/JENKINS-42549) -
        Prevent file access errors in `JARUrlConnection` due to the
        parallel reading of JAR resources
        in `MavenEmbedderUtils#getMavenVersion()` (regression in 3.12)
    -   [JENKINS-40621](https://issues.jenkins-ci.org/browse/JENKINS-40621) -
        Prevent leaked file descriptors when
        invoking `MavenEmbedderUtils#getMavenVersion()`([PR\#5](https://github.com/jenkinsci/lib-jenkins-maven-embedder/pull/5)
-   ![(info)](docs/images/information.svg)
     Update: Update plugin description in the
    plugin manager ( Maven plugin is not being installed by default
    starting from Jenkins 2 )
    ([PR\#99](https://github.com/jenkinsci/maven-plugin/pull/99) )
-   ![(info)](docs/images/information.svg)
     Update: Remove the message about
    jenkinsci-users ML (
    [PR\#97](https://github.com/jenkinsci/maven-plugin/pull/97) )

### Version 2.16 (June 08, 2017)

-   ![(tick)](docs/images/check.svg)
     Fixed: Support of Maven 3.5 (due to non backward
    compatible change in Apache Maven 3.5) (
    [JENKINS-43446](https://issues.jenkins-ci.org/browse/JENKINS-43446))
-   ![(tick)](docs/images/check.svg)
     Fixed: fix typo in image sources
    ([JENKINS-42833](https://issues.jenkins-ci.org/browse/JENKINS-42833))

### Version 2.15.1 (Feb 12, 2017)

-   ![(tick)](docs/images/check.svg)
     Fixed: Maven projects using Maven 3.0.x don't show
    annotations in console log since 2.44 / 2.32.2 (
    [JENKINS-41636](https://issues.jenkins-ci.org/browse/JENKINS-41636),
    SECURITY-382 )
-   ![(tick)](docs/images/check.svg)
     Fixed: Prevent leaked file descriptors when invoking
    `MavenEmbedderUtils#getMavenVersion()` (
    [JENKINS-40621](https://issues.jenkins-ci.org/browse/JENKINS-40621)
    )
-   ![(tick)](docs/images/check.svg)
     Fixed: NullPointerException when Jenkins uses by
    error the Maven 3.2.x launcher with a project using Maven 3.3.x (
    [JENKINS-41761](https://issues.jenkins-ci.org/browse/JENKINS-41761)
    )
-   ![(tick)](docs/images/check.svg)
     Fixed: StackOverflowError when parsing Maven POM (
    [JENKINS-41697](https://issues.jenkins-ci.org/browse/JENKINS-41697),
    [JENKINS-42183](https://issues.jenkins-ci.org/browse/JENKINS-42183)
    ). It seems to impact only users of Jenkins \>= LTS 2.32.1 (and thus
    probably users of weekly releases \>= 2.28 where guice was upgraded
    to version 4.0 final)
-   ![(info)](docs/images/information.svg)
     Update: Use Apache Maven Owl logo instead of
    the Apache Feather ( We don't have the rights ) (
    [JENKINS-38960](https://issues.jenkins-ci.org/browse/JENKINS-38960)
    )
-   ![(info)](docs/images/information.svg)
     Internal: Fix tests for Jenkins 2, use a
    minimum set of permalinks instead of a fixed set (
    [JENKINS-40949](https://issues.jenkins-ci.org/browse/JENKINS-40949)
    )
-   ![(info)](docs/images/information.svg)
     Internal: Update dependencies
    -   Do not bundle Guice (we pick it up from core).
    -   Update Sonatype Aether `0.9.0.M2` to Eclipse Aether `1.1.0`
    -   Update Apache Wagon `2.4` to `2.12` which solves various issues
        (like SNI support -
        [JENKINS-40903](https://issues.jenkins-ci.org/browse/JENKINS-40903),
        [JENKINS-38738](https://issues.jenkins-ci.org/browse/JENKINS-38738)
        ) to download dependencies in `Parsing POM` or upload artifacts
        from the post build deployment task.
    -   Internal: Upgrade lib-jenkins-maven-embedder `3.11` to `3.12`

### Version 2.15 (Feb 16, 2017)

Release failed .... thx repo.jenkins-ci.org

### Version 2.14 (Oct 18, 2016)

Requires now Jenkins \>= 1.625.3 and Java \>= 7

-   ![(tick)](docs/images/check.svg)
     Fixed: Failed maven builds using -T are showing up
    as Aborted ([JENKINS-24832](https://issues.jenkins-ci.org/browse/JENKINS-24832))
-   ![(tick)](docs/images/check.svg)
     Fixed: Apache Maven 3.3 support and toolchains
    integration (
    [JENKINS-28629](https://issues.jenkins-ci.org/browse/JENKINS-28629),
    [JENKINS-28420](https://issues.jenkins-ci.org/browse/JENKINS-28420)
    )
-   ![(tick)](docs/images/check.svg)
     Fixed: Option "Schedule build when some upstream has
    no successful builds" is never saved (
    [JENKINS-37937](https://issues.jenkins-ci.org/browse/JENKINS-37937)
    )
-   ![(tick)](docs/images/check.svg)
     Fixed: With Jenkins 2.x Maven configuration screen
    is linking to the wrong configuration page when you have no maven
    installation configured (
    [JENKINS-38923](https://issues.jenkins-ci.org/browse/JENKINS-38923),
    [JENKINS-36068](https://issues.jenkins-ci.org/browse/JENKINS-36068),
    [JENKINS-34743](https://issues.jenkins-ci.org/browse/JENKINS-34743)
    )
-   ![(tick)](docs/images/check.svg)
     Fixed: Release builds should not trigger downstream
    projects ([JENKINS-34789](https://issues.jenkins-ci.org/browse/JENKINS-34789))
-   ![(tick)](docs/images/check.svg)
     Fixed: Redeploy publisher must reuse global maven
    settings from the slave (
    [JENKINS-27161](https://issues.jenkins-ci.org/browse/JENKINS-27161)
    )
-   ![(tick)](docs/images/check.svg)
     Fixed: Misleading "JDK 5 not supported with Maven"
    error with Jenkins \>= 1.612 and JDK 6 (
    [JENKINS-28294](https://issues.jenkins-ci.org/browse/JENKINS-28294)
    )
-   ![(plus)](docs/images/add.svg)  New: Automatically detect tests from
    com.simpligility.maven.plugins:android-maven-plugin:internal-integration-test
-   ![(info)](docs/images/information.svg)
     Update: NL translation for
    MavenProbeAction.DisplayName
-   ![(info)](docs/images/information.svg)
     Internal: Remove usage of IOException2
-   ![(info)](docs/images/information.svg)
     Internal: Upgrade Sisu to 0.3.3 (
    [JENKINS-38736](https://issues.jenkins-ci.org/browse/JENKINS-38736)
    )

### Version 2.13 (May 19, 2016)

-   [JENKINS-31162](https://issues.jenkins-ci.org/browse/JENKINS-31162)
    New item categorization and dynamic choice offering
-   [JENKINS-33387](https://issues.jenkins-ci.org/browse/JENKINS-33387)
    Transient actions aren't displayed in Maven Jobs
-   [JENKINS-31258](https://issues.jenkins-ci.org/browse/JENKINS-31258)
    Jenkins Maven plug-in ignores JUnit-format test results from unknown
    Maven plug-ins
-   [JENKINS-31524](https://issues.jenkins-ci.org/browse/JENKINS-31524)
    SurefireArchiver ignores updated results when multiple testing
    plug-ins use the same reports directory (See [Building a maven2 project](https://wiki.jenkins.io/display/JENKINS/Building+a+maven2+project) -
    Maven Surefire Test Results)
-   [JENKINS-28147](https://issues.jenkins-ci.org/browse/JENKINS-28147)
    Perform Environment tearDown if BuildWrapper fails
-   [JENKINS-7010](https://issues.jenkins-ci.org/browse/JENKINS-7010)
    Maven settings configured at Maven job level aren't used inside the
    promotion step
-   [JENKINS-4428](https://issues.jenkins-ci.org/browse/JENKINS-4428)
    MavenProbeAction exposes password parameters
-   [JENKINS-32635](https://issues.jenkins-ci.org/browse/JENKINS-32635)
    New option ignoreUnsuccessfulUpstreams. If checked, Jenkins will
    schedule build even if some upstream project has no successful
    builds. If not checked, Jenkins will not schedule build when some
    SNAPSHOT if any other upstream project has no successful builds on
    this Jenkins.

### Version 2.12.1 (Oct 01, 2015)

-   [JENKINS-22252](https://issues.jenkins-ci.org/browse/JENKINS-22252)
    Reverting fix for
    [JENKINS-26947](https://issues.jenkins-ci.org/browse/JENKINS-26947)
    due to serious regression (`IllegalAccessError` on
    `AbstractMapBasedMultimap`).

### Version 2.12 (Aug 27, 2015)

-   [JENKINS-26947](https://issues.jenkins-ci.org/browse/JENKINS-26947)
    Forcibly terminate Maven remoting channel when upstream channel is
    closed.
-   [JENKINS-21746](https://issues.jenkins-ci.org/browse/JENKINS-21746)
    Introduced `TcpSocketHostLocator` extension point. Using newer
    interceptors library.

### Version 2.11 (Aug 07, 2015)

This version requires Jenkins 1.580.1 or later.

-   [JENKINS-25272](https://issues.jenkins-ci.org/browse/JENKINS-25272)
    Update
    [JENKINS-18403](https://issues.jenkins-ci.org/browse/JENKINS-18403)
    workaround for newer Jenkins versions: allow projects to be built
    using JDK 5.
-   [JENKINS-25625](https://issues.jenkins-ci.org/browse/JENKINS-25625)
    Simplified dependencies for development from other plugins.

### Version 2.10 (Jun 08, 2015)

-   [JENKINS-25406](https://issues.jenkins-ci.org/browse/JENKINS-25406)
    Error running Maven builds including static analysis on new core
    under some conditions.
-   Improved logging of incremental build behavior.

### Version 2.9 (March 18, 2015)

-   nothing special :-) see commits logs

### Version 2.8 (Nov 21, 2014)

-   [issue#25691](https://issues.jenkins-ci.org/browse/JENKINS-25691) Redeploy
    link is displayed to Anonymous users with read only permissions for
    a job

#### Version 2.7 (Oct 10, 2014)

-   [issue#11964](https://issues.jenkins-ci.org/browse/JENKINS-11964) Cannot
    build a single module in a Maven multi-module job with Maven 3
-   [issue#11078](https://issues.jenkins-ci.org/browse/JENKINS-11078) NullPointerException
    in
    hudson.maven.Maven3Builder$MavenExecutionListener.recordProjectStarted
-   [issue#20884](https://issues.jenkins-ci.org/browse/JENKINS-20884) Variable
    expansion in maven goals
-   [issue#21903](https://issues.jenkins-ci.org/browse/JENKINS-21903) Unless
    the user has requested to block when upstream is building, do not
    skip triggering a downstream build just because an upstream is
    building.
-   [issue#4861](https://issues.jenkins-ci.org/browse/JENKINS-4861) Use
    newer version of transitive plexus-utils dependency that allows to
    correctly deploy artifacts using scpexe.
-   [issue#24282](https://issues.jenkins-ci.org/browse/JENKINS-24282) Use
    noun phrases for new items
-   run a full build if build was triggered by upstream (snapshot
    dependency)
-   translation updates

#### Version 2.6 (Aug 11 2014)

-   [JENKINS-23263](https://issues.jenkins-ci.org/browse/JENKINS-23263)
    Code change to prepare for split of JUnit plugin from core.
-   Better diagnostic logging in case dependency graph calculation
    fails.
-   [JENKINS-23686](https://issues.jenkins-ci.org/browse/JENKINS-23686)
    New reverse build trigger (1.560+) made to work with a Maven project
    downstream.
-   Improved checkbox appearance in configuration UI.

#### Version 2.5 (Jul 11 2014)

-   [JENKINS-23098](https://issues.jenkins-ci.org/browse/JENKINS-23098)
    Deadlocks when running builds with `-T` (concurrency).

#### Version 2.4 (Jul 03 2014)

-   Better handle errors from `MavenReporter.postExecute`.
-   Incorrect root element for module `config.xml` files.
-   Handling new names for SOAPUI extension.
-   [JENKINS-11333](https://issues.jenkins-ci.org/browse/JENKINS-11333)
    Allow users to disable automatic fingerprinting, but add in explicit
    fingerprinting if desired.
-   Improved consistency of labels.
-   Localization and help fixes.
-   [JENKINS-21014](https://issues.jenkins-ci.org/browse/JENKINS-21014)
    Include verify lifecycle in upstream candidate calculation.

#### Version 2.3 (Apr 30 2014)

-   Fixed: Sites for nested Maven multi-modules projects deeper than one
    level are archived flat [issue#22673](https://issues.jenkins-ci.org/browse/JENKINS-22673)
-   Fixed: NPE while loading jobs [issue#22647](https://issues.jenkins-ci.org/browse/JENKINS-22647)

#### Version 2.2 (Apr 3 2014)

-   Fixed: significant improvements in the Maven build performance
    [JENKINS-22354](https://issues.jenkins-ci.org/browse/JENKINS-22354)
-   Fixed: NullPointerException during parsing POM
    [JENKINS-21279](https://issues.jenkins-ci.org/browse/JENKINS-21279)
-   Fixed: maven.build.timestamp.format is not obeyed in maven buids
    [JENKINS-9693](https://issues.jenkins-ci.org/browse/JENKINS-9693)
-   New: Expose project actions of pre- and post- builders
    [JENKINS-20506](https://issues.jenkins-ci.org/browse/JENKINS-20506)
-   New: Use a pop-up dialog instead of extra page for confirmation if
    user wants to delete all disabled maven modules. ([pull request #17](https://github.com/jenkinsci/maven-plugin/pull/17)

#### Version 2.1 (Dec 17 2013); requires 1.538+

-   Fixed: don't save per-job MAVEN\_OPTS if they're the same as the
    global ones
    [JENKINS-13926](https://issues.jenkins-ci.org/browse/JENKINS-13926)
-   New: Expose Maven properties (e.g. GAV) as environment variables
    [JENKINS-18272](https://issues.jenkins-ci.org/browse/JENKINS-18272)
-   Fixed: Abort module build when maven build is aborted
    [JENKINS-19801](https://issues.jenkins-ci.org/browse/JENKINS-19801)
-   Fixed: Show aggregated failures same way Matrix build does
    [JENKINS-19884](https://issues.jenkins-ci.org/browse/JENKINS-19884)
-   Fixed: Maven plugin sends email to 'null'
    adress [JENKINS-20209](https://issues.jenkins-ci.org/browse/JENKINS-20209)
-   Fixed: provide better error message if maven\_home isn't set
    [JENKINS-20385](https://issues.jenkins-ci.org/browse/JENKINS-20385)
-   Fixed: call postBuild after module build completion
    [JENKINS-20487](https://issues.jenkins-ci.org/browse/JENKINS-20487)
-   Fixed: Hacks used by MavenMailer to load config.jelly from plain
    Mailer broke in mailer 1.6
    [JENKINS-21045](https://issues.jenkins-ci.org/browse/JENKINS-21045)
-   New: support for play2-maven-plugin

#### Version 2.0.4 (Jul 03 2014)

-   [JENKINS-11333](https://issues.jenkins-ci.org/browse/JENKINS-11333)
    Allow users to disable automatic fingerprinting, but add in explicit
    fingerprinting if desired.

#### Version 2.0.3 (Jan 28, 2014)

-   Fixed regression in 2.0.2 that RedeployPublisher would delete
    artifacts from the build after running.

#### Version 2.0.2 (Jan 23, 2014)

-   Temporary file leak when using [Cloudbees Deployer Plugin](https://wiki.jenkins.io/display/JENKINS/Cloudbees+Deployer+Plugin)

#### Version 2.0.1 (Jan 04, 2014)

-   Fixed: don't save per-job MAVEN\_OPTS if they're the same as the
    global ones
    [JENKINS-13926](https://issues.jenkins-ci.org/browse/JENKINS-13926)
-   New: Expose Maven properties (e.g. GAV) as environment variables
    [JENKINS-18272](https://issues.jenkins-ci.org/browse/JENKINS-18272)
-   Fixed: Show aggregated failures same way Matrix build does
    [JENKINS-19884](https://issues.jenkins-ci.org/browse/JENKINS-19884)
-   Fixed: Maven plugin sends email to 'null'
    adress [JENKINS-20209](https://issues.jenkins-ci.org/browse/JENKINS-20209)
-   Fixed: provide better error message if maven\_home isn't set
    [JENKINS-20385](https://issues.jenkins-ci.org/browse/JENKINS-20385)
-   Fixed: call postBuild after module build completion
    [JENKINS-20487](https://issues.jenkins-ci.org/browse/JENKINS-20487)
-   Fixed: Hacks used by MavenMailer to load config.jelly from plain
    Mailer broke in mailer 1.6
    [JENKINS-21045](https://issues.jenkins-ci.org/browse/JENKINS-21045)
-   New: support for play2-maven-plugin

#### Version 2.0 (Oct 22, 2013)

-   Fixed: no errors logged in Jenkins' console if build failed
    ([JENKINS-19352](https://issues.jenkins-ci.org/browse/JENKINS-19352))
-   Fixed: too verbose logging in Maven builds
    ([JENKINS-19396](https://issues.jenkins-ci.org/browse/JENKINS-19396))
-   New: support notifications via the ciManagement section of the POM
    ([JENKINS-1201](https://issues.jenkins-ci.org/browse/JENKINS-1201),
    [JENKINS-6421](https://issues.jenkins-ci.org/browse/JENKINS-6421))
-   Fixed: build may fail if master and slave use different VM types
    ([JENKINS-19978](https://issues.jenkins-ci.org/browse/JENKINS-19978))
-   Fixed: Set the correct status of a maven build, in case that one
    module has test failures and other module don't compile
    ([JENKINS-16522](https://issues.jenkins-ci.org/browse/JENKINS-16522))
