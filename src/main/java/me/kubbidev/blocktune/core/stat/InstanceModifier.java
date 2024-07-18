package me.kubbidev.blocktune.core.stat;

import lombok.Getter;
import me.kubbidev.blocktune.config.BlockTuneConfiguration;
import me.kubbidev.blocktune.core.entity.modfier.EntityModifier;
import me.kubbidev.blocktune.core.entity.modfier.ModifierSource;
import me.kubbidev.blocktune.core.entity.modfier.ModifierType;
import me.kubbidev.blocktune.core.util.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Used anywhere where instances similar to {@link org.bukkit.attribute.Attribute}
 * instances are being modified by numerical modifiers.
 */
@Getter
public abstract class InstanceModifier extends EntityModifier {

    protected final double value;
    protected final ModifierType type;

    public InstanceModifier(String key, double value) {
        this(ModifierSource.OTHER, EquipmentSlot.OTHER, key, value, ModifierType.FLAT);
    }

    public InstanceModifier(ModifierSource source, EquipmentSlot slot, String key, double value, ModifierType type) {
        this(UUID.randomUUID(), source, slot, key, value, type);
    }

    public InstanceModifier(UUID uniqueId, ModifierSource source, EquipmentSlot slot, String key, double value, ModifierType type) {
        super(uniqueId, source, slot, key);
        this.value = value;
        this.type = type;
    }

    @NotNull
    public String toString(BlockTuneConfiguration configuration) {
        return configuration.getDecimalFormat().format(this.value) + (this.type == ModifierType.RELATIVE ? '%' : "");
    }
}