package com.sachinkanna.civora.utils

/**
 * Generic class that holds a value with its loading status. Used across Repositories and ViewModels
 * for predictable UI state.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(data: T) : Resource<T>(data)

    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    class Loading<T>(data: T? = null) : Resource<T>(data)
}
