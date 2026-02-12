package com.medapp.integration.google

interface GoogleTasksService {
    suspend fun listTaskLists(accessToken: String): List<GoogleTaskList>
    suspend fun createTaskList(accessToken: String, title: String): String
    suspend fun listTasks(
        accessToken: String,
        taskListId: String,
        dueMinUtc: String,
        dueMaxUtc: String
    ): List<GoogleRemoteTask>

    suspend fun getTask(accessToken: String, taskListId: String, taskId: String): GoogleRemoteTask?
    suspend fun insertTask(accessToken: String, taskListId: String, payload: GoogleTaskUpsertPayload): GoogleRemoteTask
    suspend fun patchTask(accessToken: String, taskListId: String, taskId: String, payload: GoogleTaskUpsertPayload): GoogleRemoteTask
}

data class GoogleTaskList(
    val id: String,
    val title: String
)

data class GoogleTaskUpsertPayload(
    val title: String,
    val due: String,
    val notes: String,
    val status: String,
    val completed: String?
)

data class GoogleRemoteTask(
    val id: String,
    val notes: String?,
    val status: String?,
    val updated: String?
)
