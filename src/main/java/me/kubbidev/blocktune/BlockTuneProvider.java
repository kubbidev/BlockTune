package me.kubbidev.blocktune;

import org.jetbrains.annotations.ApiStatus;

/**
 * Provides static access to the {@link BlockTune} API.
 *
 * <p>Ideally, the ServiceManager for the platform should be used to obtain an
 * instance, however, this provider can be used if this is not viable.</p>
 */
public final class BlockTuneProvider {

    private static BlockTune instance = null;

    /**
     * Gets an instance of the {@link BlockTune} API, throwing {@link IllegalStateException} if the API is not loaded yet.
     *
     * <p>This method will never return null.</p>
     *
     * @return an instance of the BlockTune API
     * @throws IllegalStateException if the API is not loaded yet
     */
    public static BlockTune get() {
        BlockTune instance = BlockTuneProvider.instance;
        if (instance == null) {
            throw new NotLoadedException();
        }
        return instance;
    }

    @ApiStatus.Internal
    static void register(BlockTune instance) {
        BlockTuneProvider.instance = instance;
    }

    @ApiStatus.Internal
    static void unregister() {
        BlockTuneProvider.instance = null;
    }

    @ApiStatus.Internal
    private BlockTuneProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Exception thrown when the API is requested before it has been loaded.
     */
    private static final class NotLoadedException extends IllegalStateException {

        private static final String MESSAGE = """
            The BlockTune API isn't loaded yet!
            This could be because:
              a) the BlockTune plugin is not installed or it failed to enable
              b) the plugin in the stacktrace does not declare a dependency on BlockTune
              c) the plugin in the stacktrace is retrieving the API before the plugin 'enable' phase
                 (call the #get method in onEnable, not the constructor!)
            """;

        NotLoadedException() {
            super(MESSAGE);
        }
    }
}