package club.skidware.kcommands.example

import club.skidware.kcommands.KCommands
import club.skidware.kcommands.example.commands.*
import club.skidware.kcommands.example.messages.ConfigMessageProvider
import club.skidware.kcommands.example.middleware.AuditMiddleware
import club.skidware.kcommands.example.resolvers.Kit
import club.skidware.kcommands.example.resolvers.KitResolver
import club.skidware.kcommands.example.resolvers.Rank
import club.skidware.kcommands.example.resolvers.RankResolver
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class ExamplePlugin : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()

        val messageProvider = ConfigMessageProvider(config)

        KCommands.setup(this) {
            messageProvider(messageProvider)
            logging(true)
            brigadier(true)
            resolver(Kit::class, KitResolver())
            resolver(Rank::class, RankResolver())
            middleware(AuditMiddleware(logger))
        }

        // Annotation-based commands
        KCommands.register(this,
            // Basic commands
            HealCommand(),
            FeedCommand(),
            GameModeCommand(),
            // Arguments and resolvers
            GiveCommand(),
            TeleportCommand(),
            MessageCommand(),
            // Subcommands
            AdminCommand(),
            KitCommand(),
            // Flags
            BanCommand(),
            // Restrictions
            SpawnCommand(),
            ConsoleInfoCommand(),
            // Cooldown and confirm
            WipeCommand(),
            // Async
            LookupCommand(),
            // Range
            SetSpeedCommand(),
            // Custom resolvers
            RankCommand(),
            // Nested subcommands
            ServerCommand(),
            // Overloaded handlers
            InfoCommand()
        )

        // DSL-based commands
        registerDslCommands()

        logger.info("KCommandsExample enabled! All example commands registered.")
    }

    private fun registerDslCommands() {
        KCommands.register(this) {
            command("ping") {
                description("Check server latency")
                default {
                    execute { sender, _ ->
                        sender.sendMessage("Pong!")
                    }
                }
            }

            command("broadcast") {
                permission("example.broadcast")
                description("Broadcast a message")
                default {
                    arg<String>("message") {
                        greedy = true
                    }
                    execute { sender, args ->
                        val message = args["message"] as String
                        @Suppress("DEPRECATION")
                        Bukkit.broadcastMessage("[Broadcast] $message")
                    }
                }
            }
        }
    }

    override fun onDisable() {
        KCommands.unregisterAll()
        logger.info("KCommandsExample disabled.")
    }
}
