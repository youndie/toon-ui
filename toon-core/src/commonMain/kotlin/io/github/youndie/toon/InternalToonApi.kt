package io.github.youndie.toon

/**
 * Public only so that this repository's own modules (the frame-cost bench of B-05) can reach it.
 * Not part of the library's API: it changes without notice, and a consumer has to opt in to see it.
 */
@RequiresOptIn(
    message = "Internal to toon-ui; it may change or disappear without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class InternalToonApi
