package me.kubbidev.blocktune.core.damage;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains all the information about damage being dealt during a specific attack.
 */
public class DamageMetadata implements Cloneable {
    @Getter
    private final List<DamagePacket> packets = new ArrayList<>();

    /**
     * The first damage packet to be registered inside of this damage
     * metadata. It is usually the most significant (highest value)
     * or at least the base damage on which all modifiers are then
     * applied.
     * <p>
     * This field is a direct reference of an existing element
     * of the collection returned by {@link #getPackets()}.
     * <p>
     * Although not common, it can be null.
     */
    @Getter
    @Nullable
    private final DamagePacket initialPacket;

    @Setter
    @Getter
    private boolean weaponCrit;

    @Setter
    @Getter
    private boolean skillCrit;

    private final Set<Element> elementalCrit = new HashSet<>();

    /**
     * Used to register an attack with NO initial packet.
     */
    public DamageMetadata() {
        this.initialPacket = null;
    }

    public DamageMetadata(@Nullable DamagePacket initialPacket) {
        this.initialPacket = initialPacket;
    }

    /**
     * Used to register an attack.
     *
     * @param damage The attack damage
     * @param types  The attack damage types
     */
    public DamageMetadata(double damage, DamageType... types) {
        this(damage, null, types);
    }

    /**
     * Used to register an attack.
     *
     * @param damage  The attack damage
     * @param element If this is an elemental attack
     * @param types   The attack damage types
     */
    public DamageMetadata(double damage, @Nullable Element element, DamageType... types) {
        this.initialPacket = new DamagePacket(damage, element, types);
        this.packets.add(this.initialPacket);
    }

    public boolean isElementCrit(Element element) {
        return this.elementalCrit.contains(element);
    }

    public void registerElementalCrit(Element element) {
        this.elementalCrit.add(element);
    }

    /**
     * You cannot deal less than 0.01 damage.
     * <p>
     * This is an arbitrary positive constant, as BlockTune and other plugins consider
     * 0-damage events to be fake damage events used to check for the PvP/PvE flag.
     */
    public static final double MINIMAL_DAMAGE = 0.01;

    public double getDamage() {
        double d = this.packets.stream().mapToDouble(DamagePacket::getFinalValue).sum();
        return Math.max(MINIMAL_DAMAGE, d);
    }

    /**
     * @param element If null, non-elemental damage will be returned.
     */
    public double getDamage(@Nullable Element element) {
        return this.packets.stream().filter(packet -> packet.isElement(element)).mapToDouble(DamagePacket::getFinalValue).sum();
    }

    public double getDamage(DamageType type) {
        return this.packets.stream().filter(packet -> packet.hasType(type)).mapToDouble(DamagePacket::getFinalValue).sum();
    }

    @NotNull
    public Map<Element, Double> mapElementalDamage() {
        Map<Element, Double> mapped = new HashMap<>();

        for (DamagePacket packet : this.packets) {
            if (packet.getElement() != null) {
                mapped.put(packet.getElement(), mapped.getOrDefault(packet.getElement(), 0d) + packet.getFinalValue());
            }
        }
        return mapped;
    }

    @NotNull
    public Set<DamageType> collectTypes() {
        return this.packets.stream().flatMap(packet -> Arrays.stream(packet.getTypes())).collect(Collectors.toSet());
    }

