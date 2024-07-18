package me.kubbidev.blocktune.server.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public class StringUtil {
    private StringUtil() {
    }

    public static int countMatches(@NotNull CharSequence str, char ch) {
        if (str.isEmpty()) {
            return 0;
        }
        int count = 0;
        // We could also call str.toCharArray() for faster look ups but that would generate more garbage.
        for (int i = 0; i < str.length(); i++) {
            if (ch == str.charAt(i)) {
                count++;
            }
        }
        return count;
    }
}