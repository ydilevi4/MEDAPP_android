package com.medapp.data.model

enum class ScheduleType {
    FOOD_SLEEP,
    EVERY_N_HOURS
}

enum class DurationType {
    DAYS,
    PILLS_COUNT,
    COURSES
}

enum class IntakeStatus {
    PLANNED,
    COMPLETED,
    MISSED
}

enum class EqualDistanceRule {
    PREFER_HIGHER,
    PREFER_LOWER
}

enum class Language {
    RU,
    EN
}
