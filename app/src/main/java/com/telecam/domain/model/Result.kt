package com.telecam.domain.model

/**
 * Sealed class representing the result of an operation.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
}

/**
 * Extension to map Result data.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * Extension to get data or default.
 */
fun <T> Result<T>.getOrDefault(default: T): T {
    return getOrNull() ?: default
}
