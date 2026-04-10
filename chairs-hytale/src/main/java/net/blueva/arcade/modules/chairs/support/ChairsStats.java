package net.blueva.arcade.modules.chairs.support;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;

public class ChairsStats {

    private final StatsAPI<Player> statsAPI;
    private final ModuleInfo moduleInfo;

    public ChairsStats(StatsAPI<Player> statsAPI, ModuleInfo moduleInfo) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
    }

    public void register() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", "Wins", "Chairs wins", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", "Games Played", "Chairs games played", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("correct_blocks", "Correct blocks", "Correct colors matched", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("powerups_used", "Power-ups", "Power-ups collected", StatScope.MODULE));
    }
}
