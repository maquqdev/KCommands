package club.skidware.kcommands.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Flag(val name: String, val short: Char = ' ', val description: String = "")
