package com.beust.kobalt.internal

import com.beust.kobalt.Args
import com.beust.kobalt.TaskResult
import com.beust.kobalt.api.ITask
import com.beust.kobalt.api.Project
import com.beust.kobalt.misc.log
import com.google.common.collect.ListMultimap
import com.google.common.collect.TreeMultimap
import java.util.concurrent.Callable

/**
 * Build the projects in parallel.
 *
 * The projects are sorted in topological order and then run by the DynamicGraphExecutor in background threads
 * wherever appropriate. Inside a project, all the tasks are run sequentially.
 */
class ParallelProjectRunner(val tasksByNames: (Project) -> ListMultimap<String, ITask>,
        val dependsOn: TreeMultimap<String, String>,
        val reverseDependsOn: TreeMultimap<String, String>, val runBefore: TreeMultimap<String, String>,
        val runAfter: TreeMultimap<String, String>,
        val alwaysRunAfter: TreeMultimap<String, String>, val args: Args, val pluginInfo: PluginInfo)
            : BaseProjectRunner() {
    override fun runProjects(taskInfos: List<TaskManager.TaskInfo>, projects: List<Project>)
            : TaskManager .RunTargetResult {
        class ProjectTask(val project: Project, val dryRun: Boolean) : Callable<TaskResult2<ProjectTask>> {
            override fun toString() = "[ProjectTask " + project.name + "]"
            override fun hashCode() = project.hashCode()
            override fun equals(other: Any?) : Boolean =
                    if (other is ProjectTask) other.project.name == project.name
                    else false

            override fun call(): TaskResult2<ProjectTask> {
                val tasksByNames = tasksByNames(project)
                val graph = createTaskGraph(project.name, taskInfos, tasksByNames,
                        dependsOn, reverseDependsOn, runBefore, runAfter, alwaysRunAfter,
                        { task: ITask -> task.name },
                        { task: ITask -> task.plugin.accept(project) })
                var lastResult = TaskResult()
                while (graph.freeNodes.any()) {
                    val toProcess = graph.freeNodes
                    toProcess.forEach { node ->
                        val tasks = tasksByNames[node.name]
                        tasks.forEach { task ->
                            log(1, "===== " + project.name + ":" + task.name)
                            val tr = if (dryRun) TaskResult2(true, null, task) else task.call()
                            if (lastResult.success) {
                                lastResult = tr
                            }
                        }
                    }
                    graph.freeNodes.forEach { graph.removeNode(it) }
                }

                return TaskResult2(lastResult.success, lastResult.errorMessage, this)
            }

        }

        val factory = object : IThreadWorkerFactory<ProjectTask> {
            override fun createWorkers(nodes: Collection<ProjectTask>): List<IWorker<ProjectTask>> {
                val result = nodes.map { it ->
                    object: IWorker<ProjectTask> {
                        override val priority: Int
                            get() = 0

                        override fun call(): TaskResult2<ProjectTask> {
                            val tr = it.call()
                            return tr
                        }

                    }
                }
                return result
            }
        }

        val projectGraph = DynamicGraph<ProjectTask>().apply {
            projects.forEach { project ->
                project.dependsOn.forEach {
                    addEdge(ProjectTask(project, args.dryRun), ProjectTask(it, args.dryRun))
                }
            }
        }

        val taskResult = DynamicGraphExecutor(projectGraph, factory, 5).run()

        return TaskManager.RunTargetResult(taskResult, emptyList())
    }
}