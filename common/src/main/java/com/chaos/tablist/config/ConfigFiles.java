package com.chaos.tablist.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Bounded reads and replace-on-success writes, independent of the game lifecycle. */
public final class ConfigFiles {
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    private ConfigFiles() {}

    public static String read(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            byte[] data = in.readNBytes(MAX_BYTES + 1);
            if (data.length > MAX_BYTES) throw new IOException("Configuration exceeds 4 MiB");
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    public static void write(Path file, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_BYTES) throw new IOException("Configuration exceeds 4 MiB");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "tablist-", ".tmp");
        try {
            try (var channel = java.nio.channels.FileChannel.open(temporary,
                    java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                var buffer = java.nio.ByteBuffer.wrap(data);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            if (Files.exists(file)) {
                // Keep the previous file, including invalid manual edits, for recovery.
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
