package club.skidware.kcommands.argument

/**
 * Thrown when a command argument cannot be resolved from the provided input.
 *
 * @param message a human-readable description of the parsing failure
 */
class ArgumentException(message: String) : Exception(message)
