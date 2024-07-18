package me.kubbidev.blocktune.core.event.attack;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.kubbidev.blocktune.core.entity.EntityMetadata;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * An attack that is called by an entity.
 */
@Getter
public class EntityAttackEvent extends AttackEvent implements Cancellable {
    private final EntityMetadata attacker;

    /**
     * Called whenever an entity deals damage to another entity.
     *
     * @param event  The corresponding damage event.
     * @param attack The generated attack result which can be edited.
     */
    public EntityAttackEvent(EntityDamageEvent event, AttackMetadata attack) {
        super(event, attack);

        Preconditions.checkArgument(attack.hasAttacker(), "Attack was not performed by an entity");
        this.attacker = attack.getAttacker();
    }

}