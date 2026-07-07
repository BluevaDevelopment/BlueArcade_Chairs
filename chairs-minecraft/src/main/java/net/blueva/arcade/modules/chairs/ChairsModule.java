package net.blueva.arcade.modules.chairs;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.achievements.AchievementsAPI;
import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameModule;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.ui.VoteMenuAPI;
import net.blueva.arcade.modules.chairs.game.ChairsGame;
import net.blueva.arcade.modules.chairs.listener.ChairsListener;
import net.blueva.arcade.modules.chairs.setup.ChairsSetup;
import net.blueva.arcade.modules.chairs.support.ChairsSettings;
import net.blueva.arcade.modules.chairs.support.ChairsStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import net.blueva.arcade.api.setup.ModuleSetupCommand;
import net.blueva.arcade.api.setup.ModuleSetupMetadata;
import net.blueva.arcade.api.setup.ModuleSetupStep;
import net.blueva.arcade.api.setup.ModuleSetupStatusCheck;
import java.util.List;

public class ChairsModule implements GameModule<Player, Location, World, Material, ItemStack, Sound, Block, Entity, Listener, EventPriority> {

    private ModuleConfigAPI moduleConfig;
    private CoreConfigAPI coreConfig;
    private ModuleInfo moduleInfo;
    private StatsAPI statsAPI;
    private ChairsSettings settings;
    private ChairsGame game;

    @Override
    public void onLoad() {
        moduleInfo = ModuleAPI.getModuleInfo("chairs");
        moduleConfig = ModuleAPI.getModuleConfig(moduleInfo.getId());
        coreConfig = ModuleAPI.getCoreConfig();
        statsAPI = ModuleAPI.getStatsAPI();

        moduleConfig.register("settings.yml");
        moduleConfig.register("achievements.yml");

        settings = new ChairsSettings();
        settings.load(moduleConfig);

        new ChairsStats(statsAPI, moduleInfo).register();

        AchievementsAPI achievementsAPI = ModuleAPI.getAchievementsAPI();
        if (achievementsAPI != null) {
            achievementsAPI.registerModuleAchievements(moduleInfo.getId(), "achievements.yml");
        }

        ModuleAPI.getSetupAPI().registerHandler(moduleInfo.getId(), new ChairsSetup(this));

        VoteMenuAPI voteMenu = ModuleAPI.getVoteMenuAPI();
        if (voteMenu != null) {
            voteMenu.registerGame(
                    moduleInfo.getId(),
                    Material.valueOf(moduleConfig.getString("menus.vote.item")),
                    moduleConfig.getTranslation(null, "vote_menu.name"),
                    moduleConfig.getTranslationList(null, "vote_menu.lore")
            );
        }

        game = new ChairsGame(this);
    }

    @Override
    public void onStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.onStart(context);
    }

    @Override
    public void onCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                int secondsLeft) {
        game.onCountdownTick(context, secondsLeft);
    }

    @Override
    public void onCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.onCountdownFinish(context);
    }

    @Override
    public boolean freezePlayersOnCountdown() {
        return false;
    }

    @Override
    public void onGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.onGameStart(context);
    }

    @Override
    public void onEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                      GameResult<Player> result) {
        game.onEnd(context, result);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.onDisable();
        }
    }

    @Override
    public void registerEvents(CustomEventRegistry<Listener, EventPriority> registry) {
        registry.register(new ChairsListener(this));
    }

    @Override
    public Map<String, String> getCustomPlaceholders(Player player) {
        if (game == null) {
            return Map.of();
        }
        return game.getCustomPlaceholders(player);
    }

    public ModuleConfigAPI getModuleConfig() {
        return moduleConfig;
    }

    public CoreConfigAPI getCoreConfig() {
        return coreConfig;
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public StatsAPI getStatsAPI() {
        return statsAPI;
    }

    public ChairsSettings getSettings() {
        return settings;
    }

    public ChairsGame getGame() {
        return game;
    }


    @Override
    public boolean requiresSpawnCapacityValidation() {
        return false;
    }

    @Override
    public ModuleSetupMetadata getSetupMetadata() {
        return new ModuleSetupMetadata() {

            @Override
            public List<ModuleSetupStep> getSetupSteps() {
                return List.of(
                        new ModuleSetupStep("musicreduction", true, "Configure Musicreduction", "Configure the module-specific musicreduction setup data.", List.of("/baa game <arena> chairs musicreduction"), "percentage or amount"),
                        new ModuleSetupStep("musictime", true, "Configure Musictime", "Configure the module-specific musictime setup data.", List.of("/baa game <arena> chairs musictime"), "seconds"),
                        new ModuleSetupStep("sitreduction", true, "Configure Sitreduction", "Configure the module-specific sitreduction setup data.", List.of("/baa game <arena> chairs sitreduction"), "percentage or amount"),
                        new ModuleSetupStep("sittime", true, "Configure Sittime", "Configure the module-specific sittime setup data.", List.of("/baa game <arena> chairs sittime"), "seconds")
                );
            }

            @Override
            public List<ModuleSetupCommand> getSetupCommands() {
                return List.of(
                        new ModuleSetupCommand("musicreduction", "/baa game <arena> chairs musicreduction", "Configure musicreduction setup data.", true),
                        new ModuleSetupCommand("musictime", "/baa game <arena> chairs musictime", "Configure musictime setup data.", true),
                        new ModuleSetupCommand("sitreduction", "/baa game <arena> chairs sitreduction", "Configure sitreduction setup data.", true),
                        new ModuleSetupCommand("sittime", "/baa game <arena> chairs sittime", "Configure sittime setup data.", true)
                );
            }

            @Override
            public List<ModuleSetupStatusCheck<?, ?, ?>> getStatusChecks() {
                return List.of(
                        new ModuleSetupStatusCheck<>("musicreduction", true, "Set the music reduction value.", context -> context.getData().getDouble("basic.music_time_reduction", 0.0D) > 0.0D || context.getData().getInt("basic.music_time_reduction", 0) > 0),
                        new ModuleSetupStatusCheck<>("musictime", true, "Set the music time.", context -> context.getData().getDouble("basic.initial_music_time", 0.0D) > 0.0D || context.getData().getInt("basic.initial_music_time", 0) > 0),
                        new ModuleSetupStatusCheck<>("sitreduction", true, "Set the sit reduction value.", context -> context.getData().getDouble("basic.sit_time_reduction", 0.0D) > 0.0D || context.getData().getInt("basic.sit_time_reduction", 0) > 0),
                        new ModuleSetupStatusCheck<>("sittime", true, "Set the sit time.", context -> context.getData().getDouble("basic.initial_sit_time", 0.0D) > 0.0D || context.getData().getInt("basic.initial_sit_time", 0) > 0)
                );
            }
        };
    }

}
