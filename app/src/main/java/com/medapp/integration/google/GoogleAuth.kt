package com.medapp.integration.google

sealed class GoogleAuthException(message: String) : Exception(message) {
    data object Unauthorized : GoogleAuthException("Unauthorized")
}
