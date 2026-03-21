package club.skidware.kcommands.scanner

import club.skidware.kcommands.annotation.*
import club.skidware.kcommands.model.CooldownInfo
import club.skidware.kcommands.model.ConfirmInfo
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * Utility for reading KCommands annotations from Kotlin reflection elements.
 *
 * Provides methods to extract `@Command`, `@SubCommand`, `@Sender`, `@Arg`, `@Flag`,
 * `@Permission`, `@Cooldown`, `@Confirm`, and other annotation values from functions and parameters.
 */
object AnnotationReader {

    fun isDefaultHandler(func: KFunction<*>): Boolean = func.hasAnnotation<DefaultHandler>()

    fun isSubCommand(func: KFunction<*>): Boolean = func.hasAnnotation<SubCommand>()

    fun getSubCommandName(func: KFunction<*>): String? = func.findAnnotation<SubCommand>()?.name

    fun getSubCommandAliases(func: KFunction<*>): List<String> =
        func.findAnnotation<SubCommand>()?.aliases?.toList() ?: emptyList()

    fun getPermission(func: KFunction<*>): String? = func.findAnnotation<Permission>()?.node

    fun getPermissionMessage(func: KFunction<*>): String? =
        func.findAnnotation<Permission>()?.message?.takeIf { it.isNotEmpty() }

    fun getDescription(func: KFunction<*>): String? = func.findAnnotation<Description>()?.value

    fun isAsync(func: KFunction<*>): Boolean = func.hasAnnotation<Async>()

    fun isPlayerOnly(func: KFunction<*>): Boolean = func.hasAnnotation<PlayerOnly>()

    fun isConsoleOnly(func: KFunction<*>): Boolean = func.hasAnnotation<ConsoleOnly>()

    fun getCooldown(func: KFunction<*>): CooldownInfo? {
        val ann = func.findAnnotation<Cooldown>() ?: return null
        return CooldownInfo(ann.seconds, ann.message, ann.bypassPermission)
    }

    fun getConfirm(func: KFunction<*>): ConfirmInfo? {
        val ann = func.findAnnotation<Confirm>() ?: return null
        return ConfirmInfo(ann.message, ann.timeoutSeconds)
    }

    fun isSender(param: KParameter): Boolean = param.findAnnotation<Sender>() != null

    fun getArgName(param: KParameter): String? = param.findAnnotation<Arg>()?.name

    fun isGreedy(param: KParameter): Boolean = param.hasAnnotation<Greedy>()

    fun getRange(param: KParameter): ClosedFloatingPointRange<Double>? {
        val ann = param.findAnnotation<Range>() ?: return null
        return ann.min..ann.max
    }

    fun getComplete(param: KParameter): kotlin.reflect.KClass<*>? =
        param.findAnnotation<Complete>()?.value

    fun getFlag(param: KParameter): Triple<String, Char, String>? {
        val ann = param.findAnnotation<Flag>() ?: return null
        return Triple(ann.name, ann.short, ann.description)
    }

    fun isFlag(param: KParameter): Boolean = param.hasAnnotation<Flag>()
}
