package net.blueva.arcade.modules.chairs.setup;

import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import net.blueva.arcade.modules.chairs.ChairsModule;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ChairsSetup implements GameSetupHandler {

    private final ChairsModule module;

    public ChairsSetup(ChairsModule module) {
        this.module = module;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        String subcommand = context.getArg(context.getStartIndex() - 1).toLowerCase(Locale.ENGLISH);

        return switch (subcommand) {
            case "musictime" -> handleTime(context, "basic.initial_music_time",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_music_time"));
            case "sittime" -> handleTime(context, "basic.initial_sit_time",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_sit_time"));
            case "musicreduction" -> handleTime(context, "basic.music_time_reduction",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_music_reduction"));
            case "sitreduction" -> handleTime(context, "basic.sit_time_reduction",
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_sit_reduction"));
            default -> {
                context.getMessagesAPI().sendRaw(context.getPlayer(),
                        module.getCoreConfig().getLanguage("admin_commands.errors.unknown_subcommand"));
                yield true;
            }
        };
    }

    private boolean handleTime(SetupContext<Player, CommandSender, Location> context, String path, String usage) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), usage);
            return true;
        }

        try {
            double value = Double.parseDouble(context.getHandlerArg(0));
            if (value <= 0D) {
                context.getMessagesAPI().sendRaw(context.getPlayer(), usage);
                return true;
            }

            context.getData().setDouble(path, value);
            context.getData().save();
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.time_updated")
                            .replace("{key}", path)
                            .replace("{value}", context.getHandlerArg(0)));
        } catch (NumberFormatException e) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), usage);
        }

        return true;
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        int relIndex = context.getRelativeArgIndex();
        if (relIndex == 0) {
            return TabCompleteResult.of("<seconds>");
        }
        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return Arrays.asList("musictime", "sittime", "musicreduction", "sitreduction");
    }

    @SuppressWarnings("unchecked")
    private SetupContext<Player, CommandSender, Location> castSetupContext(SetupContext context) {
        return (SetupContext<Player, CommandSender, Location>) context;
    }

    @SuppressWarnings("unchecked")
    private TabCompleteContext<Player, CommandSender> castTabContext(TabCompleteContext context) {
        return (TabCompleteContext<Player, CommandSender>) context;
    }
}
