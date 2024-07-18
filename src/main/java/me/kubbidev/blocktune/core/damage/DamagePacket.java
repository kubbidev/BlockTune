package me.kubbidev.blocktune.core.damage;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Some damage value weighted by a specific set of damage types.
 * <p>
 * This helps divide any attack into multiple parts that can be manipulated independently.
 * For instance, a melee sword attack would add one {@link DamageType#PHYSICAL} damage packet.
 */
public class DamagePacket implements Cloneable {

    @Setter
    @Getter
    @NotNull
    private DamageType[] types;

    @Getter
    private double value;
    private double additiveModifiers;
    private double multiplicativeModifiers = 1;

    @Setter
    @Getter
    @Nullable
    private Element element;

    public DamagePacket(double value, @NotNull DamageType... types) {
        this(value, null, types);
    }

    public DamagePacket(double value, @Nullable Element element, @NotNull DamageType... types) {
        this.value = value;
        this.types = types;
        this.element = element;
    }

    /**
     * Directly edits the damage packet value.
     *
     * @param value New damage value
     */
    public void setValue(double value) {
        Preconditions.checkArgument(value >= 0, "Value cannot be negative");
        this.value = value;
    }

    /**
     * Register a multiplicative damage modifier.
     * <p>
     * This is used for critical strikes which modifier should
     * NOT stack up with damage boosting statistics.
     *
     * @param coefficient Multiplicative coefficient. 1.5 will
     *                    increase final damage by 50%
     */
    public void multiplicativeModifier(double coefficient) {
        Preconditions.checkArgument(coefficient >= 0, "Coefficient cannot be negative");
        this.multiplicativeModifiers *= coefficient;
    }

    public void additiveModifier(double multiplier) {
        this.additiveModifiers += multiplier;
    }

    /**
     * @return Final value of the damage packet taking into account
     * all the damage modifiers that have been registered
     */
    public double getFinalValue() {
        // make sure the returned value is positive
        return this.value * Math.max(0, 1 + this.additiveModifiers) * this.multiplicativeModifiers;
    }

    public boolean hasType(DamageType type) {
        return Arrays.stream(this.types).anyMatch(checked -> checked == type);
    }

    public boolean isElement(@Nullable Element element) {
        return Objects.equals(this.element, element);
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Override
    public String toString() {
        StringBuilder damageTypes = new StringBuilder();

        // append value and modifier
        damageTypes.append("\u00a7e").append("(").append(this.value)
                .append("*").append(this.additiveModifiers)
                .append("*").append(this.multiplicativeModifiers).append(")").append("x");

        // append Scaling
        boolean damageAppended = false;
        for (DamageType type : types) {
            if (damageAppended) {
                damageTypes.append("\u00a73/");
            }
            damageAppended = true;
            switch (type) {
                case WEAPON -> damageTypes.append("\u00a77");
                case PHYSICAL -> damageTypes.append("\u00a78");
                case PROJECTILE -> damageTypes.append("\u00a7a");
                case MAGIC -> damageTypes.append("\u00a79");
                case SKILL -> damageTypes.append("\u00a7f");
                default -> damageTypes.append("\u00a7c");
            }
            damageTypes.append(type);
            if (this.element != null) {
                damageTypes.append(", element=").append(this.element.name());
            }
        }
        return damageTypes.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public DamagePacket clone() {
        DamagePacket clone = new DamagePacket(this.value, this.types);
        clone.additiveModifiers = this.additiveModifiers;
        clone.multiplicativeModifiers = this.multiplicativeModifiers;
        clone.element = this.element;
        return clone;
    }

}