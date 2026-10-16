package com.chaos.tablist.net;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import java.util.function.*;
import com.chaos.tablist.ChaosTablist;
import io.netty.buffer.ByteBuf;

import net.minecraft.resources.ResourceLocation;

public final class TabPayloads {
    public static final int MAX_C2S = 32_000;
    private static final int MAX_S2C = 1_000_000;
    private TabPayloads() {}
    public interface Message { ResourceLocation type(); }
    public record Codec<T>(Function<WireBuffer,T> reader, BiConsumer<WireBuffer,T> writer) {
        public T decode(ByteBuf buffer) { return reader.apply(new WireBuffer(buffer)); }
        public void encode(ByteBuf buffer, T value) { writer.accept(new WireBuffer(buffer),value); }
    }
	public static byte[] gzip(String json) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
			out.write(json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return bytes.toByteArray();
	}

	public static String gunzip(byte[] data) {
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
			// Tope de 4 MB descomprimidos: que nadie mande una bomba de gzip.
			byte[] out = in.readNBytes(4 * 1024 * 1024 + 1);
            if (out.length > 4 * 1024 * 1024) throw new IOException("Decompressed payload exceeds 4 MiB");
			return new String(out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

    public record SyncConfig(byte[] data, long revision) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("sync_config_v2");
        public static final Codec<SyncConfig> CODEC = new Codec<>(b -> new SyncConfig(b.readByteArray(MAX_S2C), b.readVarLong()), (b,p) -> { b.writeByteArray(p.data()); b.writeVarLong(p.revision()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
    public record SyncValues(byte[] data) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("sync_values_v2");
        public static final Codec<SyncValues> CODEC = new Codec<>(b -> new SyncValues(b.readByteArray(MAX_S2C)), (b,p) -> { b.writeByteArray(p.data()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
    public record SelfValues(byte[] data) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("self_values_v2");
        public static final Codec<SelfValues> CODEC = new Codec<>(b -> new SelfValues(b.readByteArray(MAX_S2C)), (b,p) -> { b.writeByteArray(p.data()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
    public record OpenEditor(byte[] data, boolean chaosRanks, long revision) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("open_editor_v2");
        public static final Codec<OpenEditor> CODEC = new Codec<>(b -> new OpenEditor(b.readByteArray(MAX_S2C), b.readBoolean(), b.readVarLong()), (b,p) -> { b.writeByteArray(p.data()); b.writeBoolean(p.chaosRanks()); b.writeVarLong(p.revision()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
    public record IconData(String name, byte[] png) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("icon_data");
        public static final Codec<IconData> CODEC = new Codec<>(b -> new IconData(b.readUtf(64), b.readByteArray(MAX_S2C)), (b,p) -> { b.writeUtf(p.name(),64); b.writeByteArray(p.png()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
    public record SaveConfig(byte[] data, long revision, long requestId) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("save_config_v2");
        public static final Codec<SaveConfig> CODEC = new Codec<>(b -> new SaveConfig(b.readByteArray(MAX_C2S), b.readVarLong(), b.readVarLong()), (b,p) -> { b.writeByteArray(p.data()); b.writeVarLong(p.revision()); b.writeVarLong(p.requestId()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
    public record SaveResult(long requestId, boolean success, String reason, long revision) implements Message {
        public static final ResourceLocation TYPE = ChaosTablist.id("save_result_v2");
        public static final Codec<SaveResult> CODEC = new Codec<>(b -> new SaveResult(b.readVarLong(), b.readBoolean(), b.readUtf(64), b.readVarLong()), (b,p) -> { b.writeVarLong(p.requestId()); b.writeBoolean(p.success()); b.writeUtf(p.reason(),64); b.writeVarLong(p.revision()); });
        @Override public ResourceLocation type() { return TYPE; }
    }
}
