package net.blueva.arcade.modules.chairs.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ChairsSettings {

    private double initialMusicTime = 6.0;
    private double initialSitTime = 8.0;
    private double musicTimeReduction = 0.35;
    private double sitTimeReduction = 0.4;
    private double minimumMusicTime = 2.0;
    private double minimumSitTime = 2.0;
    private double spawnHeightOffset = 6.0;
    private int warmupRounds = 2;
    private int warmupMaxPlayers = 0;
    private int seatReduction = 1;
    private int minimumSeats = 1;
    private final List<EntityType> seatTypes = new ArrayList<>();
    private final List<String> musicPlaylist = new ArrayList<>();

    public void load(ModuleConfigAPI config) {
        initialMusicTime = config.getDouble("gameplay.initial_music_time", 6.0);
        initialSitTime = config.getDouble("gameplay.initial_sit_time", 8.0);
        musicTimeReduction = config.getDouble("gameplay.music_time_reduction", 0.35);
        sitTimeReduction = config.getDouble("gameplay.sit_time_reduction", 0.4);
        minimumMusicTime = config.getDouble("gameplay.minimum_music_time", 2.0);
        minimumSitTime = config.getDouble("gameplay.minimum_sit_time", 2.0);
        spawnHeightOffset = config.getDouble("seats.spawn_height_offset", 6.0);

        seatTypes.clear();
        for (String raw : config.getStringList("seats.types")) {
            try {
                seatTypes.add(EntityType.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (seatTypes.isEmpty()) {
            seatTypes.add(EntityType.MINECART);
        }

        warmupRounds = Math.max(0, config.getInt("seats.warmup_rounds", 2));
        warmupMaxPlayers = Math.max(0, config.getInt("seats.warmup_max_players", 0));
        seatReduction = Math.max(1, config.getInt("seats.seat_reduction", 1));
        minimumSeats = Math.max(1, config.getInt("seats.minimum_seats", 1));

        loadMusicPlaylist(config);
    }

    public int resolveSeatCount(int round, int playersAlive) {
        if (playersAlive <= 0) {
            return minimumSeats;
        }

        if (isWarmupRound(round, playersAlive)) {
            return clampSeatCount(playersAlive, playersAlive);
        }

        return clampSeatCount(playersAlive, Math.max(minimumSeats, playersAlive - seatReduction));
    }

    private boolean isWarmupRound(int round, int playersAlive) {
        if (warmupRounds <= 0 || round > warmupRounds) {
            return false;
        }
        return warmupMaxPlayers <= 0 || playersAlive <= warmupMaxPlayers;
    }

    private int clampSeatCount(int playersAlive, int configuredSeats) {
        return Math.max(1, Math.min(playersAlive, configuredSeats));
    }

    public double getInitialMusicTime() { return initialMusicTime; }
    public double getInitialSitTime() { return initialSitTime; }
    public double getMusicTimeReduction() { return musicTimeReduction; }
    public double getSitTimeReduction() { return sitTimeReduction; }
    public double getMinimumMusicTime() { return minimumMusicTime; }
    public double getMinimumSitTime() { return minimumSitTime; }
    public double getSpawnHeightOffset() { return spawnHeightOffset; }
    public List<EntityType> getSeatTypes() { return seatTypes; }
    public List<String> getMusicPlaylist() { return musicPlaylist; }

    private void loadMusicPlaylist(ModuleConfigAPI moduleConfig) {
        musicPlaylist.clear();

        File moduleFolder = moduleConfig.getDataFolder();
        if (moduleFolder == null) {
            return;
        }

        File musicFolder = new File(moduleFolder, "music");
        if (!musicFolder.exists() && !musicFolder.mkdirs()) {
            return;
        }
        if (!musicFolder.isDirectory()) {
            return;
        }

        File pluginDataFolder = resolvePluginDataFolder(moduleFolder);
        if (pluginDataFolder == null) {
            return;
        }

        Path pluginDataPath = pluginDataFolder.toPath();
        try (Stream<Path> paths = Files.walk(musicFolder.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbs"))
                    .forEach(path -> {
                        String relative = pluginDataPath.relativize(path).toString().replace(File.separatorChar, '/');
                        musicPlaylist.add("[NBS] " + relative);
                    });
        } catch (IOException ignored) {
            // Keep playlist empty if folder cannot be read.
        }

    }

    private File resolvePluginDataFolder(File moduleFolder) {
        File modulesFolder = moduleFolder.getParentFile();
        if (modulesFolder == null) {
            return null;
        }
        return modulesFolder.getParentFile();
    }
}
