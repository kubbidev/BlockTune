package me.kubbidev.blocktune.core.listener;

import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.damage.DamageType;
import me.kubbidev.blocktune.core.event.attack.AttackEvent;
import me.kubbidev.blocktune.core.event.attack.EntityAttackEvent;
import me.kubbidev.blocktune.core.event.attack.EntityKillEntityEvent;
import me.kubbidev.blocktune.core.manager.DamageManager;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * First problem: you want to create a skill which does something whenever
 * an entity attacks another given entity, but you have to listen to all the attack
 * types and even abilities.
 * <p>
 * We need some event to generalize every attack, which is {@link EntityAttackEvent}
 * <p>
 * An EntityAttackEvent is called whenever an entity attacks, by any way, another entity.
 * <p>
 * Second problem: if an entity shoots another entity, it's not hard to get the
 * damaging entity, the arrow and trace back its shooter.
 * <p>
 * However, if an external plugin damages an entity without telling Spigot that
 * the entity is the damage source, it's impossible to trace back the initial damager.
 * <p>
 * {@link DamageManager} gives a way to let know that some entity damaged some entity.
 * <p>
 * Basically BlockTune is monitoring every single attack from every single entity to keep track:
 * <br>1) of the initial caster,
 * <br>2) of the damage types, that is whether it is a {@link DamageType#SKILL} or {@link DamageType#WEAPON} attack.
 */
public class AttackEventListener implements Listener {

    private final BlockTune plugin;

    public AttackEventListener(BlockTune plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void registerAttacks(EntityDamageEvent e) {
        // ignore fake events...
        if (!(e.getEntity() instanceof LivingEntity living) || this.plugin.getFakeEventManager().isFake(e)) {
            return;
        }

        AttackMetadata attack = this.plugin.getDamageManager().findAttack(e);
        if (attack.hasAttacker()) {
            //noinspection DataFlowIssue
            LivingEntity entity = attack.getAttacker().entity();
            if (entity instanceof Player && ((Player) entity).getGameMode() == GameMode.SPECTATOR) {
                return;
            }
        }

        AttackEvent called = attack.hasAttacker() ? new EntityAttackEvent(e, attack) : new AttackEvent(e, attack);
        if (!called.callEvent()) return;

        e.setDamage(attack.getMetadata().getDamage());

        // call the death event if the entity is being killed
        if (attack.hasAttacker() && e.getFinalDamage() >= living.getHealth()) {
            EntityKillEntityEvent event = new EntityKillEntityEvent(e, attack, living);
            event.callEvent();
        }
    }
}