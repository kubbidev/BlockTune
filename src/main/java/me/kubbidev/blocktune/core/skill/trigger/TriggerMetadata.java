package me.kubbidev.blocktune.core.skill.trigger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.blocktune.core.damage.AttackMetadata;
import me.kubbidev.blocktune.core.entity.EntityMetadata;
import me.kubbidev.blocktune.core.event.attack.EntityAttackEvent;
import me.kubbidev.blocktune.core.skill.Skill;
import me.kubbidev.blocktune.core.skill.SkillMetadata;
import me.kubbidev.blocktune.core.util.EquipmentSlot;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class TriggerMetadata {

    @Getter
    private final LivingEntity caster;

    @Getter
    private final EquipmentSlot actionHand;

    @Getter
    private final Location source;

    @Getter
    @Nullable
    private final Entity target;

    @Getter
    @Nullable
    private final Location targetLocation;

    @Getter
    @Nullable
    private final AttackMetadata attack;

    /**
     * The instantiation of an EntityMetadata can be quite intensive in computation,
     * especially because it can be up to 20 times a second for every player in the server.
     * <p>
     * For this reason, it's best to NOT generate the EntityMetadata unless it has been
     * provided beforehand in the constructor, until it's finally asked for in the getter.
     */
    @Nullable
    private EntityMetadata cachedMetadata;

    public TriggerMetadata(LivingEntity entity) {
        this(entity, (LivingEntity) null);
    }

    public TriggerMetadata(LivingEntity entity, @Nullable LivingEntity target) {
        this(entity, EquipmentSlot.MAIN_HAND, entity.getLocation(), target, null, null);
    }

    public TriggerMetadata(LivingEntity entity, @Nullable Location targetLocation) {
        this(entity, EquipmentSlot.MAIN_HAND, entity.getLocation(), null, targetLocation, null);
    }

    public TriggerMetadata(LivingEntity entity, Location source, @Nullable Location targetLocation) {
        this(entity, EquipmentSlot.MAIN_HAND, source, null, targetLocation, null);
    }

    /**
     * The entity responsible for the attack is the one triggering the skill.
     */
    public TriggerMetadata(EntityAttackEvent event) {
        this(event.getAttacker(), event.getEntity(), event.getAttack());
    }

    public TriggerMetadata(EntityMetadata caster, @Nullable Entity target, @Nullable AttackMetadata attack) {
        this(caster.entity(), caster.actionHand(), caster.entity().getLocation(), target, null, attack);
    }

    public EntityMetadata getCachedMetadata(BlockTune plugin) {
        if (this.cachedMetadata == null) {
            this.cachedMetadata = new EntityMetadata(plugin, this.caster, this.actionHand);
        }
        return this.cachedMetadata;
    }

    public SkillMetadata toSkillMetadata(Skill cast) {
        return new SkillMetadata(cast, getCachedMetadata(cast.getPlugin()), this.source, this.target, this.targetLocation, this.attack);
    }
}