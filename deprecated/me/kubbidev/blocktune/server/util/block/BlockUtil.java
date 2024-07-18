package me.kubbidev.blocktune.server.util.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.kubbidev.blocktune.server.util.StringUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Internal
public class BlockUtil {
    private BlockUtil() {
    }

    public static Map<String, String> parseProperties(String query) {
        if (!query.startsWith("[") || !query.endsWith("]")) {
            return Map.of();
        }
        if (query.length() == 2) {
            return Map.of();
        }
        int entries = StringUtil.countMatches(query, ',') + 1;
        assert entries > 0;
        String[] keys = new String[entries];
        String[] values = new String[entries];
        int entryCount = 0;

        int length = query.length() - 1;
        int start = 1;
        int index = 1;
        while (index <= length) {
            if (query.charAt(index) == ',' || index == length) {
                int equalIndex = query.indexOf('=', start);
                if (equalIndex != -1) {
                    String key = query.substring(start, equalIndex).trim();
                    String value = query.substring(equalIndex + 1, index).trim();
                    keys[entryCount] = key;
                    values[entryCount++] = value;
                }
                start = index + 1;
            }
            index++;
        }
        return new Object2ObjectArrayMap<>(keys, values, entryCount);
    }
}