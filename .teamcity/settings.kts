import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.failureConditions.BuildFailureOnText
import jetbrains.buildServer.configs.kotlin.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.failureConditions.failOnText
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.11"

project {

    buildType(RetrievePluginPath)

    template(Template1)

    subProject(TestCodeCoverage)
    subProject(BuildIncrementor)
}

object RetrievePluginPath : BuildType({
    name = "RetrievePluginPath"

    enablePersonalBuilds = false
    maxRunningBuilds = 1

    params {
        param("autoinc.testcounter", "0")
    }

    vcs {
        root(DslContext.settingsRoot)

        showDependenciesChanges = true
    }

    steps {
        script {
            name = "SetCounterParameter"
            id = "SetCounterParameter"
            enabled = false
            scriptContent = """
                echo "##teamcity[setParameter name='autoinc.%teamcity.build.branch%' value='1']"
                echo "%autoinc.%teamcity.build.branch%%"
            """.trimIndent()
        }
        script {
            name = "FetchPluginPath"
            id = "FetchPluginPath"
            enabled = false
            scriptContent = """
                echo "Before Execution"
                echo "build.counter number is:"
                echo %build.counter%
                echo "build.number is:"
                echo %build.number%
                echo "autoinc.testcounter number is:"
                echo %autoinc.testcounter%
                echo "After Execution"
            """.trimIndent()
        }
        script {
            name = "CurlRequestToGetLastBuildFromBranch"
            id = "CurlRequestToGetLastBuildFromBranch"
            enabled = false
            scriptContent = """
                echo %teamcity.build.branch%
                echo "%teamcity.serverUrl%/app/rest/builds?locator=buildType:TestAutoIncrementerPlugin_RetrievePluginPath,branch:testing123"
                curl -u "%system.teamcity.auth.userId%:%system.teamcity.auth.password%" "%teamcity.serverUrl%/app/rest/builds?locator=buildType:TestAutoIncrementerPlugin_RetrievePluginPath,branch:testing123"
            """.trimIndent()
        }
        script {
            name = "GetBuildInfoIntoJsonFormat"
            id = "SaveIntoVariable"
            scriptContent = """
                set _response="sangram"
                echo %teamcity.build.branch%
                echo "%teamcity.serverUrl%/app/rest/builds?locator=buildType:TestAutoIncrementerPlugin_RetrievePluginPath,branch:testing123"
                set CRUMB=${'$'}(curl -u "%system.teamcity.auth.userId%:%system.teamcity.auth.password%" "%teamcity.serverUrl%/app/rest/builds?locator=buildType:TestAutoIncrementerPlugin_RetrievePluginPath,branch:testing123" -H "Accept: application/json")
            """.trimIndent()
        }
    }

    failureConditions {
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.COVERAGE_STATEMENT_PERCENTAGE
            units = BuildFailureOnMetric.MetricUnit.PERCENTS
            comparison = BuildFailureOnMetric.MetricComparison.LESS
            compareTo = build {
                buildRule = lastSuccessful()
            }
        }
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.COVERAGE_METHOD_PERCENTAGE
            units = BuildFailureOnMetric.MetricUnit.PERCENTS
            comparison = BuildFailureOnMetric.MetricComparison.LESS
            compareTo = build {
                buildRule = lastSuccessful()
            }
        }
    }
})

object Template1 : Template({
    name = "Template1"

    enablePersonalBuilds = false
    buildNumberPattern = "${BuildIncrementor_AddBuildCounter.depParamRefs["env.BUILD_NUMBER"]}"
    maxRunningBuilds = 1

    params {
        param("dep.TestAutoIncrementerPlugin_BuildIncrementor_AddBuildCounter.build.number", "10")
    }

    vcs {
        showDependenciesChanges = true
    }

    steps {
        script {
            name = "FetchPluginPath"
            id = "FetchPluginPath"
            scriptContent = """
                echo "Before Execution"
                echo ${BuildIncrementor_AddBuildCounter.depParamRefs["env.BUILD_NUMBER"]}
                echo %build.counter%
                echo %build.number%
                echo %teamcity.serverUrl%/app/plugins/autoincrementer/counter/getAndIncrement
                echo curl -X POST %teamcity.serverUrl%/app/plugins/autoincrementer/counter/getAndIncrement
                echo "After Execution"
            """.trimIndent()
        }
    }

    dependencies {
        snapshot(BuildIncrementor_AddBuildCounter) {
            reuseBuilds = ReuseBuilds.NO
        }
    }
})


object BuildIncrementor : Project({
    name = "BuildIncrementor"

    buildType(BuildIncrementor_AddBuildCounter)
})

object BuildIncrementor_AddBuildCounter : BuildType({
    name = "AddBuildCounter"
})


object TestCodeCoverage : Project({
    name = "TestCodeCoverage"

    buildType(TestCodeCoverage_CheckTestCoverage)
})

object TestCodeCoverage_CheckTestCoverage : BuildType({
    name = "CheckTestCoverage"
    params {
        checkbox("TightenCoverage", "true",
                  checked = "true", unchecked = "false")
    }
    DslContext.addParameters(Pair("MyFlagEnabled", "true"))

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        powerShell {
            name = "check_code"
            id = "check_code"
            scriptMode = script {
                content = """
                    echo "Started Working"
                    echo "%TightenCoverage%"
                    if("%TightenCoverage%" -eq 'false')
                    {
                        DslContext.clearParameter()
                        DslContext.addParameters(Pair("MyFlagEnabled", "false"))
                    }
                    echo %TightenCoverage%
                """.trimIndent()
            }
        }
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 10
            perCheckinTriggering = true
            enableQueueOptimization = false
        }
    }

    failureConditions {
        failOnText {
            enabled = DslContext.getParameter(name = "MyFlagEnabled") == "true"
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "Working"
            failureMessage = "Good job"
            reverse = false
            stopBuildOnFailure = true
        }
    }
})
