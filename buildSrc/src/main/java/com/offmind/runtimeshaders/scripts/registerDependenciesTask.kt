package com.offmind.runtimeshaders.scripts

import org.gradle.api.tasks.TaskContainer

const val CREATE_DEPENDENCY_TASK_NAME = "generateShaderDependencyMap"

fun registerGenerateShaderFunctionsTask(tasks: TaskContainer) {
    tasks.register(CREATE_DEPENDENCY_TASK_NAME) {
        doLast {
            createDependenciesScript(this)
        }
    }
}
