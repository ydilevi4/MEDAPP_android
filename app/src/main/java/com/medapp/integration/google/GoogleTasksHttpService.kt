package com.medapp.integration.google

import com.google.gson.Gson
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
}
