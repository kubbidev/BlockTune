package me.kubbidev.blocktune.debug;

import me.kubbidev.blocktune.server.instance.Instance;
import me.kubbidev.blocktune.server.instance.block.TunedBlockHandler;
import me.kubbidev.blocktune.server.util.NamespaceID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PaperWallHandler implements TunedBlockHandler {
    public static final PaperWallHandler INSTANCE = new PaperWallHandler();

    private PaperWallHandler() {
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        Instance instance = placement.getInstance();
        instance.getPlugin().getLogger().info("Placed block: " + placement.getBlock().name());

        instance.getBukkitInstance().spawnParticle(Particle.CLOUD,
                placement.getBlockLocation().x(),
                placement.getBlockLocation().y(),
                placement.getBlockLocation().z(), 25);
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        Instance instance = destroy.getInstance();
        instance.getPlugin().getLogger().info("Destroyed block: " + destroy.getBlock().name());

        instance.getBukkitInstance().spawnParticle(Particle.CLOUD,
                destroy.getBlockLocation().x(),
                destroy.getBlockLocation().y(),
                destroy.getBlockLocation().z(), 25);
    }

    @Override
    public void tick(@NotNull Tick tick) {
        Location corner1 = tick.getBlockLocation();
        Location corner2 = corner1.clone().add(1, 1, 1);

        for (Location location : getHollowCube(corner1, corner2, 0.1)) {
            Instance instance = tick.getInstance();
            instance.getBukkitInstance().spawnParticle(Particle.HAPPY_VILLAGER,
                    location.x(),
                    location.y(),
                    location.z(), 0, 0, 0, 0, 0, null, true);
        }

    }

    public List<Location> getHollowCube(Location corner1, Location corner2, double particleDistance) {
        List<Location> result = new ArrayList<>();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (double x = minX; x <= maxX; x += particleDistance) {
            for (double y = minY; y <= maxY; y += particleDistance) {
                for (double z = minZ; z <= maxZ; z += particleDistance) {
                    int components = 0;
                    if (x == minX || x == maxX) components++;
                    if (y == minY || y == maxY) components++;
                    if (z == minZ || z == maxZ) components++;
                    if (components >= 2) {
                        result.add(new Location(corner1.getWorld(), x, y, z));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean isTickable() {
        return true;
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NamespaceID.from("blocktune:paper_wall");
    }
}
