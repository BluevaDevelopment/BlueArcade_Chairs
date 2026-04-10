package net.blueva.arcade.modules.chairs.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.chairs.ChairsModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class ChairsListener implements Listener {

    private final ChairsModule module;

    public ChairsListener(ChairsModule module) {
        this.module = module;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = module.getGame().getContext(player);
        if (context != null && context.isPlayerPlaying(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (module.getGame().isProtectedEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = module.getGame().getContext(player);
        if (context != null && context.isPlayerPlaying(player)) {
            event.setCancelled(true);
        }
    }
}
