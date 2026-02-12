package com.medapp.integration.google

interface GoogleTasksService {
    suspend fun listTaskLists(accessToken: String): List<GoogleTaskList>
    suspend fun createTaskList(accessToken: String, title: String): String
}

data class GoogleTaskList(
    val id: String,
    val title: String
)
