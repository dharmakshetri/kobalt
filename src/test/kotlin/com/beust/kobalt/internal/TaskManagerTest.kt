package com.beust.kobalt.internal

import com.beust.kobalt.TestModule
import com.google.common.collect.TreeMultimap
import com.google.inject.Inject
import org.testng.Assert
import org.testng.annotations.Guice
import org.testng.annotations.Test

@Guice(modules = arrayOf(TestModule::class))
class TaskManagerTest @Inject constructor(val taskManager: TaskManager) {

    class DryRunGraphExecutor<T>(val graph: DynamicGraph<T>) {
        fun run() : List<T> {
            val result = arrayListOf<T>()
            var freeNodes = graph.freeNodes
            while (freeNodes.size > 0) {
                val toRemove = arrayListOf<T>()
                graph.freeNodes.forEach {
                    result.add(it)
                    toRemove.add(it)
                }
                toRemove.forEach {
                    graph.removeNode(it)
                }
                freeNodes = graph.freeNodes
            }
            return result
        }
    }

    private fun runTasks(tasks: List<String>) : List<String> {
        val dependsOn = TreeMultimap.create<String, String>().apply {
            put("assemble", "compile")
        }
        val reverseDependsOn = TreeMultimap.create<String, String>().apply {
            put("clean", "copyVersion")
            put("compile", "postCompile")
        }
        val runBefore = TreeMultimap.create<String, String>().apply {
        }
        val runAfter = TreeMultimap.create<String, String>().apply {
            put("compile", "clean")
        }
        val dependencies = TreeMultimap.create<String, String>().apply {
            listOf("assemble", "compile", "clean", "copyVersion", "postCompile").forEach {
                put(it, it)
            }
        }
        val graph = taskManager.createGraph("", tasks, dependencies, dependsOn, reverseDependsOn, runBefore, runAfter,
                { it }, { t -> true })
        val result = DryRunGraphExecutor(graph).run()
        return result
    }

    @Test
    fun graphTest() {
//        KobaltLogger.LOG_LEVEL = 3
        Assert.assertEquals(runTasks(listOf("compile")), listOf("compile", "postCompile"))
        Assert.assertEquals(runTasks(listOf("postCompile")), listOf("postCompile"))
        Assert.assertEquals(runTasks(listOf("compile", "postCompile")), listOf("compile", "postCompile"))
        Assert.assertEquals(runTasks(listOf("clean")), listOf("clean", "copyVersion"))
        Assert.assertEquals(runTasks(listOf("clean", "compile")), listOf("clean", "compile", "copyVersion",
                "postCompile"))
        Assert.assertEquals(runTasks(listOf("assemble")), listOf("compile", "assemble"))
        Assert.assertEquals(runTasks(listOf("clean", "assemble")), listOf("clean", "compile", "assemble",
                "copyVersion"))
    }
}

