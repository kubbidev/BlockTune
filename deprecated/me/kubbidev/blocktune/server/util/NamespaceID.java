package me.kubbidev.blocktune.server.util;

import com.github.benmanes.caffeine.cache.Cache;
import me.kubbidev.blocktune.util.CaffeineFactory;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a <a href="https://minecraft.wiki/w/Namespaced_ID">Resource location</a>
 */
public final class NamespaceID implements CharSequence, Key {
    private static final Cache<String, NamespaceID> CACHE = CaffeineFactory.newBuilder().weakKeys().weakValues().build();

    @SuppressWarnings("SpellCheckingInspection")
    private static final String legalLetters
            = "[0123456789abcdefghijklmnopqrstuvwxyz_-]+";

    @SuppressWarnings("SpellCheckingInspection")
    private static final String legalPathLetters
            = "[0123456789abcdefghijklmnopqrstuvwxyz./_-]+";

    private final String domain;
    private final String path;
    private final String full;

    @SuppressWarnings("DataFlowIssue")
    public static @NotNull NamespaceID from(@NotNull String namespace) {
        return CACHE.get(namespace, id -> {
            int index = id.indexOf(':');
            String domain;
            String path;
            if (index < 0) {
                domain = "minecraft";
                path = id;
                id = "minecraft:" + id;
            } else {
                domain = id.substring(0, index);
                path = id.substring(index + 1);
            }
            return new NamespaceID(id, domain, path);
        });
    }

    public static @NotNull NamespaceID from(@NotNull String domain, @NotNull String path) {
        return from(domain + ":" + path);
    }

    public static @NotNull NamespaceID from(@NotNull Key key) {
        return from(key.asString());
    }

    private NamespaceID(String full, String domain, String path) {
        this.full = full;
        this.domain = domain;
        this.path = path;
        assert !domain.contains(".") && !domain.contains("/")
                : "Domain cannot contain a dot nor a slash character (" + full + ")";

        assert domain.matches(legalLetters)
                : "Illegal character in domain (" + full + "). Must match " + legalLetters;

        assert path.matches(legalPathLetters)
                : "Illegal character in path (" + full + "). Must match " + legalPathLetters;
    }

    public @NotNull String domain() {
        return this.domain;
    }

    public @NotNull String path() {
        return this.path;
    }

    @Override
    public int length() {
        return this.full.length();
    }

    @Override
    public char charAt(int index) {
        return this.full.charAt(index);
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return this.full.subSequence(start, end);
    }

    @Pattern("[a-z0-9_\\-.]+")
    @Override
    public @NotNull String namespace() {
        return this.domain;
    }

    @Override
    public @NotNull String value() {
        return this.path;
    }

    @Override
    public @NotNull String asString() {
        return this.full;
    }

    @Override
    public @NotNull String toString() {
        return this.full;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NamespaceID other)) {
            return false;
        }
        return Objects.equals(this.domain, other.namespace()) &&
                Objects.equals(this.path, other.value());
    }

    @Override
    public int hashCode() {
        int result = this.domain.hashCode();
        result = (31 * result) + this.path.hashCode();
        return result;
    }
}