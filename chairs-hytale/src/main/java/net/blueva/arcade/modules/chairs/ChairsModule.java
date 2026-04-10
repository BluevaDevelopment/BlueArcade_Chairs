package net.blueva.arcade.modules.chairs;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.achievements.AchievementsAPI;
import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.events.EventSubscription;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameModule;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.ui.VoteMenuAPI;
import net.blueva.arcade.modules.chairs.game.ChairsGame;
import net.blueva.arcade.modules.chairs.listener.ChairsDamageListener;
import net.blueva.arcade.modules.chairs.setup.ChairsSetup;
import net.blueva.arcade.modules.chairs.support.ChairsSettings;
import net.blueva.arcade.modules.chairs.support.ChairsStats;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

import java.util.Map;

public class ChairsModule implements GameModule<Player, Location, World, String, ItemStack, String, Holder, Entity, EventSubscription<?>, Short> {

    private ModuleConfigAPI moduleConfig;
    private CoreConfigAPI coreConfig;
    private ModuleInfo moduleInfo;
    private StatsAPI<Player> statsAPI;
    private ChairsSettings settings;
    private ChairsGame game;
    private boolean systemsRegistered;

    @Override
    public void onLoad() {
        moduleInfo = ModuleAPI.getModuleInfo("chairs");

        if (moduleInfo == null) {
            throw new IllegalStateException("ModuleInfo not available for Chairs module");
        }

        moduleConfig = ModuleAPI.getModuleConfig(moduleInfo.getId());
        coreConfig = ModuleAPI.getCoreConfig();
        statsAPI = ModuleAPI.getStatsAPI();
        VoteMenuAPI<String> voteMenu = ModuleAPI.getVoteMenuAPI();
        AchievementsAPI achievementsAPI = ModuleAPI.getAchievementsAPI();

        moduleConfig.register("language.yml", 1);
        moduleConfig.register("settings.yml", 1);
        moduleConfig.register("achievements.yml", 1);

        settings = new ChairsSettings();
        settings.load(moduleConfig, moduleInfo.getId());

        ChairsStats chairsStats = new ChairsStats(statsAPI, moduleInfo);
        chairsStats.register();

        if (achievementsAPI != null) {
            achievementsAPI.registerModuleAchievements(moduleInfo.getId(), "achievements.yml");
        }

        ModuleAPI.getSetupAPI().registerHandler(moduleInfo.getId(), new ChairsSetup(this));

        if (moduleConfig != null && voteMenu != null) {
            String voteItem = moduleConfig.getString("menus.vote.item");
            voteMenu.registerGame(
                    moduleInfo.getId(),
                    voteItem,
                    moduleConfig.getStringFrom("language.yml", "vote_menu.name"),
                    moduleConfig.getStringListFrom("language.yml", "vote_menu.lore")
            );
        }

        game = new ChairsGame(this);
    }

    @Override
    public void onStart(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        game.onStart(context);
    }

    @Override
    public void onCountdownTick(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                int secondsLeft) {
        game.onCountdownTick(context, secondsLeft);
    }

    @Override
    public void onCountdownFinish(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        game.onCountdownFinish(context);
    }

    @Override
    public boolean freezePlayersOnCountdown() {
        return false;
    }

    @Override
    public void onGameStart(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        game.onGameStart(context);
    }

    @Override
    public void onEnd(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
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
    public void registerEvents(CustomEventRegistry<EventSubscription<?>, Short> registry) {
        if (systemsRegistered) {
            return;
        }
        if (registry instanceof net.blueva.arcade.api.events.hytale.HytaleEventRegistry hytaleRegistry) {
            hytaleRegistry.registerSystem(new ChairsDamageListener(game));
            systemsRegistered = true;
        }
    }

    @Override
    public Map<String, String> getCustomPlaceholders(Player player) {
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

    public StatsAPI<Player> getStatsAPI() {
        return statsAPI;
    }

    public ChairsSettings getSettings() {
        return settings;
    }

    public ChairsGame getGame() {
        return game;
    }

    public GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> getGameContext(Player player) {
        return game.getGameContext(player);
    }

    public void handleRespawnEffects(Player player) {
        game.handleRespawnEffects(player);
    }

    public void handleWin(Player player) {
        game.handleWin(player);
    }

}
