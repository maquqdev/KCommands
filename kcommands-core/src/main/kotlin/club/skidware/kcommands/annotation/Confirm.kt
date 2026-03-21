package club.skidware.kcommands.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Confirm(val message: String = "Are you sure?", val timeoutSeconds: Int = 15)
