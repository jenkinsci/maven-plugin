## Jenkins plugin for building Maven 2/3 jobs

![](docs/images/hn.png)

**Maven jobs and Java versions compatibility** : Because java serialized classes are exchanged
between Jenkins Controller and Maven Jobs it is required that the JVM used
to launch Maven is superior or equal to the version of Java for which
Jenkins Controller is built for.

-   Jenkins \>= 1.520 requires Java 6 thus Maven jobs must be launched
    with Java \>= 6.
-   Jenkins \>= 1.612 requires Java 7 thus Maven jobs must be launched
    with Java \>= 7.
-   Jenkins \>= 2.54 requires Java 8 thus Maven jobs must be launched
    with Java \>= 8.
-   Jenkins \>= 2.357 requires Java 11 thus Maven jobs must be launched
    with Java \>= 11.

See also
[JENKINS-18403](https://issues.jenkins.io/browse/JENKINS-18403),
[JENKINS-28294](https://issues.jenkins.io/browse/JENKINS-28294)

**If Jenkins detects that you are trying to use a JDK older than the
controller prerequisite, it automatically reconfigures your build to use the
JDK on which your agent is running.** It displays in your build logs a
message like :

```
ERROR: ================================================================================
ERROR: Invalid project setup: hudson/maven/AbstractMavenProcessFactory$ConfigureOriginalJDK : Unsupported major.minor version 51.0
ERROR: [JENKINS-18403][JENKINS-28294] JDK 'j6' not supported to run Maven projects.
ERROR: Maven projects have to be launched with a Java version greater or equal to the minimum version required by the controller.
ERROR: Use the Maven JDK Toolchains (plugin) to build your maven project with an older JDK.
ERROR: Retrying with agent Java and setting compile/test properties to point to /Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/.
ERROR: ================================================================================
```

**But due to the remoting upgrade in Jenkins 2.27+ this workaround
doesn't work anymore if your agent or Maven job is using Java \< 7 -
[JENKINS-40990](https://issues.jenkins.io/browse/JENKINS-40990)
(Because remoting is compiled for Java 7)**

Known issues are listed [in Jira](https://issues.jenkins.io/issues/?jql=resolution%20is%20EMPTY%20and%20component%3D16033)

Historically this plugin was released alongside Jenkins core releases.
2.0 releases up to 2.204.1 saw this plugin released separately, but still bundled
with Jenkins, although not always in the newest version.
See [Stop bundling Maven plugin and Subversion plugin with Jenkins](https://www.jenkins.io/doc/upgrade-guide/2.204/#stop-bundling-maven-plugin-and-subversion-plugin-with-jenkins).

## Version history

See [GitHub releases](https://github.com/jenkinsci/maven-plugin/releases) for recent releases and [the legacy changelog](https://github.com/jenkinsci/maven-plugin/blob/maven-plugin-3.19/CHANGELOG.md) for older releases.

## Risks

The Jenkins project recommends that users transition from the Maven job type to use Pipeline jobs or freestyle jobs.
Stephen Connolly describes many of the reasons in his 2013 blog post, ["Jenkins' Maven job type considered evil"](http://javaadventure.blogspot.com/2013/11/jenkins-maven-job-type-considered-evil.html).
A revised version of that blog post is included here.

There are multiple ways to build a Maven project with Jenkins:

* Use a free-style project with a Maven build step
* Use a Maven-style project
* Use a Pipeline project with a shell, batch, or powershell build step that calls Maven

The first way runs the build as Maven intended.
The second way adds a whole lot of hooks and can even modify the build in ways that Maven did not intend.

The first way requires that you configure stuff yourself.
The second way tries to "guess" what you want and auto-configure it.

The first way is initially less user friendly, i.e. you have more UI to click through to get the full set of reports.
The second way is initially more user friendly... but when things go wrong... well sorry out of luck.

If something goes wrong with the first way, worst case you add a shell build step above the Maven build step that just runs SET, trigger a build, login to the build agent, switch to the user the build is running as, apply the environment your SET build step output and then run the Maven command that the build's console log captured.
That will give you an exact reproduction of the Maven build and you can debug why your build is not working.
When something goes wrong with the second way, well good luck.
By all means try to do the same as you would for a freestyle project, but at the end of the day, there is no way you can replicate the injected hooks that Jenkins puts into your Maven build.
You can get an approximate reproduction, and hey, that may just be enough to let you figure out what is wrong and fix your build... but there are cases where you cannot.

It is very attractive because is easy to configure (so users use it) and gives nice per-module reports.
When it blows up, and it will blow up, it blows up big.

## Features of Maven job type

The Jenkins project recommends Pipeline jobs and freestyle jobs for Maven projects.
See the earlier section to understand the risks associated with using the Maven job type instead of Pipeline or freestyle jobs.

This plugin provides a more advanced integration with additional features like:

-   Automatic configuration of reporting plugins (Junit, Findbugs, ...)
-   Automatic triggering across jobs based on SNAPSHOTs published/consumed
-   Incremental build - only build changed modules
-   Build modules in parallel on multiple executors/nodes
-   Post build deployment of binaries only if the project succeeded and all tests passed
-   ...

## Environment variables

This plugin exposes variables found from the project's POM (as of version 2.1):

-   **POM\_DISPLAYNAME** - taken from `<name>` in POM
-   **POM\_VERSION** - taken from `<version>` in POM
-   **POM\_GROUPID** - taken from `<groupId>` in POM
-   **POM\_ARTIFACTID** - taken from `<artifactId>` in POM
-   **POM\_PACKAGING** - taken from `<packaging>` in POM

## Building a project

Jenkins provides a job type dedicated to Maven 2/3.
This job type integrates Jenkins deeply with Maven 2/3 and provides the following benefits compared to the more generic Pipeline and free-style software projects.

Jenkins parses Maven POMs to obtain much of the information needed to do its work.
As a result, the amount of configuration is drastically reduced.
Jenkins listens to Maven execution and figures out what should be done when on its own.
For example, it will automatically record the JUnit report when Maven runs the test phase.
Or if you run the javadoc goal, Jenkins will automatically record javadoc.
Jenkins automatically creates project dependencies between projects which declare SNAPSHOT dependencies between each other.
Thus mostly you just need to configure SCM information and what goals you'd like to run, and Jenkins will figure out everything else.

## Automatic build chaining from module dependencies

Jenkins reads dependencies of your project from your POM and if they are also built on Jenkins, triggers are configured so that a new build in one of those dependencies will automatically start a new build of your project.
Jenkins understands many types of dependencies in the POM, including:

* parent POM
* `<dependencies>` section of your project
* `<plugins>` section of your project
* `<extensions>` section of your project
* `<reporting>` section of your project

This process takes versions into account, so you can have multiple versions/branches of your project on the same Jenkins and it will correctly determine dependencies.
Note that dependency version ranges are not supported, see [JENKINS-2787](https://issues.jenkins.io/browse/JENKINS-2787) for the reason.

This feature can be disabled on demand - see configuration option `Build whenever a SNAPSHOT dependency is built`.

## Maven surefire test results

The Maven Integration plugin understands the POM and knows about specific Maven testing plugins, such as the [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) and the [Tycho Surefire Plugin](https://www.eclipse.org/tycho/sitedocs/tycho-surefire-plugin/plugin-info.html).
The definitive list of supported test plugins can be found in the [TestMojo source code](https://github.com/jenkinsci/maven-plugin/blob/master/src/main/java/hudson/maven/reporters/TestMojo.java).

For such known test plugins the Maven Integration plugin is able to collect test results from their default or POM-configured reports directory.
Test results from Maven test-capable plugins unknown to the Maven Integration plugin can be collected if their execution goal is 'test', 'test-run', 'integration-test' and they have a 'reportsDirectory' configuration property containing the location of their test results.
Other than that results from unknown test plugins (such as org.codehaus.mojo:exec-maven-plugin) will not be collected, even if they are returned in the configured test results location (default `target/surefire-reports`).

### Collecting test results from arbitrary test plugins

As of org.jenkins-ci.main:maven-plugin:2.13, there is a mechanism to inform the Maven Integration plugin of the location of test results produced by an unknown Maven test plug-in.
If the POM declares a property with a name matching the pattern 'jenkins.<plugin-execution-id>.reportsDirectory', where <plugin-execution-id> is the unknown test plug-in's execution identifier, the Maven Integration plugin will resolve the value of this property against the project base directory and collect any test results that it finds in the resulting directory.

### Example

In order to have Jenkins retrieve the test results produced by an unknown plugin with a plugin execution ID 'e2eTests' which generates JUnit-compatible XML reports in the directory `target/protractor-reports` in the project workspace, add the following property to your project POM:

```xml
<properties>
    <jenkins.e2eTests.reportsDirectory>target/protractor-reports</jenkins.e2eTests.reportsDirectory>
</properties>
```
