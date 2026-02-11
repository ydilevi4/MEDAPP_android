package com.medapp.data.util

object AnchorJsonHelper {
    fun encode(anchors: List<String>): String {
        return anchors.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") { it }
    }

    fun decode(json: String): List<String> {
        if (json.length < 2) return emptyList()
        return json.removePrefix("[").removeSuffix("]")
            .split(',')
            .map { it.trim().removePrefix("\"").removeSuffix("\"") }
            .filter { it.isNotBlank() }
    }
}
