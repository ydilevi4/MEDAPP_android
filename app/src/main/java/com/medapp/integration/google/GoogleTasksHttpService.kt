package com.medapp.integration.google

import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GoogleTasksHttpService(
    private val client: OkHttpClient,
    private val gson: Gson
) : GoogleTasksService {

    override suspend fun listTaskLists(accessToken: String): List<GoogleTaskList> {
        val request = Request.Builder()
            .url("https://tasks.googleapis.com/tasks/v1/users/@me/lists")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            when {
                response.code == 401 -> throw GoogleAuthException.Unauthorized
                !response.isSuccessful -> throw IOException("Failed to list task lists: HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val parsed = gson.fromJson(body, TaskListResponse::class.java)
            return parsed.items.orEmpty().mapNotNull { item ->
                val id = item.id ?: return@mapNotNull null
                val title = item.title ?: return@mapNotNull null
                GoogleTaskList(id = id, title = title)
            }
        }
    }

    override suspend fun createTaskList(accessToken: String, title: String): String {
        val payload = gson.toJson(CreateTaskListRequest(title = title))
        val request = Request.Builder()
            .url("https://tasks.googleapis.com/tasks/v1/users/@me/lists")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            when {
                response.code == 401 -> throw GoogleAuthException.Unauthorized
                !response.isSuccessful -> throw IOException("Failed to create task list: HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val parsed = gson.fromJson(body, TaskListItem::class.java)
            return parsed.id ?: throw IOException("Task list id is missing in Google response")
        }
    }

    override suspend fun listTasks(
        accessToken: String,
        taskListId: String,
        dueMinUtc: String,
        dueMaxUtc: String
    ): List<GoogleRemoteTask> {
        val url = "https://tasks.googleapis.com/tasks/v1/lists/$taskListId/tasks".toHttpUrl().newBuilder()
            .addQueryParameter("showCompleted", "true")
            .addQueryParameter("showHidden", "true")
            .addQueryParameter("maxResults", "100")
            .addQueryParameter("dueMin", dueMinUtc)
            .addQueryParameter("dueMax", dueMaxUtc)
            .build()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 401 -> throw GoogleAuthException.Unauthorized
                !response.isSuccessful -> throw IOException("Failed to list tasks: HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val parsed = gson.fromJson(body, TasksResponse::class.java)
            return parsed.items.orEmpty().mapNotNull { item ->
                val id = item.id ?: return@mapNotNull null
                GoogleRemoteTask(id, item.notes, item.status, item.updated)
            }
        }
    }

    override suspend fun getTask(accessToken: String, taskListId: String, taskId: String): GoogleRemoteTask? {
        val request = Request.Builder()
            .url("https://tasks.googleapis.com/tasks/v1/lists/$taskListId/tasks/$taskId")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 401 -> throw GoogleAuthException.Unauthorized
                response.code == 404 -> return null
                !response.isSuccessful -> throw IOException("Failed to get task: HTTP ${response.code}")
            }
            val parsed = gson.fromJson(response.body?.string().orEmpty(), TaskItem::class.java)
            val id = parsed.id ?: return null
            return GoogleRemoteTask(id, parsed.notes, parsed.status, parsed.updated)
        }
    }

    override suspend fun insertTask(accessToken: String, taskListId: String, payload: GoogleTaskUpsertPayload): GoogleRemoteTask {
        val request = Request.Builder()
            .url("https://tasks.googleapis.com/tasks/v1/lists/$taskListId/tasks")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        return executeUpsert(request, "insert")
    }

    override suspend fun patchTask(
        accessToken: String,
        taskListId: String,
        taskId: String,
        payload: GoogleTaskUpsertPayload
    ): GoogleRemoteTask {
        val request = Request.Builder()
            .url("https://tasks.googleapis.com/tasks/v1/lists/$taskListId/tasks/$taskId")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .patch(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        return executeUpsert(request, "patch")
    }

    private fun executeUpsert(request: Request, operation: String): GoogleRemoteTask {
        client.newCall(request).execute().use { response ->
            when {
                response.code == 401 -> throw GoogleAuthException.Unauthorized
                !response.isSuccessful -> throw IOException("Failed to $operation task: HTTP ${response.code}")
            }
            val parsed = gson.fromJson(response.body?.string().orEmpty(), TaskItem::class.java)
            val id = parsed.id ?: throw IOException("Task id missing in $operation response")
            return GoogleRemoteTask(id, parsed.notes, parsed.status, parsed.updated)
        }
    }

    private data class TaskListResponse(
        val items: List<TaskListItem>?
    )

    private data class TaskListItem(
        val id: String?,
        val title: String?
    )

    private data class CreateTaskListRequest(
        val title: String
    )

    private data class TasksResponse(
        val items: List<TaskItem>?
    )

    private data class TaskItem(
        val id: String?,
        val notes: String?,
        val status: String?,
        val updated: String?
    )
}
