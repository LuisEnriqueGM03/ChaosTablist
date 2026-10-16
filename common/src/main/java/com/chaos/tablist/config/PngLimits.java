package com.chaos.tablist.config;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Inspect the PNG header before allocating decoded pixels, on both ends of the connection. */
public final class PngLimits {
    public static final int MAX_BYTES = 256 * 1024;
    public static final int MAX_ICONS = 64;
    public static final int MAX_DIMENSION = 1024;
    private static final byte[] SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private PngLimits() {}

    public static boolean validName(String name) {
        return name != null && name.matches("[a-z0-9_-]{1,64}");
    }

    public static void validate(byte[] data) {
        if (data.length < 33 || data.length > MAX_BYTES
                || !Arrays.equals(SIGNATURE, Arrays.copyOf(data, 8))) {
            throw new IllegalArgumentException("Invalid or oversized PNG");
        }
        ByteBuffer header = ByteBuffer.wrap(data);
        if (header.getInt(8) != 13 || header.getInt(12) != 0x49484452) {
            throw new IllegalArgumentException("Missing PNG IHDR");
        }
        int width = header.getInt(16), height = header.getInt(20);
        if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException("PNG dimensions must be between 1 and 1024 pixels");
        }
    }
}