    @NotNull
    public Set<Element> collectElements() {
        return this.packets.stream().map(DamagePacket::getElement).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public boolean hasType(@NotNull DamageType type) {
        return this.packets.stream().anyMatch(packet -> packet.hasType(type));
    }

    /**
     * @param element If null, will return true if it has non-elemental damage.
     * @return Iterates through all registered damage packets and
     * see if any has this element.
     */
    public boolean hasElement(@Nullable Element element) {
        return this.packets.stream().anyMatch(packet -> packet.isElement(element));
    }

    /**
     * Registers a new damage packet.
     *
     * @param value Damage dealt by another source, this could be an on-hit
     *              skill increasing the damage of the current attack.
     * @param types The damage types of the packet being registered
     * @return The same modified damage metadata
     */
    public DamageMetadata add(double value, DamageType... types) {
        this.packets.add(new DamagePacket(value, types));
        return this;
    }

    /**
     * Registers a new elemental damage packet.
     *
     * @param value   Damage dealt by another source, this could be an on-hit
     *                skill increasing the damage of the current attack.
     * @param element The element
     * @param types   The damage types of the packet being registered
     * @return The same modified damage metadata
     */
    public DamageMetadata add(double value, @Nullable Element element, DamageType... types) {
        this.packets.add(new DamagePacket(value, element, types));
        return this;
    }

    /**
     * Register a multiplicative damage modifier in all damage packets.
     * <p>
     * This is used for critical strikes which modifier should
     * NOT stack up with damage boosting statistics.
     *
     * @param coefficient Multiplicative coefficient. 1.5 will
     *                    increase final damage by 50%
     * @return The same damage metadata
     */
    public DamageMetadata multiplicativeModifier(double coefficient) {
        this.packets.forEach(packet -> packet.multiplicativeModifier(coefficient));
        return this;
    }

    /**
     * Registers a multiplicative damage modifier
     * which applies to any damage packet
     *
     * @param multiplier From 0 to infinity, 1 increases damage by 100%.
     *                   This can be negative as well
     * @return The same damage metadata
     */
    public DamageMetadata additiveModifier(double multiplier) {
        this.packets.forEach(packet -> packet.additiveModifier(multiplier));
        return this;
    }

    /**
     * Register a multiplicative damage modifier for a specific damage type.
     *
     * @param coefficient Multiplicative coefficient. 1.5 will
     *                    increase final damage by 50%
     * @param damageType  Specific damage type
     * @return The same damage metadata
     */
    public DamageMetadata multiplicativeModifier(double coefficient, DamageType damageType) {
        this.packets.stream().filter(packet -> packet.hasType(damageType)).forEach(packet -> packet.multiplicativeModifier(coefficient));
        return this;
    }

    /**
     * Register a multiplicative damage modifier for a specific element.
     *
     * @param coefficient Multiplicative coefficient. 1.5 will
     *                    increase final damage by 50%
     * @param element     If null, non-elemental damage will be considered
     * @return The same damage metadata
     */
    public DamageMetadata multiplicativeModifier(double coefficient, @Nullable Element element) {
        this.packets.stream().filter(packet -> packet.isElement(element)).forEach(packet -> packet.multiplicativeModifier(coefficient));
        return this;
    }

    /**
     * Registers a multiplicative damage modifier which only
     * applies to a specific damage type
     *
     * @param multiplier From 0 to infinity, 1 increases damage by 100%.
     *                   This can be negative as well
     * @param damageType Specific damage type
     * @return The same damage metadata
     */
    public DamageMetadata additiveModifier(double multiplier, DamageType damageType) {
        this.packets.stream().filter(packet -> packet.hasType(damageType)).forEach(packet -> packet.additiveModifier(multiplier));
        return this;
    }

    /**
     * Register an additive damage modifier for a specific element.
     *
     * @param coefficient Multiplicative coefficient. 1.5 will
     *                    increase final damage by 50%
     * @param element     If null, non-elemental damage will be considered
     * @return The same damage metadata
     */
    public DamageMetadata additiveModifier(double coefficient, Element element) {
        this.packets.stream().filter(packet -> packet.isElement(element)).forEach(packet -> packet.additiveModifier(coefficient));
        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public DamageMetadata clone() {
        DamageMetadata clone = new DamageMetadata(this.initialPacket);
        for (DamagePacket packet : this.packets) {
            clone.packets.add(packet.clone());
        }
        return clone;
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Override
    public String toString() {
        StringBuilder damageTypes = new StringBuilder("\u00a73DamageMetadata(");

        boolean packetAppended = false;
        for (DamagePacket packet : this.packets) {
            if (packetAppended) {
                damageTypes.append("\u00a73;");
            }
            packetAppended = true;
            damageTypes.append(packet);
        }
        return damageTypes.append("\u00a73)").toString();
    }
}