package com.medapp.domain.usecase

import com.medapp.integration.google.GoogleTasksService

class TasksListBootstrapUseCase(
    private val googleTasksService: GoogleTasksService
) {
    suspend operator fun invoke(accessToken: String): String {
        val existing = googleTasksService.listTaskLists(accessToken)
            .firstOrNull { it.title == DEDICATED_LIST_TITLE }
        if (existing != null) return existing.id

        return googleTasksService.createTaskList(accessToken, DEDICATED_LIST_TITLE)
    }

    companion object {
        const val DEDICATED_LIST_TITLE = "Список для приложения My perfect pills tracker"
    }
}
