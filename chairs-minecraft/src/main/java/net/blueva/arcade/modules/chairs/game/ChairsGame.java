package net.blueva.arcade.modules.chairs.game;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.modules.chairs.ChairsModule;
import net.blueva.arcade.modules.chairs.state.ArenaBounds;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ChairsGame {

    private final ChairsModule module;
    private final Map<Integer, GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity>> contexts = new ConcurrentHashMap<>();
    private final Map<Player, Integer> playerArena = new ConcurrentHashMap<>();
    private final Map<Integer, ArenaBounds> arenaBounds = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> rounds = new ConcurrentHashMap<>();
    private final Map<Integer, Double> musicTimes = new ConcurrentHashMap<>();
    private final Map<Integer, Double> sitTimes = new ConcurrentHashMap<>();
    private final Map<Integer, Double> musicReductions = new ConcurrentHashMap<>();
    private final Map<Integer, Double> sitReductions = new ConcurrentHashMap<>();
    private final Map<Integer, List<Entity>> activeSeats = new ConcurrentHashMap<>();
    private final Map<Integer, Double> currentPhaseTime = new ConcurrentHashMap<>();
    private final Set<Integer> ended = ConcurrentHashMap.newKeySet();

    public ChairsGame(ChairsModule module) {
        this.module = module;
    }

    public void onStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        contexts.put(arenaId, context);
        rounds.put(arenaId, 1);
        musicTimes.put(arenaId, resolveConfiguredDouble(context, "basic.initial_music_time", module.getSettings().getInitialMusicTime()));
        sitTimes.put(arenaId, resolveConfiguredDouble(context, "basic.initial_sit_time", module.getSettings().getInitialSitTime()));
        musicReductions.put(arenaId, resolveConfiguredDouble(context, "basic.music_time_reduction", module.getSettings().getMusicTimeReduction()));
        sitReductions.put(arenaId, resolveConfiguredDouble(context, "basic.sit_time_reduction", module.getSettings().getSitTimeReduction()));
        arenaBounds.put(arenaId, resolveBounds(context));
        currentPhaseTime.put(arenaId, 0D);
        ended.remove(arenaId);

        sendDescription(context);

        for (Player player : context.getPlayers()) {
            playerArena.put(player, arenaId);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    public void onCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            context.getSoundsAPI().play(player, module.getCoreConfig().getSound("sounds.starting_game.countdown"));

            String title = module.getCoreConfig().getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", module.getModuleInfo().getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = module.getCoreConfig().getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", module.getModuleInfo().getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void onCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            String title = module.getCoreConfig().getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", module.getModuleInfo().getName());

            String subtitle = module.getCoreConfig().getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", module.getModuleInfo().getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);
            context.getSoundsAPI().play(player, module.getCoreConfig().getSound("sounds.starting_game.start"));
        }
    }

    public void onGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            context.getScoreboardAPI().showScoreboard(player, getScoreboardPath());
        }
        runRound(context, true);
    }

    private void runRound(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context, boolean first) {
        int arenaId = context.getArenaId();
        if (ended.contains(arenaId)) return;

        if (!first) {
            rounds.computeIfPresent(arenaId, (id, round) -> round + 1);
            double musicReduction = musicReductions.getOrDefault(arenaId, module.getSettings().getMusicTimeReduction());
            double sitReduction = sitReductions.getOrDefault(arenaId, module.getSettings().getSitTimeReduction());
            musicTimes.computeIfPresent(arenaId, (id, value) -> Math.max(module.getSettings().getMinimumMusicTime(), value - musicReduction));
            sitTimes.computeIfPresent(arenaId, (id, value) -> Math.max(module.getSettings().getMinimumSitTime(), value - sitReduction));
        }

        playMusic(context);
        currentPhaseTime.put(arenaId, musicTimes.getOrDefault(arenaId, module.getSettings().getInitialMusicTime()));
        updateDisplays(context, false);

        int musicTicks = Math.max(1, (int) Math.round(musicTimes.getOrDefault(arenaId, module.getSettings().getInitialMusicTime()) * 20.0));
        context.getSchedulerAPI().runLater("chairs_music_" + arenaId, () -> {
            stopMusic(context);
            spawnSeats(context);
            currentPhaseTime.put(arenaId, sitTimes.getOrDefault(arenaId, module.getSettings().getInitialSitTime()));
            updateDisplays(context, true);

            int sitTicks = Math.max(1, (int) Math.round(sitTimes.getOrDefault(arenaId, module.getSettings().getInitialSitTime()) * 20.0));
            context.getSchedulerAPI().runLater("chairs_sit_" + arenaId, () -> {
                eliminateStandingPlayers(context);
                cleanupSeats(arenaId);
                currentPhaseTime.put(arenaId, 0D);

                if (context.getAlivePlayers().size() <= 1) {
                    endGame(context);
                    return;
                }
                runRound(context, false);
            }, sitTicks);
        }, musicTicks);

        startDisplayLoop(context);
    }

    private void startDisplayLoop(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        String taskId = "arena_" + arenaId + "_chairs_display";
        if (context.getSchedulerAPI().isTaskRunning(taskId)) {
            return;
        }

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (ended.contains(arenaId)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            currentPhaseTime.computeIfPresent(arenaId, (id, time) -> Math.max(0D, time - 0.05D));
            boolean seatingPhase = !activeSeats.getOrDefault(arenaId, List.of()).isEmpty();
            updateDisplays(context, seatingPhase);
        }, 0L, 1L);
    }

    private void updateDisplays(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                boolean seatingPhase) {
        int arenaId = context.getArenaId();
        String actionBarTemplate = seatingPhase
                ? module.getModuleConfig().getStringFrom("language.yml", "action_bar.sit")
                : module.getModuleConfig().getStringFrom("language.yml", "action_bar.music");

        String timeText = formatSeconds(currentPhaseTime.getOrDefault(arenaId, 0D));
        String roundText = String.valueOf(rounds.getOrDefault(arenaId, 1));

        for (Player player : context.getPlayers()) {
            String actionBar = actionBarTemplate
                    .replace("{chairs_time}", timeText)
                    .replace("{chairs_round}", roundText);
            context.getMessagesAPI().sendActionBar(player, actionBar);

            Map<String, String> placeholders = getCustomPlaceholders(player);
            context.getScoreboardAPI().update(player, getScoreboardPath(), placeholders);
        }
    }

    private void spawnSeats(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        ArenaBounds bounds = arenaBounds.get(arenaId);
        if (bounds == null || bounds.min().getWorld() == null) return;
        World world = bounds.min().getWorld();

        int alive = context.getAlivePlayers().size();
        int round = rounds.getOrDefault(arenaId, 1);
        int seatCount = module.getSettings().resolveSeatCount(round, alive);
        List<Entity> seats = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double minX = Math.min(bounds.min().getX(), bounds.max().getX());
        double maxX = Math.max(bounds.min().getX(), bounds.max().getX());
        double minZ = Math.min(bounds.min().getZ(), bounds.max().getZ());
        double maxZ = Math.max(bounds.min().getZ(), bounds.max().getZ());
        int minY = Math.min(bounds.min().getBlockY(), bounds.max().getBlockY());
        int maxY = Math.max(bounds.min().getBlockY(), bounds.max().getBlockY());

        for (int i = 0; i < seatCount; i++) {
            double x = random.nextDouble(minX + 0.5, maxX + 0.5);
            double z = random.nextDouble(minZ + 0.5, maxZ + 0.5);
            int groundY = findGroundYInsideBounds(world, (int) Math.floor(x), (int) Math.floor(z), minY, maxY);
            Location spawn = new Location(world, x, groundY + module.getSettings().getSpawnHeightOffset(), z);
            EntityType type = module.getSettings().getSeatTypes().get(random.nextInt(module.getSettings().getSeatTypes().size()));
            Entity seat = world.spawnEntity(spawn, type);
            configureSeat(seat);
            seats.add(seat);
        }

        activeSeats.put(arenaId, seats);
    }

    private int findGroundYInsideBounds(World world, int blockX, int blockZ, int minY, int maxY) {
        int top = Math.min(maxY, world.getMaxHeight() - 1);
        int bottom = Math.max(minY, world.getMinHeight());

        for (int y = top; y >= bottom; y--) {
            if (!world.getBlockAt(blockX, y, blockZ).isPassable()) {
                return y;
            }
        }

        return bottom;
    }

    private void configureSeat(Entity seat) {
        if (seat instanceof Pig pig) {
            pig.setSaddle(true);
        }
        if (seat instanceof AbstractHorse horse) {
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            horse.setTamed(true);
        }
    }

    private void eliminateStandingPlayers(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<Player> alive = new ArrayList<>(context.getAlivePlayers());
        for (Player player : alive) {
            Entity vehicle = player.getVehicle();
            if (vehicle == null || !activeSeats.getOrDefault(context.getArenaId(), List.of()).contains(vehicle)) {
                context.eliminatePlayer(player, module.getModuleConfig().getStringFrom("language.yml", "messages.eliminated"));
                player.setGameMode(GameMode.SPECTATOR);
            } else if (module.getStatsAPI() != null) {
                module.getStatsAPI().addModuleStat(player, module.getModuleInfo().getId(), "rounds_survived", 1);
            }
        }
    }

    private void endGame(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        ended.add(arenaId);
        List<Player> alive = new ArrayList<>(context.getAlivePlayers());
        if (!alive.isEmpty()) {
            Player winner = alive.get(0);
            context.setWinner(winner);
            if (module.getStatsAPI() != null) {
                module.getStatsAPI().addModuleStat(winner, module.getModuleInfo().getId(), "wins", 1);
                module.getStatsAPI().addGlobalStat(winner, "wins", 1);
            }
        }
        context.endGame();
    }

    public void onEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context, GameResult<Player> result) {
        int arenaId = context.getArenaId();
        context.getSchedulerAPI().cancelArenaTasks(arenaId);
        cleanupSeats(arenaId);
        stopMusic(context);

        if (module.getStatsAPI() != null) {
            for (Player player : context.getPlayers()) {
                module.getStatsAPI().addModuleStat(player, module.getModuleInfo().getId(), "games_played", 1);
            }
        }

        contexts.remove(arenaId);
        rounds.remove(arenaId);
        musicTimes.remove(arenaId);
        sitTimes.remove(arenaId);
        musicReductions.remove(arenaId);
        sitReductions.remove(arenaId);
        arenaBounds.remove(arenaId);
        currentPhaseTime.remove(arenaId);
        ended.remove(arenaId);

        for (Player player : context.getPlayers()) {
            playerArena.remove(player);
        }
    }

    public void onDisable() {
        if (!contexts.isEmpty()) {
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> anyContext =
                    contexts.values().iterator().next();
            anyContext.getSchedulerAPI().cancelModuleTasks(module.getModuleInfo().getId());
        }

        for (Integer arenaId : new ArrayList<>(activeSeats.keySet())) {
            cleanupSeats(arenaId);
        }

        contexts.clear();
        playerArena.clear();
        arenaBounds.clear();
        rounds.clear();
        musicTimes.clear();
        sitTimes.clear();
        musicReductions.clear();
        sitReductions.clear();
        currentPhaseTime.clear();
        ended.clear();
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        if (context == null) {
            placeholders.put("chairs_round", "-");
            placeholders.put("chairs_time", "-");
            placeholders.put("alive", "0");
            placeholders.put("spectators", "0");
            return placeholders;
        }

        int arenaId = context.getArenaId();
        placeholders.put("chairs_round", String.valueOf(rounds.getOrDefault(arenaId, 1)));
        placeholders.put("chairs_time", formatSeconds(currentPhaseTime.getOrDefault(arenaId, 0D)));
        placeholders.put("alive", String.valueOf(context.getAlivePlayers().size()));
        placeholders.put("spectators", String.valueOf(context.getSpectators().size()));
        return placeholders;
    }

    private void cleanupSeats(int arenaId) {
        for (Entity seat : activeSeats.getOrDefault(arenaId, List.of())) {
            for (Entity passenger : new ArrayList<>(seat.getPassengers())) {
                seat.removePassenger(passenger);
            }
            seat.remove();
        }
        activeSeats.remove(arenaId);
    }

    private void playMusic(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<String> tracks = module.getSettings().getMusicPlaylist();
        if (tracks.isEmpty()) return;
        String track = tracks.get(ThreadLocalRandom.current().nextInt(tracks.size()));
        for (Player player : context.getPlayers()) {
            context.getSoundsAPI().play(player, track);
        }
    }

    private void stopMusic(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            context.getSoundsAPI().stopMusic(player);
        }
    }

    private ArenaBounds resolveBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        Location min = context.getDataAccess().getArenaLocation("bounds.min");
        Location max = context.getDataAccess().getArenaLocation("bounds.max");
        if (min != null && max != null) return new ArenaBounds(min, max);
        Location spawn = context.getArenaAPI().getRandomSpawn();
        if (spawn == null) return null;
        return new ArenaBounds(spawn.clone().add(-8, -2, -8), spawn.clone().add(8, 8, 8));
    }

    private double resolveConfiguredDouble(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                           String path,
                                           double fallback) {
        Double configured = context.getDataAccess().getGameData(path, Double.class);
        return configured != null && configured > 0D ? configured : fallback;
    }

    private void sendDescription(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<String> description = module.getModuleConfig().getStringListFrom("language.yml", "description.default");
        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    private String formatSeconds(double seconds) {
        return String.format(Locale.ENGLISH, "%.1f", Math.max(0D, seconds));
    }

    private String getScoreboardPath() {
        return "scoreboard.main";
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext(Player player) {
        Integer arenaId = playerArena.get(player);
        return arenaId == null ? null : contexts.get(arenaId);
    }

    public boolean isProtectedEntity(Entity entity) {
        if (entity instanceof Player player) {
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
            return context != null && context.isPlayerPlaying(player);
        }

        for (List<Entity> seats : activeSeats.values()) {
            if (seats.contains(entity)) {
                return true;
            }
        }

        return false;
    }
}
