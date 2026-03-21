package club.skidware.kcommands.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cooldown(val seconds: Int, val message: String = "", val bypassPermission: String = "")
