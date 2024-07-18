package me.kubbidev.blocktune.core.listener;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.skill.Ability;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.SkillPlayer;
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
    private final BlockTune plugin;
    private final Map<UUID, SkillPlayer> connectedPlayers = new ConcurrentHashMap<>();

    public AttackActionListener(BlockTune plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        Bukkit.getOnlinePlayers().forEach(this::playerJoin);
    }

    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::playerQuit);
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
                && e.getMaterial() == Material.STICK
                && e.getHand() == EquipmentSlot.HAND
                // avoid spectators to cast skills (bug when they can click on a block)
                && player.getGameMode() != GameMode.SPECTATOR) {
            e.setCancelled(true);
        } else {
            return;
        }

        if (this.connectedPlayers.containsKey(player.getUniqueId())) {
            SkillPlayer skillPlayer = this.connectedPlayers.get(player.getUniqueId());
            skillPlayer.getActualSkill().ifPresent(skill -> {

                Skill cast = skill.toSkill(this.plugin);
                cast.cast(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void changePerformingAttack(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (e.getItemDrop().getItemStack().getType() == Material.STICK) {
            e.setCancelled(true);
        } else {
            return;
        }

        if (connectedPlayers.containsKey(player.getUniqueId())) {
            SkillPlayer skillPlayer = connectedPlayers.get(player.getUniqueId());

            // safe avoid dividing by zero if empty
            List<Ability> unlockedSkills = skillPlayer.getUnlockedSkills();
            if (unlockedSkills.isEmpty()) {
                return;
            }
            // if it's true, subtracts one from the index and adds the size of the task list to handle negative indices before applying modulo,
            // otherwise, simply increments the index and applies modulo to wrap around the list size if needed.
            skillPlayer.setIndex(player.isSneaking()
                    ? (skillPlayer.getIndex() - 1 + unlockedSkills.size()) % unlockedSkills.size()
                    : (skillPlayer.getIndex() + 1) % unlockedSkills.size());
        }
    }

    private void playerJoin(Player p) {
        if (!this.connectedPlayers.containsKey(p.getUniqueId())) {
            SkillPlayer skillPlayer = new SkillPlayer(this.plugin);
            skillPlayer.onJoin(p);

            this.connectedPlayers.put(p.getUniqueId(), skillPlayer);
        }
    }

    private void playerQuit(Player p) {
        SkillPlayer skillPlayer;
        if ((skillPlayer = this.connectedPlayers.remove(p.getUniqueId())) != null) {
            skillPlayer.onQuit(p);
        }
    }
}