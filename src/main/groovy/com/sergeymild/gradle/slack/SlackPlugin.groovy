package com.sergeymild.gradle.slack

import com.sergeymild.gradle.slack.model.SlackMessageTransformer
import net.gpedro.integrations.slack.SlackApi
import net.gpedro.integrations.slack.SlackMessage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.tasks.TaskState

/**
 * Created by joaoprudencio on 05/05/15.
 */
class SlackPlugin implements Plugin<Project> {

    SlackPluginExtension mExtension
    StringBuilder mTaskLogBuilder

    void apply(Project project) {

        mTaskLogBuilder = new StringBuilder()
        mExtension = project.extensions.create('slack', SlackPluginExtension)
        println("apply(Project project)")
        project.afterEvaluate {
            if (mExtension.url != null && mExtension.enabled)
                monitorTasksLifecyle(project)
        }
    }

    void monitorTasksLifecyle(Project project) {


        project.getGradle().getTaskGraph().addTaskExecutionListener(new TaskExecutionListener() {
            @Override
            void beforeExecute(Task task) {
                task.logging.addStandardOutputListener(new StandardOutputListener() {
                    @Override
                    void onOutput(CharSequence charSequence) {
                        mTaskLogBuilder.append(charSequence)
                    }
                })
            }

            @Override
            void afterExecute(Task task, TaskState state) {
                handleTaskFinished(task, state)
            }
        })
    }

    void handleTaskFinished(Task task, TaskState state) {
        Throwable failure = state.getFailure()
        boolean shouldSendMessage = failure != null || shouldMonitorTask(task);

        // only send a slack message if the task failed
        // or the task is registered to be monitored
        if (shouldSendMessage) {
            SlackMessage slackMessage = SlackMessageTransformer.buildSlackMessage(mExtension.title, task, state, mTaskLogBuilder.toString())
            SlackApi api = new SlackApi(mExtension.url)
            api.call(slackMessage)
        }
    }

    boolean shouldMonitorTask(Task task) {
        for (dependentTask in mExtension.dependsOnTasks) {
            if (task.getName() == dependentTask) {
                return true
            }
        }
        return false
    }
}