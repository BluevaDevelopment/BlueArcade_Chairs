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

        moduleConfig.register("language.yml", 1);
        moduleConfig.register("settings.yml", 1);
        moduleConfig.register("achievements.yml", 1);

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
                    moduleConfig.getStringFrom("language.yml", "vote_menu.name"),
                    moduleConfig.getStringListFrom("language.yml", "vote_menu.lore")
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
}
