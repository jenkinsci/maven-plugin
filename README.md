maven-plugin
============
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/maven-plugin)](https://plugins.jenkins.io/maven-plugin)
[![Changelog](https://img.shields.io/github/v/tag/jenkinsci/maven-plugin?label=changelog)](https://github.com/jenkinsci/maven-plugin/blob/master/CHANGELOG.md)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/maven-plugin?color=blue)](https://plugins.jenkins.io/maven-plugin)
Jenkins plugin for building Maven 2/3 jobs.
See [Maven Project Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Maven+Project+Plugin) on Jenkins' Wiki for more information.

## Version history

See [the changelog](CHANGELOG.md)

**![](docs/images/hn.png)

Maven jobs and Java versions compatibility** : Because java serialized classes are exchanged
between Jenkins master and Maven Jobs it is required that the JVM used
to launch Maven is superior or equal to the version of Java for which
Jenkins Master is built for.

-   Jenkins \>= 1.520 requires Java 6 thus Maven jobs must be launched
    with Java \>= 6.
-   Jenkins \>= 1.612 requires Java 7 thus Maven jobs must be launched
    with Java \>= 7.
-   Jenkins \>= 2.54 requires Java 8 thus Maven jobs must be launched
    with Java \>= 8.

See also
[JENKINS-18403](https://issues.jenkins-ci.org/browse/JENKINS-18403),
[JENKINS-28294](https://issues.jenkins-ci.org/browse/JENKINS-28294)

**If Jenkins detects that you are trying to use a JDK older than the
master prerequisite, it automatically reconfigure your build to use the
JDK on which your agent is running.** It displays in your build logs a
message like :

``` syntaxhighlighter-pre
ERROR: ================================================================================
ERROR: Invalid project setup: hudson/maven/AbstractMavenProcessFactory$ConfigureOriginalJDK : Unsupported major.minor version 51.0
ERROR: [JENKINS-18403][JENKINS-28294] JDK 'j6' not supported to run Maven projects.
ERROR: Maven projects have to be launched with a Java version greater or equal to the minimum version required by the master.
ERROR: Use the Maven JDK Toolchains (plugin) to build your maven project with an older JDK.
ERROR: Retrying with slave Java and setting compile/test properties to point to /Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/.
ERROR: ================================================================================
```

**But due to the remoting upgrade in Jenkins 2.27+ this workaround
doesn't work anymore if your agent or maven job is using Java \< 7 -
[JENKINS-40990](https://issues.jenkins-ci.org/browse/JENKINS-40990)
(Because remoting is compiled for Java 7)**

Known issues are listed [in Jira](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20status%20in%20(Open%2C%20%22In%20Progress%22%2C%20Reopened)%20AND%20component%20%3D%20%27maven-plugin%27)

Historically this plugin was released alongside Jenkins core releases.
Since version 2.0 this plugin is released separately, but still bundled
with Jenkins - though not always the newest version of the plugin might
be bundled.

This plugin provides an advanced integration for Maven 2/3 projects.

Even if Jenkins provides natively a Maven builder to use a build step in
classical Jenkins jobs (freestyle, ...) this plugin provides a more
advanced integration with specific a specific job type providing uniq
features like:

-   Automatic configuration of reporting plugins (Junit, Findbugs, ...)
-   Automatic triggering across jobs based on SNAPSHOTs
    published/consumed
-   Incremental build - only build changed modules
-   Build modules in parallel on multiple executors/nodes
-   Post build deployment of binaries only if the project succeeded and
    all tests passed
-   ...

See [Building a maven2
project](https://wiki.jenkins.io/display/JENKINS/Building+a+maven2+project)
for more information on how to use this.

**Environment Variables**

This plugin exposes variables found from the project's POM (as of
version 2.1):

-   POM\_DISPLAYNAME
-   POM\_VERSION
-   POM\_GROUPID
-   POM\_ARTIFACTID
-   POM\_PACKAGING

And many others features provided by Jenkins plugins ecosystem
