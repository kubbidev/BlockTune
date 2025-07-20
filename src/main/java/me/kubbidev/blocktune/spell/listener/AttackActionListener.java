package me.kubbidev.blocktune.spell.listener;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.spell.Ability;
import me.kubbidev.blocktune.spell.SpellPlayer;
import me.kubbidev.spellcaster.spell.Spell;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Experimental
public class AttackActionListener implements Listener {

    private final BlockTune              plugin;
    private final Map<UUID, SpellPlayer> connectedPlayers = new ConcurrentHashMap<>();

    public AttackActionListener(BlockTune plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        Bukkit.getOnlinePlayers().forEach(this::playerJoin);
    }

    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::playerQuit);
    }

    private void playerJoin(Player p) {
        if (!this.connectedPlayers.containsKey(p.getUniqueId())) {
            SpellPlayer spellPlayer = new SpellPlayer(this.plugin);
            spellPlayer.onJoin(p);

            this.connectedPlayers.put(p.getUniqueId(), spellPlayer);
        }
    }

    private void playerQuit(Player p) {
        SpellPlayer spellPlayer;
        if ((spellPlayer = this.connectedPlayers.remove(p.getUniqueId())) != null) {
            spellPlayer.onQuit(p);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        playerJoin(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        playerQuit(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void performAttack(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
            && e.getMaterial() == Material.IRON_SWORD
            && e.getHand() == EquipmentSlot.HAND
            // avoid spectators to cast spells (bug when they can click on a block)
            && player.getGameMode() != GameMode.SPECTATOR) {
            e.setCancelled(true);
        } else {
            return;
        }

        if (this.connectedPlayers.containsKey(player.getUniqueId())) {
            SpellPlayer spellPlayer = this.connectedPlayers.get(player.getUniqueId());
            spellPlayer.getActualAbility().ifPresent(ability -> {

                Spell cast = ability.toSpell();
                cast.cast(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void changePerformingAttack(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (e.getItemDrop().getItemStack().getType() == Material.IRON_SWORD) {
            e.setCancelled(true);
        } else {
            return;
        }

        if (connectedPlayers.containsKey(player.getUniqueId())) {
            SpellPlayer spellPlayer = connectedPlayers.get(player.getUniqueId());

            // safe avoid dividing by zero if empty
            List<Ability> unlockedSpells = spellPlayer.getUnlockedAbilities();
            if (unlockedSpells.isEmpty()) {
                return;
            }
            // if it's true, subtracts one from the index and adds the size of the task list to handle negative indices before applying modulo,
            // otherwise, simply increments the index and applies modulo to wrap around the list size if needed.
            spellPlayer.setIndex(player.isSneaking()
                ? (spellPlayer.getIndex() - 1 + unlockedSpells.size()) % unlockedSpells.size()
                : (spellPlayer.getIndex() + 1) % unlockedSpells.size());
        }
    }
}