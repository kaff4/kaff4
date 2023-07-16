package net.navatwo.kaff4.api

/**
 * Tags an API as internal. Used to exclude internal APIs from `binary-compatibility-validator`.
 *
 * Any API annotated with this annotation is exempt from any compatibility guarantees.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
annotation class InternalApi
