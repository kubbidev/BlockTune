package me.kubbidev.blocktune.core.hologram.factory;

import com.google.common.base.Preconditions;
import me.kubbidev.blocktune.core.hologram.Hologram;
import me.kubbidev.blocktune.core.hologram.HologramFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class BukkitHologramFactory implements HologramFactory {
    public static final BukkitHologramFactory INSTANCE = new BukkitHologramFactory();

    private BukkitHologramFactory() {
    }

    @Override
    public Hologram newHologram(Location location, List<Component> lines) {
        return new BukkitHologram(location, lines);
    }

    private static final class BukkitHologram implements Hologram {

        private Location location;
        private final List<Component> lines = new ArrayList<>();
        private final List<ArmorStand> spawnedEntities = new ArrayList<>();
        private boolean spawned = false;

        BukkitHologram(Location location, List<Component> lines) {
            this.location = Objects.requireNonNull(location, "location");
            updateLines(lines);
        }

        private Location getNewLinePosition() {
            if (this.spawnedEntities.isEmpty()) {
                return this.location;
            } else {
                // get the last entry
                ArmorStand last = this.spawnedEntities.getLast();
                return last.getLocation().add(0, 0.25, 0);
            }
        }

        @Override
        public void spawn() {
            // resize to fit any new lines
            int linesSize = this.lines.size();
            int spawnedSize = this.spawnedEntities.size();

            // remove excess lines
            if (linesSize < spawnedSize) {
                int diff = spawnedSize - linesSize;
                for (int i = 0; i < diff; i++) {

                    // get and remove the last entry
                    ArmorStand as = this.spawnedEntities.removeLast();
                    as.remove();
                }
            }

            // now enough armor stands are spawned, we can now update the text
            for (int i = 0; i < this.lines.size(); i++) {
                Component line = this.lines.get(i);

                if (i >= this.spawnedEntities.size()) {
                    // add a new line
                    Location loc = getNewLinePosition();

                    // ensure the hologram's chunk is loaded.
                    Chunk chunk = loc.getChunk();
                    if (!chunk.isLoaded()) {
                        chunk.load();
                    }

                    // remove any armor stands already at this location. (leftover from a server restart)
                    loc.getWorld().getNearbyEntities(loc, 1, 1, 1).forEach(e -> {
                        if (e.getType() == EntityType.ARMOR_STAND && locationsEqual(e.getLocation(), loc)) {
                            e.remove();
                        }
                    });

                    ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class, a -> {
                        a.setSmall(true);
                        a.setMarker(true);
                        a.setArms(false);
                        a.setBasePlate(false);
                        a.setGravity(false);
                        a.setVisible(false);
                        a.customName(line);
                        a.setCustomNameVisible(true);
                        a.setAI(false);
                        a.setCollidable(false);
                        a.setInvulnerable(true);
                        a.setCanTick(false);
                    });

                    this.spawnedEntities.add(as);
                } else {
                    // update existing line if necessary
                    ArmorStand as = this.spawnedEntities.get(i);

                    if (Objects.equals(as.customName(), line)) {
                        continue;
                    }
                    as.customName(line);
                }
            }

            this.spawned = true;
        }

        @Override
        public void despawn() {
            this.spawnedEntities.forEach(Entity::remove);
            this.spawnedEntities.clear();
            this.spawned = false;
        }

        @Override
        public boolean isSpawned() {
            if (!this.spawned) {
                return false;
            }

            for (ArmorStand stand : this.spawnedEntities) {
                if (!stand.isValid()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public Collection<ArmorStand> getArmorStands() {
            return this.spawnedEntities;
        }

        @Override
        public @Nullable ArmorStand getArmorStand(int line) {
            if (line >= this.spawnedEntities.size()) {
                return null;
            }
            return this.spawnedEntities.get(line);
        }

        @Override
        public void updateLocation(Location location) {
            Objects.requireNonNull(location, "location");

            this.location = location;
            if (!isSpawned()) {
                spawn();
            } else {
                double offset = 0.0;
                for (ArmorStand as : getArmorStands()) {
                    as.teleport(location.clone().add(0.0, offset, 0.0));
                    offset += 0.25;
                }
            }
        }

        @Override
        public void updateLines(List<Component> lines) {
            Objects.requireNonNull(lines, "lines");
            Preconditions.checkArgument(!lines.isEmpty(), "lines cannot be empty");
            for (Component line : lines) {
                Objects.requireNonNull(line, "null line");
            }

            List<Component> ret = new ArrayList<>(lines);
            if (this.lines.equals(ret)) {
                return;
            }

            this.lines.clear();
            this.lines.addAll(ret);
        }

        private static boolean locationsEqual(Location l1, Location l2) {
            return Double.doubleToLongBits(l1.getX()) == Double.doubleToLongBits(l2.getX()) &&
                    Double.doubleToLongBits(l1.getY()) == Double.doubleToLongBits(l2.getY()) &&
                    Double.doubleToLongBits(l1.getZ()) == Double.doubleToLongBits(l2.getZ());
        }
    }
}