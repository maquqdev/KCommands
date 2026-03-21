package club.skidware.kcommands.error

/**
 * Formats [CommandDefinitionError] lists into a human-readable, boxed console report
 * for display in the server log when command registration fails.
 */
object CommandErrorFormatter {

    /**
     * Formats a list of validation errors into a structured, multi-line report string.
     *
     * @param errors the validation errors to format
     * @return a formatted string suitable for logging
     */
    fun format(errors: List<CommandDefinitionError>): String {
        val sb = StringBuilder()

        sb.appendLine()
        sb.appendLine("┌─────────────────────────────────────────────────────────────┐")
        sb.appendLine("│          KCommands - Command Registration Failed            │")
        sb.appendLine("└─────────────────────────────────────────────────────────────┘")
        sb.appendLine()

        val grouped = errors.groupBy { it.commandClass }

        for ((className, classErrors) in grouped) {
            sb.appendLine("  ● $className")
            sb.appendLine()

            for ((index, error) in classErrors.withIndex()) {
                val location = buildString {
                    append(error.commandClass)
                    if (error.method != null) append("::${error.method}()")
                    if (error.parameter != null) append(" → param '${error.parameter}'")
                }

                sb.appendLine("    ${index + 1}) $location")
                sb.appendLine("       ✗ ${error.message}")
                sb.appendLine("       → ${error.suggestion}")

                if (error.codeHint != null) {
                    sb.appendLine()
                    sb.appendLine("       Fix:")
                    error.codeHint!!.lines().forEach { line ->
                        sb.appendLine("       │ $line")
                    }
                }
                sb.appendLine()
            }
        }

        sb.appendLine("  ══════════════════════════════════════════")
        sb.appendLine("  ${errors.size} error(s) - fix them and restart the server.")
        sb.appendLine()

        return sb.toString()
    }
}
