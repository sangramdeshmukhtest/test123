package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.PowerShellStep
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the buildType with id = 'TestCodeCoverage_CheckTestCoverage'
accordingly, and delete the patch script.
*/
changeBuildType(RelativeId("TestCodeCoverage_CheckTestCoverage")) {
    params {
        remove {
            checkbox("system.coverage", "true", display = ParameterDisplay.PROMPT,
                      checked = "true", unchecked = "false")
        }
        add {
            checkbox("coverage", "true", display = ParameterDisplay.PROMPT,
                      checked = "true", unchecked = "false")
        }
    }

    expectSteps {
        powerShell {
            name = "check_code"
            id = "check_code"
            scriptMode = script {
                content = """
                    echo "Started Working"
                    echo %system.coverage%
                """.trimIndent()
            }
        }
    }
    steps {
        update<PowerShellStep>(0) {
            clearConditions()
            scriptMode = script {
                content = """
                    echo "Started Working"
                    echo %coverage%
                """.trimIndent()
            }
        }
    }

    triggers {
        val trigger1 = find<VcsTrigger> {
            vcs {
                enabled = false
                quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
                quietPeriod = 10
                perCheckinTriggering = true
                enableQueueOptimization = false
            }
        }
        trigger1.apply {
            enabled = true
            quietPeriod = 5
            perCheckinTriggering = false
            enableQueueOptimization = true

        }
    }
}
