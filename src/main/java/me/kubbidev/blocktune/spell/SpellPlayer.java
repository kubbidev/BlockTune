package me.kubbidev.blocktune.spell;

import com.google.common.collect.ImmutableList;
import me.kubbidev.blocktune.BlockTune;
import me.kubbidev.nexuspowered.cooldown.CooldownMap;
import me.kubbidev.spellcaster.entity.EntityMetadataProvider;
import me.kubbidev.spellcaster.spell.handler.SpellHandler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@ApiStatus.Experimental
public final class SpellPlayer extends BukkitRunnable {
    private final BlockTune plugin;

    @Nullable
    private BossBar bossBar;

    @Nullable
    private Player player;

    private final List<Ability> unlockedAbilities = ImmutableList.<Ability>builder()
            .add(Ability.DANCE)
            .add(Ability.CLEAR_BLUE_SKY)
            .add(Ability.RAGING_SUN)
            .add(Ability.BURNING_BONES_SUMMER_SUN)
            .add(Ability.SETTING_SUN_TRANSFORMATION)
            .add(Ability.SOLAR_HEAT_HAZE)
            .add(Ability.BENEFICENT_RADIANCE)
            .add(Ability.SUNFLOWER_THRUST)
            .add(Ability.SUN_HALO_DRAGON_HEAD_DANCE)
            .add(Ability.FIRE_WHEEL)
            .build();

    private int index = 0;

    public SpellPlayer(BlockTune plugin) {
        this.plugin = plugin;
    }

    public @NotNull List<Ability> getUnlockedAbilities() {
        return this.unlockedAbilities;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void onJoin(Player p) {
        // update player reference
        this.player = p;

        this.bossBar = BossBar.bossBar(Component.empty(), BossBar.MIN_PROGRESS, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        this.bossBar.addViewer(p);
        // run the boss bar title animated task
        runTaskTimerAsynchronously(this.plugin, 0, 1);
    }

    public void onQuit(Player p) {
        Objects.requireNonNull(this.bossBar, "bossBar");
        cancel();

        this.bossBar.removeViewer(p);
        this.bossBar = null;
        // update player reference
        this.player = null;
    }

    @NotNull
    public Optional<Ability> getActualAbility() {
        try {
            return Optional.ofNullable(this.unlockedAbilities.get(this.index));
        } catch (IndexOutOfBoundsException e) {
            // ignore
        }
        return Optional.empty();
    }

    @Override
    public void run() {
        Objects.requireNonNull(this.bossBar, "bossBar");
        Objects.requireNonNull(this.player, "player");

        getActualAbility().ifPresentOrElse(ability -> {
            SpellHandler<?> handler = ability.getHandler();
            // boss bar progress based on cooldown duration
            // for the actual selected spell
            AtomicReference<Float> progress = new AtomicReference<>(BossBar.MAX_PROGRESS);

            CooldownMap<SpellHandler<?>> cooldownMap = EntityMetadataProvider.retrieveCooldown(this.player);
            cooldownMap.get(handler).ifPresent(cooldown -> {
                float percentage = ((float) cooldown.remainingMillis() / cooldown.getTimeout());
                float difference = BossBar.MAX_PROGRESS - percentage;

                progress.set(difference);
            });

            this.bossBar.progress(progress.get());
            this.bossBar.name(Component.text(handler.getId()));
        }, () -> {
            this.bossBar.progress(BossBar.MAX_PROGRESS);
            this.bossBar.name(Component.text("Unknown"));
        });
    }
}