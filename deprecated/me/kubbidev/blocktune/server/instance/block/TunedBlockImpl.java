package me.kubbidev.blocktune.server.instance.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import me.kubbidev.blocktune.server.registry.Registry;
import me.kubbidev.blocktune.server.util.ArrayUtil;
import me.kubbidev.blocktune.server.util.block.BlockUtil;
import me.kubbidev.blocktune.server.util.collection.MergedMap;
import me.kubbidev.blocktune.server.util.collection.ObjectArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

record TunedBlockImpl(@NotNull Registry.BlockEntry registry,
                      byte @NotNull [] propertiesArray,
                      @Nullable TunedBlockHandler handler) implements TunedBlock {
    // block state -> block object
    private static final ObjectArray<TunedBlock> BLOCK_STATE_MAP = ObjectArray.singleThread();
    // block id -> valid property keys (order is important for lookup)
    private static final ObjectArray<PropertyType[]> PROPERTIES_TYPE = ObjectArray.singleThread();
    // block id -> Map<PropertiesValues, Block>
    private static final ObjectArray<Map<PropertiesHolder, TunedBlockImpl>> POSSIBLE_STATES = ObjectArray.singleThread();

    @SuppressWarnings("unchecked")
    private static final Registry.Container<TunedBlock> CONTAINER = Registry.createStaticContainer(Registry.Resource.BLOCKS,
            (namespace, properties) -> {
                int blockId = properties.getInt("id");
                Registry.Properties stateObject = properties.section("states");

                // retrieve properties
                PropertyType[] propertyTypes;
                {
                    Registry.Properties stateProperties = properties.section("properties");
                    if (stateProperties != null) {
                        int stateCount = stateProperties.size();
                        propertyTypes = new PropertyType[stateCount];
                        int i = 0;
                        for (Map.Entry<String, Object> entry : stateProperties) {
                            String k = entry.getKey();
                            List<String> v = (List<String>) entry.getValue();
                            propertyTypes[i++] = new PropertyType(k, v);
                        }
                    } else {
                        propertyTypes = new PropertyType[0];
                    }
                }
                PROPERTIES_TYPE.set(blockId, propertyTypes);

                // retrieve block states
                {
                    int propertiesCount = stateObject.size();
                    PropertiesHolder[] propertiesKeys = new PropertiesHolder[propertiesCount];
                    TunedBlockImpl[] blocksValues = new TunedBlockImpl[propertiesCount];
                    int propertiesOffset = 0;
                    for (Map.Entry<String, Object> stateEntry : stateObject) {
                        String query = stateEntry.getKey();

                        Map<String, Object> stateOverride = (Map<String, Object>) stateEntry.getValue();
                        Map<String, String> propertyMap = BlockUtil.parseProperties(query);
                        assert propertyTypes.length == propertyMap.size();
                        byte[] propertiesArray = new byte[propertyTypes.length];
                        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
                            byte keyIndex = findKeyIndex(propertyTypes, entry.getKey(), null);
                            byte valueIndex = findValueIndex(propertyTypes[keyIndex], entry.getValue(), null);
                            propertiesArray[keyIndex] = valueIndex;
                        }

                        Registry.Properties mainProperties = Registry.Properties.fromMap(new MergedMap<>(stateOverride, properties.asMap()));
                        TunedBlockImpl block = new TunedBlockImpl(Registry.block(namespace, mainProperties),
                                propertiesArray, null);
                        BLOCK_STATE_MAP.set(block.stateId(), block);
                        propertiesKeys[propertiesOffset] = new PropertiesHolder(propertiesArray);
                        blocksValues[propertiesOffset++] = block;
                    }
                    POSSIBLE_STATES.set(blockId, ArrayUtil.toMap(propertiesKeys, blocksValues, propertiesOffset));
                }
                // register default state
                int defaultState = properties.getInt("defaultStateId");
                return getState(defaultState);
            });

    static {
        PROPERTIES_TYPE.trim();
        BLOCK_STATE_MAP.trim();
        POSSIBLE_STATES.trim();
    }

    static TunedBlock get(@NotNull String namespace) {
        return CONTAINER.get(namespace);
    }

    static TunedBlock getSafe(@NotNull String namespace) {
        return CONTAINER.getSafe(namespace);
    }

    static TunedBlock getId(int id) {
        return CONTAINER.getId(id);
    }

    static TunedBlock getState(int stateId) {
        return BLOCK_STATE_MAP.get(stateId);
    }

    static Collection<TunedBlock> values() {
        return CONTAINER.values();
    }

    @Override
    public @NotNull TunedBlock withProperty(@NotNull String property, @NotNull String value) {
        PropertyType[] propertyTypes = PROPERTIES_TYPE.get(id());
        assert propertyTypes != null;
        byte keyIndex = findKeyIndex(propertyTypes, property, this);
        byte valueIndex = findValueIndex(propertyTypes[keyIndex], value, this);
        byte[] properties = this.propertiesArray.clone();
        properties[keyIndex] = valueIndex;
        return compute(properties);
    }

    @Override
    public @NotNull TunedBlock withProperties(@NotNull Map<@NotNull String, @NotNull String> properties) {
        if (properties.isEmpty()) {
            return this;
        }
        PropertyType[] propertyTypes = PROPERTIES_TYPE.get(id());
        assert propertyTypes != null;
        byte[] result = this.propertiesArray.clone();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            byte keyIndex = findKeyIndex(propertyTypes, entry.getKey(), this);
            byte valueIndex = findValueIndex(propertyTypes[keyIndex], entry.getValue(), this);
            result[keyIndex] = valueIndex;
        }
        return compute(result);
    }

    @Override
    public @NotNull TunedBlock withHandler(@Nullable TunedBlockHandler handler) {
        return new TunedBlockImpl(this.registry, this.propertiesArray, handler);
    }

    @Override
    public @Unmodifiable @NotNull Map<String, String> properties() {
        PropertyType[] propertyTypes = PROPERTIES_TYPE.get(id());
        assert propertyTypes != null;
        int length = propertyTypes.length;
        if (length == 0) {
            return Map.of();
        }
        String[] keys = new String[length];
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            PropertyType property = propertyTypes[i];
            keys[i] = property.key();
            values[i] = property.values().get(this.propertiesArray[i]);
        }
        return Object2ObjectMaps.unmodifiable(new Object2ObjectArrayMap<>(keys, values, length));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull TunedBlock defaultState() {
        return TunedBlock.fromBlockId(id());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public @NotNull Collection<@NotNull TunedBlock> possibleStates() {
        return (Collection) possibleProperties().values();
    }

    private Map<PropertiesHolder, TunedBlockImpl> possibleProperties() {
        return POSSIBLE_STATES.get(id());
    }

    @Override
    public String toString() {
        return String.format("%s{properties=%s, handler=%s}", name(), properties(), this.handler);
    }

    private TunedBlock compute(byte[] properties) {
        if (Arrays.equals(this.propertiesArray, properties)) {
            return this;
        }
        TunedBlockImpl block = possibleProperties().get(new PropertiesHolder(properties));
        assert block != null;
        return this.handler == null ? block : new TunedBlockImpl(block.registry(), block.propertiesArray, this.handler);
    }

    private static byte findKeyIndex(PropertyType[] properties, String key, TunedBlockImpl block) {
        for (byte i = 0; i < properties.length; i++) {
            if (properties[i].key().equals(key)) return i;
        }
        if (block != null) {
            throw new IllegalArgumentException("Property " + key + " is not valid for block " + block);
        } else {
            throw new IllegalArgumentException("Unknown property key: " + key);
        }
    }

    private static byte findValueIndex(PropertyType propertyType, String value, TunedBlockImpl block) {
        List<String> values = propertyType.values();
        byte index = (byte) values.indexOf(value);
        if (index != -1) {
            return index;
        }
        if (block != null) {
            throw new IllegalArgumentException("Property " + propertyType.key() + " value " + value + " is not valid for block " + block);
        } else {
            throw new IllegalArgumentException("Unknown property value: " + value);
        }
    }

    private record PropertyType(String key, List<String> values) {
    }

    private static final class PropertiesHolder {
        private final byte[] properties;
        private final int hashCode;

        public PropertiesHolder(byte[] properties) {
            this.properties = properties;
            this.hashCode = Arrays.hashCode(properties);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PropertiesHolder other)) {
                return false;
            }
            return Arrays.equals(this.properties, other.properties);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
