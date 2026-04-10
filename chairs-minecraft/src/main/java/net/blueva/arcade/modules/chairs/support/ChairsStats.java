package net.blueva.arcade.modules.chairs.support;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;


public class ChairsStats {

    private final StatsAPI statsAPI;
    private final ModuleInfo moduleInfo;

    public ChairsStats(StatsAPI statsAPI, ModuleInfo moduleInfo) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
    }

    public void register() {
        if (statsAPI == null || moduleInfo == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", "Games played", "Chairs matches played", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", "Wins", "Chairs wins", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("rounds_survived", "Rounds survived", "Rounds survived in Chairs", StatScope.MODULE));
    }
}
