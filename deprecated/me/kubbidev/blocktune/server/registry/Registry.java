package me.kubbidev.blocktune.server.registry;

import com.google.gson.ToNumberPolicy;
import com.google.gson.stream.JsonReader;
import me.kubbidev.blocktune.server.util.NamespaceID;
import me.kubbidev.blocktune.server.util.collection.ObjectArray;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles registry data, used by {@link StaticProtocolObject} implementations and is strictly internal.
 * Use at your own risk.
 */
public final class Registry {
    @ApiStatus.Internal
    public static BlockEntry block(String namespace, @NotNull Properties main) {
        return new BlockEntry(namespace, main, null);
    }

    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    public static Map<String, Map<String, Object>> load(Resource resource) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        try (InputStream resourceStream = Registry.class.getClassLoader().getResourceAsStream(resource.name)) {
            Objects.requireNonNull(resourceStream, "Resource " + resource + " does not exist!");

            try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream))) {
                reader.beginObject();
                while (reader.hasNext()) {
                    map.put(reader.nextName(), (Map<String, Object>) readObject(reader));
                }
                reader.endObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    @ApiStatus.Internal
    public static <T extends StaticProtocolObject> Container<T> createStaticContainer(Resource resource, Container.Loader<T> loader) {
        Map<String, Map<String, Object>> entries = Registry.load(resource);
        Map<String, T> namespaces = new HashMap<>(entries.size());

        ObjectArray<T> ids = ObjectArray.singleThread(entries.size());
        for (Map.Entry<String, Map<String, Object>> entry : entries.entrySet()) {
            String namespace = entry.getKey();

            Properties properties = Properties.fromMap(entry.getValue());
            T value = loader.get(namespace, properties);

            ids.set(value.id(), value);
            namespaces.put(value.name(), value);
        }
        return new Container<>(resource, namespaces, ids);
    }

    @ApiStatus.Internal
    public record Container<T extends StaticProtocolObject>(Resource resource,
                                                            Map<String, T> namespaces,
                                                            ObjectArray<T> ids) {
        public Container {
            namespaces = Map.copyOf(namespaces);
            ids.trim();
        }

        public T get(@NotNull String namespace) {
            return this.namespaces.get(namespace);
        }

        public T getSafe(@NotNull String namespace) {
            return get(namespace.contains(":") ? namespace : "minecraft:" + namespace);
        }

        public T getId(int id) {
            return this.ids.get(id);
        }

        public int toId(@NotNull String namespace) {
            return get(namespace).id();
        }

        public Collection<T> values() {
            return this.namespaces.values();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Container<?> that)) {
                return false;
            }
            return this.resource == that.resource;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.resource);
        }

        public interface Loader<T extends ProtocolObject> {
            T get(String namespace, Properties properties);
        }
    }

    @ApiStatus.Internal
    public enum Resource {
        BLOCKS("deprecated/blocks.json");

        private final String name;

        Resource(String name) {
            this.name = name;
        }
    }

    public static final class BlockEntry implements Entry {
        private final NamespaceID namespace;
        private final int id;
        private final int stateId;
        private final String translationKey;
        private final Properties custom;

        private BlockEntry(String namespace, Properties main, Properties custom) {
            this.namespace = NamespaceID.from(namespace);
            this.id = main.getInt("id");
            this.stateId = main.getInt("stateId");
            this.translationKey = main.getString("translationKey");
            this.custom = custom;
        }

        public @NotNull NamespaceID namespace() {
            return this.namespace;
        }

        public int id() {
            return this.id;
        }

        public int stateId() {
            return this.stateId;
        }

        public String translationKey() {
            return this.translationKey;
        }

        @Override
        public Properties custom() {
            return this.custom;
        }
    }

    public interface Entry {
        @ApiStatus.Experimental
        Properties custom();
    }

    private static Object readObject(JsonReader reader) throws IOException {
        return switch (reader.peek()) {
            case BEGIN_ARRAY -> {
                List<Object> list = new ArrayList<>();
                reader.beginArray();
                while (reader.hasNext()) {
                    list.add(readObject(reader));
                }
                reader.endArray();
                yield list;
            }
            case BEGIN_OBJECT -> {
                Map<String, Object> map = new HashMap<>();
                reader.beginObject();
                while (reader.hasNext()) {
                    map.put(reader.nextName(), readObject(reader));
                }
                reader.endObject();
                yield map;
            }
            case STRING -> reader.nextString();
            case NUMBER -> ToNumberPolicy.LONG_OR_DOUBLE.readNumber(reader);
            case BOOLEAN -> reader.nextBoolean();
            default -> throw new IllegalStateException("Invalid peek: " + reader.peek());
        };
    }

    record PropertiesMap(Map<String, Object> map) implements Properties {
        @Override
        public String getString(String name, String defaultValue) {
            Object element = element(name);
            return element != null ? (String) element : defaultValue;
        }

        @Override
        public String getString(String name) {
            return element(name);
        }

        @Override
        public double getDouble(String name, double defaultValue) {
            Object element = element(name);
            return element != null ? ((Number) element).doubleValue() : defaultValue;
        }

        @Override
        public double getDouble(String name) {
            return ((Number) element(name)).doubleValue();
        }

        @Override
        public int getInt(String name, int defaultValue) {
            Object element = element(name);
            return element != null ? ((Number) element).intValue() : defaultValue;
        }

        @Override
        public int getInt(String name) {
            return ((Number) element(name)).intValue();
        }

        @Override
        public boolean getBoolean(String name, boolean defaultValue) {
            Object element = element(name);
            return element != null ? (boolean) element : defaultValue;
        }

        @Override
        public boolean getBoolean(String name) {
            return element(name);
        }

        @Override
        public Properties section(String name) {
            Map<String, Object> map = element(name);
            if (map == null) return null;
            return new PropertiesMap(map);
        }

        @Override
        public boolean containsKey(String name) {
            return map.containsKey(name);
        }

        @Override
        public Map<String, Object> asMap() {
            return map;
        }

        @SuppressWarnings("unchecked")
        private <T> T element(String name) {
            return (T) map.get(name);
        }

        @Override
        public String toString() {
            AtomicReference<String> string = new AtomicReference<>("{ ");
            this.map.forEach((s, object) -> string.set(string.get() + " , " + "\"" + s + "\"" + " : " + "\"" + object.toString() + "\""));
            return string.updateAndGet(s -> s.replaceFirst(" , ", "") + "}");
        }

    }

    public interface Properties extends Iterable<Map.Entry<String, Object>> {
        static Properties fromMap(Map<String, Object> map) {
            return new PropertiesMap(map);
        }

        String getString(String name, String defaultValue);

        String getString(String name);

        double getDouble(String name, double defaultValue);

        double getDouble(String name);

        int getInt(String name, int defaultValue);

        int getInt(String name);

        boolean getBoolean(String name, boolean defaultValue);

        boolean getBoolean(String name);

        Properties section(String name);

        boolean containsKey(String name);

        Map<String, Object> asMap();

        @Override
        default @NotNull Iterator<Map.Entry<String, Object>> iterator() {
            return asMap().entrySet().iterator();
        }

        default int size() {
            return asMap().size();
        }
    }
}